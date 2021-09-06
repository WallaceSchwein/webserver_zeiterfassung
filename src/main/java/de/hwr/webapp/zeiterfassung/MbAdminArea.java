/*
 * MbAdminArea
 * JSF 2.3 DB-Anwendung
 */

package de.hwr.webapp.zeiterfassung;

import static java.lang.System.out;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

/**
 * Backing bean der JSF-Seite adminarea.xhtml
 *
 * @author Lea Heilmann, Willi Kristen
 * @version 2021-05-31
 * @see "Projektdokumentation"
 */
@Named
@SessionScoped
public class MbAdminArea implements Serializable {

	private static final long serialVersionUID = 1L;
	
	//Benötigte SQL-Statements in Konstanten abgelegt
	
	//SELECT-Statements
	final String SQL_SELECT_USERS = "SELECT * FROM user";
	final String SQL_SELECT_PROJECTS = "SELECT * FROM projekte";
	final String SQL_SELECT_MODULS = "SELECT * FROM module WHERE projekt = ";
	final String SQL_SELECT_ADD_ALLOCATION = "SELECT * FROM projekte WHERE NOT id IN ("
																					+ "SELECT p.id "
																					+ "FROM zuordnung z "
																					+ "JOIN projekte p ON z.projekt = p.id "
																					+ "WHERE user = ";
	final String SQL_SELECT_DEL_ALLOCATION = "SELECT z.id, p.projektname "
											+ "FROM zuordnung z "
											+ "JOIN projekte p ON z.projekt = p.id "
											+ "WHERE user = ";
	//DELETE-Statements
	final String SQL_DELETE_USER = "DELETE FROM user WHERE id = ";
	final String SQL_DELETE_PROJECT = "DELETE FROM projekte WHERE id = ";
	final String SQL_DELETE_PROJECT_MODULS = "DELETE FROM module WHERE projekt = ";
	final String SQL_DELETE_MODUL = "DELETE FROM module WHERE id = ";
	final String SQL_DELETE_ALLOCATION = "DELETE FROM zuordnung WHERE id = ";
	//INSERT-Statements
	final String INSERT_USER_STATEMENT = "INSERT INTO user (username) VALUE ('";
	final String INSERT_PROJECT_STATEMENT = "INSERT INTO projekte (projektname) VALUE ('";
	final String INSERT_MODUL_STATEMENT = "INSERT INTO module (projekt, modulname) VALUE(";
	final String INSERT_ALLOCATION_STATEMENT = "INSERT INTO zuordnung (projekt, user) VALUE(";
	
	//Für die Funktionalität der Seite benötigte Variablen
	
	private boolean connected = false;

	private Util util = new Util();

	private Connection con = null;
	private Statement stm = null;
	private ResultSet rs = null;
	
	Map<String, Integer> userData;
	Map<String, Integer> projectData;
	Map<String, Integer> modulData;
	Map<String, Integer> addAllocationData;
	Map<String, Integer> delAllocationData;
	
	private String addUser = "";
	private int delUser = -1;
	
	private String addProject = "";
	private int delProject = -1;
	
	private int selectedProject = -1;
	
	private String addModul = "";
	private int delModul = -1;
	
	private int addAllocationUser = -1;
	private int addAllocationProject = -1;
	private int delAllocationUser = -1;
	private int delAllocationProject = -1;
	
	/*--------------------------------------------------------------------------*/
	
	//Konstruktor, in dem die Datenbankverbindung aufgebaut wird.
	
	public MbAdminArea() throws SQLException {
		System.out.println("MbAdminArea Konstruktor: DB Connection wird aufgebaut und RS geladen!");
		
		this.connect();
		
		this.setUserData();
		this.setProjectData();
		
	}
	
	// Init-Methode (Enthält keinerlei Funktionalität. Dient nur zum besseren Nachvollziehen des JSF-Lifecycles
	@PostConstruct
	public void init() {
		System.out.println("@PostConstruct.MbAdminArea: init()");
	}

	/*--------------------------------------------------------------------------*/
	
	//Benötigte Getter und Setter
	
	public String getAddUser() {
		System.out.println("Get AddUser: " + addUser);
		return addUser;
	}

	public void setAddUser(String s) {
		addUser = s;
		System.out.println("Set AddUser: " + addUser);
	}
	
	public int getDelUser() {
		System.out.println("Get DelUser: " + delUser);
		return delUser;
	}

	public void setDelUser(int i) {
		delUser = i;
		System.out.println("Set DelUser: " + delUser);
	}
	
	public String getAddProject() {
		System.out.println("Get AddProject: " + addProject);
		return addProject;
	}

