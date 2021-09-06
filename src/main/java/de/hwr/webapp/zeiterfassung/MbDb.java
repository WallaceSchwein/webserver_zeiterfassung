/*
 * MbDb.java
 * JSF 2.3
 */

package de.hwr.webapp.zeiterfassung;

import static java.lang.System.*;

/*
 * Einige ältere Versionen des MySQL-Connectors haben einen Bug in der Klasse
 * MysqlConnectionPoolDataSource, der dazu führt, dass kein Connect zu MariaDB
 * aufgebaut werden kann (außer der User verwendet kein Passwort) . Vgl. hier: 
 * https://mariadb.atlassian.net/browse/MDEV-5155
 * Ohne PW klappts.
 * Workaround: Aktuelle Version des MySQL-Connectors einsetzen, z. B. 
 * mysql-connector-java-8.0.16.jar.
 */
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Über diese Bean erfolgt der Connect zu Datenbank. Die Verwaltung der Ver-
 * bindungen kann über einen connection pool erfolgen (use_pool == true). Die
 * Verbindungsparameter werden als managed properties aus der faces-config.xml
 * gelesen. Alternativ ist auch ein JNDI lookup möglich.
 * 
 * Wichtig: Dem Projekt muss der verwendete Datenbanktreiber hinzugefügt werden,
 * in Eclipse hier: <code>Project | Properties | Java Build Path</code>
 *
 * @author Wolfgang Lang
 * @version 2019-12-04
 * @see "Foliensatz zur Vorlesung"
 */
public class MbDb implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final String TIMEZONE = "UTC";

	private MysqlConnectionPoolDataSource mds_pool = null;
	private MysqlDataSource mds = null;

	private PrintWriter pwLog = null;
	private boolean use_pool = false;

	private String 	user = "from faces-config.xml", 
					pw = "from faces-config.xml", 
					constr = "from faces-config.xml",
					logfile = "from faces-config.xml";

	/*--------------------------------------------------------------------------*/

	public MbDb() {
		log("Creating net.lehre_online.db.MbDb at " + new Date());
	}

	/*--------------------------------------------------------------------------*/

	public void setUse_pool(boolean b) {
		use_pool = b;
	}

	public void setConstr(String s) {
		constr = s;
	}

	public void setUser(String s) {
		user = s;
	}

	public void setPw(String s) {
		pw = s;
	}

	public PrintWriter getLogWriter() {
		return pwLog;
	}

	/*--------------------------------------------------------------------------*/

	/**
	 * Logfile setzen (managed property) und Logwriter öffnen
	 * 
	 * @param s : logfile Pfad und Name
	 */
	public void setLogfile(String s) {

		logfile = s;

		try {
			if (pwLog == null) {
				// GGf. directory anlegen:
				File fn = new File(logfile);
				if (!fn.exists()) {
					File fParent = fn.getParentFile();
					if (fParent != null && (!fParent.exists())) {
						out.println("Pfad " + fParent.getAbsolutePath() + " für Logfile wird angelegt...");
						fParent.mkdirs();
					}
				}

				pwLog = new PrintWriter(new FileOutputStream(fn));
				// Zusätzliche Infos in Log-File ausgeben. Hilft beim debuggen:
				log("Application start.");
				DriverManager.setLogWriter(pwLog);
			}
		} catch (SecurityException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SecurityException", ex.getLocalizedMessage()));
			err.println("Fehler: Kann Verzeichnis für " + logfile + " nicht anlegen!");
			ex.printStackTrace();
		} catch (FileNotFoundException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "FileNotFoundException", ex.getLocalizedMessage()));
			err.println("Fehler: Kann file " + logfile + " nicht öffnen!");
			ex.printStackTrace();
		}
	}

	/*--------------------------------------------------------------------------*/

	/**
	 * Ausgabe einer Meldung in das Log file
	 * 
	 * @param s : Meldung
	 */
	public void log(String s) {
		if (pwLog != null) {
			pwLog.println(new Date().toString() + ": " + s);
			pwLog.flush();
		} else
			out.println(s);
	}

	/*--------------------------------------------------------------------------*/

	/**
	 * Verbindung zur Datenbank an den Pool zurück geben
	 * 
	 * @param con : Aktuelle connection zur Datenbank
	 */
	public void closeConnection(Connection con) {
		try {
			log("Close connection...");
			con.close();
		} catch (SQLException ex) {
			log("SQLException!");
			while (ex != null) {
				ex.printStackTrace();
				ex = ex.getNextException();
			}
		}
	}

	/*--------------------------------------------------------------------------*/

	/**
	 * Stellt eine Connection zur Verfügung. Falls use_pool == true, wird die
	 * Connection aus dem Pool geholt. Ist noch kein Pool vorhanden, wird er
	 * angelegt.
	 * 
	 * @return Connection-Objekt
	 */
	public Connection getCon() {

		log("Get connection...");

		Connection con = null;

		try {
			if (use_pool) {
				if (mds_pool == null) {
					// Connection pool anlegen:
					mds_pool = new MysqlConnectionPoolDataSource();
					mds_pool.setURL(constr);
					mds_pool.setUser(user);
					mds_pool.setPassword(pw);
					mds_pool.setLogWriter(pwLog);
					mds_pool.setServerTimezone(TIMEZONE);
				}

				if (mds_pool != null) {
					con = mds_pool.getPooledConnection().getConnection();
					con.setAutoCommit(true);
				}
			} else { 
				// neue Connection erzeugen:
				mds = new MysqlDataSource();
				mds.setURL(constr);
				mds.setUser(user);
				mds.setPassword(pw);
				mds.setLogWriter(pwLog);
				mds.setServerTimezone(TIMEZONE);
				con = mds.getConnection();
				con.setAutoCommit(true);
			}

			logDebugInfo(con);
		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			log("SQLException!");
			while (ex != null) {
				ex.printStackTrace();
				ex = ex.getNextException();
			}
		}

		return con;
	}

	/*--------------------------------------------------------------------------*/

	/**
	 * Gibt einige Infos zur aktuellen connection über das log file aus.
	 * 
	 * @param con
	 * @throws SQLException
	 */
	private void logDebugInfo(Connection con) throws SQLException {

		log("*--------------  connection data  --------------------*");
		log("use_pool is " + use_pool);
		log("Kennung/PW " + user + "/" + pw);

		DatabaseMetaData meta = con.getMetaData();
		log("Server name: " + meta.getDatabaseProductName());
		log("Server version: " + meta.getDatabaseProductVersion());
		log("Driver name: " + meta.getDriverName());
		log("Driver version: " + meta.getDriverVersion());
		log("JDBC major.minor version: " + meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());

		log("*-----------------------------------------------------*");
	}
}