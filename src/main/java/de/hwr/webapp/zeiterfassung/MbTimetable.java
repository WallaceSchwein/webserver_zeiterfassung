/*
 * MbTimetable
 * JSF 2.3 DB-Anwendung
 */

package de.hwr.webapp.zeiterfassung;

import static java.lang.System.*;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.sql.PreparedStatement;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

/**
 * Backing bean der JSF-Seite timetable.xhtml
 *
 * @author Lea Heilmann, Willi Kristen
 * @version 2021-05-31
 * @see "Projektdokumentation"
 */
@Named
@SessionScoped
public class MbTimetable implements Serializable {

	private static final long serialVersionUID = 1L;
	
	//Benötigte SQL-Statements in Konstanten abgelegt
	
	final String SQL_SELECT_USERS = "SELECT * FROM user";
	final String SQL_SELECT_PROJECTS = "SELECT z.projekt, p.projektname "
											+ "FROM zuordnung z "
											+ "JOIN projekte p ON z.projekt = p.id "
											+ "WHERE z.user = ";
	final String SQL_SELECT_MODULS = "SELECT id, modulname FROM module WHERE projekt = ";
	final String INSERT_STATEMENT = "INSERT INTO timetable (user, projekt, modul, datum, stunden) VALUES (";
	
	//Für die Funktionalität der Seite benötigte Variablen
	
	private boolean connected = false;

	private Util util = new Util();

	private Connection con = null;
	private Statement stm = null;
	private ResultSet rs = null;
	
	Map<String, Integer> userData;
	Map<String, Integer> projectData;
	Map<String, Integer> modulData;
	
	private int user = -1;
	private int project = -1;
	private int modul = -1;
	private double hours = 0.0;
	private String strDate = "";
	private Date date;
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	
	/*--------------------------------------------------------------------------*/
	
	//Konstruktor, in dem die Datenbankverbindung aufgebaut und das erste Result Set geladen wird
	
	public MbTimetable() throws SQLException {
		System.out.println("MbTimetable Konstruktor: DB Connection wird aufgebaut und RS geladen!");
		
		this.connect(SQL_SELECT_USERS);
		
		userData = new HashMap<String, Integer>();
		
		do {
			userData.put(rs.getString("username"), rs.getInt("id"));
		} while (rs.next());
		
	}
	
	// Init-Methode (Enthält keinerlei Funktionalität. Dient nur zum besseren Nachvollziehen des JSF-Lifecycles
	@PostConstruct
	public void init() {
		System.out.println("@PostConstruct.MbTimetable: init()");
	}

	/*--------------------------------------------------------------------------*/
	
	//Benötigte Getter und Setter
	
	public int getUser() {
		System.out.println("Get User: " + user);
		return user;
	}

	public void setUser(int i) {
		user = i;
		System.out.println("Set User: " + user);
	}
	
	public int getProject() {
		System.out.println("Get Project: " + project);
		return project;
	}

	public void setProject(int i) {
		project = i;
		System.out.println("Set Project: " + project);
	}
	
	public int getModul() {
		System.out.println("Get Modul: " + modul);
		return modul;
	}

	public void setModul(int i) {
		modul = i;
		System.out.println("Set Modul: " + modul);
	}
	
	public double getHours() {
		System.out.println("Get Hours: " + hours);
		return hours;
	}

	public void setHours(double d) {
		hours = d;
		System.out.println("Set Hours: " + hours);
	}
	
	public Date getDate() {
		System.out.println("Get Date: " + date);
		return date;
	}

	public void setDate(Date d) {
		date = d;
		System.out.println("Set Date: " + date);
	}
	
	public Map<String, Integer> getUserData() throws SQLException {
		System.out.println("Get UserData");
		return userData;
	}
	
	public Map<String, Integer> getProjectData() throws SQLException {
		System.out.println("Get ProjectData");
		return projectData;
	}

	public Map<String, Integer> getModulData() throws SQLException {
		System.out.println("Get ModulData");
		return modulData;
	}
	