	public void setAddProject(String s) {
		addProject = s;
		System.out.println("Set AddProject: " + addProject);
	}
	
	public int getDelProject() {
		System.out.println("Get DelProject: " + delProject);
		return delProject;
	}

	public void setDelProject(int i) {
		delProject = i;
		System.out.println("Set DelProject: " + delProject);
	}
	
	public int getSelectedProject() {
		System.out.println("Get SelectedProject: " + selectedProject);
		return selectedProject;
	}

	public void setSelectedProject(int i) {
		selectedProject = i;
		System.out.println("Set SelectedProject: " + selectedProject);
	}
	
	public String getAddModul() {
		System.out.println("Get AddModul: " + addModul);
		return addModul;
	}

	public void setAddModul(String s) {
		addModul = s;
		System.out.println("Set AddModul: " + addModul);
	}
	
	public int getDelModul() {
		System.out.println("Get DelModul: " + delModul);
		return delModul;
	}

	public void setDelModul(int i) {
		delModul = i;
		System.out.println("Set DelMOdul: " + delModul);
	}
	
	public int getAddAllocationUser() {
		System.out.println("Get AddAllocationUser: " + addAllocationUser);
		return addAllocationUser;
	}

	public void setAddAllocationUser(int i) {
		addAllocationUser = i;
		System.out.println("Set AddAllocationUser: " + addAllocationUser);
	}
	
	public int getAddAllocationProject() {
		System.out.println("Get AddAllocationProject: " + addAllocationProject);
		return addAllocationProject;
	}

	public void setAddAllocationProject(int i) {
		addAllocationProject = i;
		System.out.println("Set AddAllocationProject: " + addAllocationProject);
	}
	
	
	public int getDelAllocationUser() {
		System.out.println("Get DelAllocationUser: " + delAllocationUser);
		return delAllocationUser;
	}

	public void setDelAllocationUser(int i) {
		delAllocationUser = i;
		System.out.println("Set DelAllocationUser: " + delAllocationUser);
	}
	
	public int getDelAllocationProject() {
		System.out.println("Get DelAllocationProject: " + delAllocationProject);
		return delAllocationProject;
	}

