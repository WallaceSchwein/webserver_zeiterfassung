/*
 * MbOverview
 * JSF 2.3 DB-Anwendung
 */

package de.hwr.webapp.zeiterfassung;

import static java.lang.System.out;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class MbOverview implements Serializable {

	private static final long serialVersionUID = 1L;
	
	//Benötigte SQL-Statements in Konstanten abgelegt
	
	final String SQL_SELECT_USERS = "SELECT * FROM user";
	final String SQL_SELECT_PROJECTS = "SELECT * FROM projekte";
	final String SQL_SELECT_MODULS = "SELECT * FROM module";
	final String SQL_SELECT_USERS_PROJECTS = "SELECT z.projekt, p.projektname "
											+ "FROM zuordnung z "
											+ "JOIN projekte p ON z.projekt = p.id "
											+ "WHERE z.user = ";
	final String SQL_SELECT_PROJECTS_MODULS = "SELECT id, modulname FROM module WHERE projekt = ";
	final String SQL_SELECT_DATA = "SELECT t.id, t.user, u.username, t.projekt, p.projektname, t.modul, m.modulname, t.datum, t.stunden "
									+ "FROM timetable t "
									+ "JOIN user u ON t.user = u.id "
									+ "JOIN projekte p ON t.projekt = p.id "
									+ "JOIN module m ON t.modul = m.id";
	
	//Für die Funktionalität der Seite benötigte Variablen
	
	private boolean connected = false;

	private Util util = new Util();

	private Connection con = null;
	private Statement stm = null;
	private ResultSet rs = null;
	
	Map<String, Integer> userData;
	Map<String, Integer> projectData;
	Map<String, Integer> modulData;
	Map<String, Integer> sortData;
	
	List<Eintrag> dataList;
	
	private int user = -1;
	private int project = -1;
	private int modul = -1;
	private int sort = -1;
	private String strStartDate = "1900-01-01";
	private String strEndDate = "2199-12-31";
	private Date startDate;
	private Date endDate;
	
	/*--------------------------------------------------------------------------*/
	
	//Konstruktor, in dem die Datenbankverbindung aufgebaut und das erste Result Set geladen wird
	
	public MbOverview() throws SQLException, ParseException {
		System.out.println("MbOverview Konstruktor: DB Connection wird aufgebaut und RS geladen!");
		
		this.connect();
		
		setUserData();
		setProjectData();
		setModulData();
		setDataList();
	}
	
	// Init-Methode (Enthält keinerlei Funktionalität. Dient nur zum besseren Nachvollziehen des JSF-Lifecycles
	@PostConstruct
	public void init() {
		System.out.println("@PostConstruct.MbOverview: init()");
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
	
	public double getSort() {
		System.out.println("Get Sort: " + sort);
		return sort;
	}

	public void setSort(int i) {
		sort = i;
		System.out.println("Set Sort: " + sort);
	}
	
	public Date getStartDate() {
		System.out.println("Get StartDate: " + startDate);
		return startDate;
	}

	public void setStartDate(Date d) {
		startDate = d;
		System.out.println("Set StartDate: " + startDate);
	}
	
	public Date getEndDate() {
		System.out.println("Get EndDate: " + endDate);
		return endDate;
	}

	public void setEndDate(Date d) {
		endDate = d;
		System.out.println("Set EndDate: " + endDate);
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
	
	public List<Eintrag> getDataList() {
		System.out.println("Get DataList");
		return dataList;
	}
	
	public Map<String, Integer> getSortData() throws SQLException {
		System.out.println("Get SortData");
		return sortData;
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
	 */
	public void connect() {

		if (util != null) {
			con = util.getCon();
			setConnected(true);
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
				setSort(-1);
				setStartDate(null);
				setEndDate(null);

			} catch (Exception ex) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Exception", ex.getLocalizedMessage()));
				out.println("Error: " + ex);
				ex.printStackTrace();
			}
		}
	}
	
	/*--------------------------------------------------------------------------*/
	
	//Die SetData-Methoden werden im Konstruktor aufgerufen. Sie stellen die Daten für die Comboboxen bereit, die schon zu beginn des Lifecycles aufgebaut werden.
	
	/**
	 * Befüllt die Map, aus der die
	 * Combobox für die User-Auswahl
	 * ihre Einträge erhält mit Daten
	 * 
	 */
	public void setUserData() throws SQLException {
		
		System.out.println("UserData wird aus DB geladen");
		
		if (con != null) {
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				rs = stm.executeQuery(SQL_SELECT_USERS);
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
		
		userData = new HashMap<String, Integer>();
		
		do {
			userData.put(rs.getString("username"), rs.getInt("id"));
		} while (rs.next());
		
		rs = null;
	}
	
	/**
	 * Befüllt die Map, aus der die
	 * Combobox für die Projekt-Auswahl
	 * ihre Einträge erhält mit Daten
	 * 
	 */
	public void setProjectData() throws SQLException {
		
		System.out.println("ProjectData wird aus DB geladen");
		
		if (con != null) {
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				rs = stm.executeQuery(SQL_SELECT_PROJECTS);
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
		
		projectData = new HashMap<String, Integer>();
		
		do {
			projectData.put(rs.getString("projektname"), rs.getInt("id"));
		} while (rs.next());
		
		rs = null;
	}
	
	/**
	 * Befüllt die Map, aus der die
	 * Combobox für die Modul-Auswahl
	 * ihre Einträge erhält mit Daten
	 * 
	 */
	public void setModulData() throws SQLException {
		
		System.out.println("ModulData wird aus DB geladen");
		
		if (con != null) {
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				rs = stm.executeQuery(SQL_SELECT_MODULS);
				if (rs.first())
					System.out.println("Resultset aufgebaut.. \n Erster Eintrag: " + rs.getString(3));
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
		
		modulData = new HashMap<String, Integer>();
		
		do {
			modulData.put(rs.getString("modulname"), rs.getInt("id"));
		} while (rs.next());
		
		rs = null;
	}
	
	/**
	 * Befüllt die Liste, mit Daten,
	 * die die Einträge enthält,
	 * die im DataTable der Seite
	 * gerendert werden.
	 * 
	 */
	public void setDataList() throws SQLException, ParseException {
		
		System.out.println("DataList wird aus DB geladen");
		
		if (con != null) {
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				System.out.println("Statement: " + SQL_SELECT_DATA);
				rs = stm.executeQuery(SQL_SELECT_DATA);
				if (rs.first())
					System.out.println("Resultset aufgebaut.. \n Erster Eintrag: " + rs.getString(5));
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
		
		dataList = new ArrayList<Eintrag>();
		
		do {
			dataList.add(new Eintrag(rs.getInt("id"), rs.getInt("user"), rs.getString("username"), rs.getInt("projekt"), rs.getString("projektname"), rs.getInt("modul"), rs.getString("modulname"), rs.getString("datum"), rs.getDouble("stunden")));
		} while (rs.next());
		
		rs = null;
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
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				System.out.println("Statement: " + SQL_SELECT_USERS_PROJECTS + user);
				rs = stm.executeQuery(SQL_SELECT_USERS_PROJECTS + user);
				if (rs.first())
					System.out.println("Resultset aufgebaut.. \n Erster Eintrag: " + rs.getString(1));
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
		
		projectData = new HashMap<String, Integer>();
		
		do {
			projectData.put(rs.getString("projektname"), rs.getInt("projekt"));
		} while (rs.next());
		
		rs = null;
		
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
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				System.out.println("Statement: " + SQL_SELECT_PROJECTS_MODULS + project);
				rs = stm.executeQuery(SQL_SELECT_PROJECTS_MODULS + project);
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
		
		modulData = new HashMap<String, Integer>();
		
		do {
			modulData.put(rs.getString("modulname"), rs.getInt("id"));
		} while (rs.next());
		
		rs = null;
		
	}
	
	/*--------------------------------------------------------------------------*/
	
	//Methode, die bei Klick auf den Filter-Button über den AJAX-Listener ausgeführt wird
	
	public void onFilter() throws SQLException, ParseException {
		System.out.println("Filterbutton clicked");
		System.out.println("Final: User - " + user);
		System.out.println("Final: Projekt - " + project);
		System.out.println("Final: Modul - " + modul);
		
		setDataList();
		
		List<Eintrag> copyList = new ArrayList<Eintrag>();
		
		for (Eintrag e : dataList) {
			System.out.println(e);
			copyList.add(e);
		}
		
		System.out.println("Liste kopiert");
		
		dataList = new ArrayList<Eintrag>();
		
		System.out.println("DataList neu angelegt");
		
		if (startDate == null) {
			startDate = (Date) new SimpleDateFormat("yyyy-MM-dd").parse(strStartDate);
		}
		
		if (endDate == null) {
			endDate = (Date) new SimpleDateFormat("yyyy-MM-dd").parse(strEndDate);
		}
		
		System.out.println("Datumswerte überprüft");
		
		for (Eintrag e : copyList) {
			if (e.getDate().compareTo(startDate) >= 0 && e.getDate().compareTo(endDate) < 0) {
				System.out.println(e);
				dataList.add(e);
			}
		}
		
		System.out.println("Nach Datum gefiltert (immer)");
		
		copyList = new ArrayList<Eintrag>();
		
		for (Eintrag e : dataList) {
			System.out.println(e);
			copyList.add(e);
		}
		
		dataList = new ArrayList<Eintrag>();
		
		System.out.println("Liste erneut kopiert und DataList neu angelegt");
		
		if (user > 0 && project <= 0 && modul <= 0) {
			for (Eintrag e : copyList) {
				if (e.getUserID() == user) {
					System.out.println(e);
					dataList.add(e);
					System.out.println("Eintrag hinzugefügt");
				}
			}
		} else if (user <= 0 && project > 0 && modul <= 0) {
			for (Eintrag e : copyList) {
				if (e.getProjectID() == project) {
					dataList.add(e);
				}
			}
		} else if (user <= 0 && project <= 0 && modul > 0) {
			for (Eintrag e : copyList) {
				if (e.getModulID() == modul) {
					dataList.add(e);
				}
			}
		} else if (user > 0 && project > 0 && modul <= 0) {
			for (Eintrag e : copyList) {
				if (e.getUserID() == user && e.getProjectID() == project) {
					dataList.add(e);
				}
			}
		} else if (user <= 0 && project > 0 && modul > 0) {
			for (Eintrag e : copyList) {
				if (e.getProjectID() == project && e.getModulID() == modul) {
					dataList.add(e);
				}
			}
		} else if (user > 0 && project <= 0 && modul > 0) {
			for (Eintrag e : copyList) {
				if (e.getUserID() == user && e.getModulID() == modul) {
					dataList.add(e);
				}
			}
		} else if (user > 0 && project > 0 && modul > 0) {
			for (Eintrag e : copyList) {
				if (e.getUserID() == user && e.getProjectID() == project && e.getModulID() == modul) {
					dataList.add(e);
				}
			}
		}
		
		System.out.println("DataList final gefiltert");
		
		if (dataList.isEmpty()) {
			setDataList();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Null", "Ihre Filteranfrage brachte leider keine Ergebnisse."));
		}
	}

}