	public boolean getConnected() {
		return connected;
	}

	public void setConnected(boolean b) {
		connected = b;
	}

	/*--------------------------------------------------------------------------*/

	/**
	 * Verbindung zur Datenbank herstellen
	 * 
	 * @param SQLstatement, String
	 */
	public void connect(String SQLstatement) {

		if (util != null)
			con = util.getCon();
		if (con != null) {
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				rs = stm.executeQuery(SQLstatement);
				if (rs.first())
					System.out.println("Resultset aufgebaut.. \n Erster Eintrag: " + rs.getString(2));
				connected = true;
				
			} catch (Exception ex) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
				out.println("Error: " + ex);
				ex.printStackTrace();
			}
		} else {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Exception", "Keine Verbindung zur Datenbank (Treiber nicht gefunden?)"));
			out.println("Keine Verbingung zur Datenbank");
		}
	}

	/*--------------------------------------------------------------------------*/

	/**
	 * Verbindung zur Datenbank beenden
	 * 
	 * @param ae ActionEvent
	 */
	public void disconnect(ActionEvent ae) {

		if (con != null) {
			try {
				if (rs != null)
					rs.close();
				if (stm != null)
					stm.close();

				util.closeConnection(con);

				connected = false;

				setUser(-1);
				setProject(-1);
				setModul(-1);
				setHours(0.0);
				strDate = "";
				setDate(null);

			} catch (Exception ex) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Exception", ex.getLocalizedMessage()));
				out.println("Error: " + ex);
				ex.printStackTrace();
			}
		}
	}

	/*--------------------------------------------------------------------------*/
	
	//onChange-Methoden, die bei Änderung der Werte über den AJAX-Listener aufgerufen werden
	
	/**
	 * Baut in Abhängigkeit zum gewählten User
	 * ein neues Result Set auf und speichert 
	 * die Werte in der Map, die dem DropDown-Menü
	 * für die Projekte übergeben wird
	 * 
	 */
	public void onUserChange() throws SQLException {
		System.out.println("onUserChange");
		if (user != -1) {
			connect(SQL_SELECT_PROJECTS + user);
			
			projectData = new HashMap<String, Integer>();
			
			do {
				projectData.put(rs.getString("projektname"), rs.getInt("projekt"));
			} while (rs.next());
		}
	}
	
	/**
	 * Baut in Abhängigkeit zum gewählten Projekt
	 * ein neues Result Set auf und speichert 
	 * die Werte in der Map, die dem DropDown-Menü
	 * für die Module übergeben wird
	 * 
	 */
	public void onProjectChange() throws SQLException {
		System.out.println("onProjectChange");
		if (project != -1) {
			connect(SQL_SELECT_MODULS + project);
			
			modulData = new HashMap<String, Integer>();
			
			do {
				modulData.put(rs.getString("modulname"), rs.getInt("id"));
			} while (rs.next());
		}
	}
	
	/*--------------------------------------------------------------------------*/
	
	//Methode, die bei Klick auf den Submit-Button über den AJAX-Listener ausgeführt wird
	
	public void onSubmit() {
		System.out.println("Submitbutton clicked");
		System.out.println("Final: User - " + user);
		System.out.println("Final: Projekt - " + project);
		System.out.println("Final: Modul - " + modul);
		System.out.println("Final: Datum - " + date.toString());
		System.out.println("Final: Stunden - " + hours);
		
		strDate = "'" + df.format(date) + "'";
		System.out.println("Formattiertes Datum: " + strDate);
		
		try {
			System.out.println("Statement: " + INSERT_STATEMENT + user + ", " + project + ", " + modul + ", " + strDate + ", " + hours + ")");
			PreparedStatement ps = con.prepareStatement(INSERT_STATEMENT + user + ", " + project + ", " + modul + ", " + strDate + ", " + hours + ")");
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Stundensatz wurde erfolgreich eingetragen!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Stundensatz wurde erfolgreich eingetragen!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
	}
}