	public void setDelAllocationProject(int i) {
		delAllocationProject = i;
		System.out.println("Set DelAllocationProject: " + delAllocationProject);
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
	
	public Map<String, Integer> getAddAllocationData() throws SQLException {
		System.out.println("Get AddAllocationData");
		return addAllocationData;
	}
	
	public Map<String, Integer> getDelAllocationData() throws SQLException {
		System.out.println("Get DelAllocationData");
		return delAllocationData;
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
		
		System.out.println("Connection wird aufgebaut...");
		
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

				setAddUser("");
				setDelUser(-1);
				
				setAddProject("");
				setDelProject(-1);
				
				setSelectedProject(-1);
				
				setAddModul("");
				setDelModul(-1);
				
				setAddAllocationUser(-1);
				setAddAllocationProject(-1);
				setDelAllocationUser(-1);
				setDelAllocationProject(-1);

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
	
	/*--------------------------------------------------------------------------*/
	
	//onChange-Methoden, die bei Änderung der Werte über den AJAX-Listener aufgerufen werden
	
	/**
	 * Baut in Abhängigkeit zum gewählten User
	 * ein neues Result Set auf und speichert 
	 * die Werte in der Map, die dem DropDown-Menü
	 * für die Projekte übergeben wird
	 * 
	 */
	public void onDelUserChange() throws SQLException {
		
		System.out.println("onDelUserChange");
		
		if (delAllocationUser != -1) {
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				System.out.println("Statement: " + SQL_SELECT_DEL_ALLOCATION + delAllocationUser);
				rs = stm.executeQuery(SQL_SELECT_DEL_ALLOCATION + delAllocationUser);
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
		
		delAllocationData = new HashMap<String, Integer>();
		
		do {
			delAllocationData.put(rs.getString("projektname"), rs.getInt("id"));
		} while (rs.next());
		
		rs = null;
		
	}
	
	/**
	 * Baut in Abhängigkeit zum gewählten User
	 * ein neues Result Set auf und speichert 
	 * die Werte in der Map, die dem DropDown-Menü
	 * für die Projekte übergeben wird
	 * 
	 */
	public void onAddUserChange() throws SQLException {
		
		System.out.println("onAddUserChange");
		
		if (addAllocationUser != -1) {
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				System.out.println("Statement: " + SQL_SELECT_ADD_ALLOCATION + addAllocationUser + ")");
				rs = stm.executeQuery(SQL_SELECT_ADD_ALLOCATION + addAllocationUser);
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
		
		addAllocationData = new HashMap<String, Integer>();
		
		do {
			addAllocationData.put(rs.getString("projektname"), rs.getInt("id"));
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
		if (selectedProject != -1) {
			
			try {
				stm = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				System.out.println("Statement: " + SQL_SELECT_MODULS + selectedProject);
				rs = stm.executeQuery(SQL_SELECT_MODULS + selectedProject);
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
	
	//Logik der gleichnamen Buttons auf der Seite, zum Hinzufügen nd Löschen von: Usern, Projekten, Modulen und Zuweisungen.
	
	public void onAddUser() {
		
		System.out.println("onAddUser");
		
		try {
			System.out.println("Statement: " + INSERT_USER_STATEMENT + addUser + "')");
			PreparedStatement ps = con.prepareStatement(INSERT_USER_STATEMENT + addUser + "')");
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("User wurde erfolgreich eingetragen!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "User wurde erfolgreich eingetragen!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
	}
	
	public void onDelUser() {
		
		System.out.println("onDelUser");
		
		try {
			System.out.println("Statement: " + SQL_DELETE_USER + delUser);
			PreparedStatement ps = con.prepareStatement(SQL_DELETE_USER + delUser);
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("User wurde erfolgreich gelöscht!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "User wurde erfolgreich gelöscht!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
		
	}
	
	public void onAddProject() {
		
		System.out.println("onAddProject");
		
		try {
			System.out.println("Statement: " + INSERT_PROJECT_STATEMENT + addProject + "')");
			PreparedStatement ps = con.prepareStatement(INSERT_PROJECT_STATEMENT + addProject + "')");
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Projekt wurde erfolgreich eingetragen!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Projekt wurde erfolgreich eingetragen!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
		
	}
	
	public void onDelProject() {
		
		System.out.println("onDelProject");
		
		try {
			System.out.println("Statement: " + SQL_DELETE_PROJECT_MODULS + delProject);
			PreparedStatement ps = con.prepareStatement(SQL_DELETE_PROJECT_MODULS + delProject);
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Projektmodule wurden zunächst gelöscht..");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Projektmodule wurden zunächst gelöscht!"));
			
			ps = null;	
			n = 0;
			
			}
			
			System.out.println("Statement: " + SQL_DELETE_PROJECT + delProject);
			ps = con.prepareStatement(SQL_DELETE_PROJECT + delProject);
			
			n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Projekt wurde erfolgreich gelöscht!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Projekt wurde erfolgreich gelöscht!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
		
	}
	
	public void onAddModul() {
		
		System.out.println("onAddModul");
		
		try {
			System.out.println("Statement: " + INSERT_MODUL_STATEMENT + selectedProject + ", '" + addModul + "')");
			PreparedStatement ps = con.prepareStatement(INSERT_MODUL_STATEMENT + selectedProject + ", '" + addModul + "')");
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Modul wurde erfolgreich eingetragen!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Modul wurde erfolgreich eingetragen!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
		
	}
	
	public void onDelModul() {
		
		System.out.println("onDelModul");
		
		try {
			System.out.println("Statement: " + SQL_DELETE_MODUL + delModul);
			PreparedStatement ps = con.prepareStatement(SQL_DELETE_MODUL + delModul);
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Modul wurde erfolgreich gelöscht!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Modul wurde erfolgreich gelöscht!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
		
	}
	
	public void onAddAllocation() {
		
		System.out.println("onAddAllocation");
		
		try {
			System.out.println("Statement: " + INSERT_ALLOCATION_STATEMENT + addAllocationProject + ", " + addAllocationUser + "')");
			PreparedStatement ps = con.prepareStatement(INSERT_ALLOCATION_STATEMENT + addAllocationProject + ", " + addAllocationUser + "')");
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Zuordnung wurde erfolgreich eingetragen!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Zuordnung wurde erfolgreich eingetragen!"));
			}

			ps.close();

		} catch (SQLException ex) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "SQLException", ex.getLocalizedMessage()));
			out.println("Error: " + ex);
			ex.printStackTrace();
		}
		
	}
	
	public void onDelAllocation() {
		
		System.out.println("onDelAllocation");
		
		try {
			System.out.println("Statement: " + SQL_DELETE_ALLOCATION + delAllocationProject);
			PreparedStatement ps = con.prepareStatement(SQL_DELETE_ALLOCATION + delAllocationProject);
			
			int n = ps.executeUpdate();
			if (n == 1) {
				System.out.println("Zuordnung wurde erfolgreich gelöscht!");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Succes!", "Zuordnung wurde erfolgreich gelöscht!"));
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
