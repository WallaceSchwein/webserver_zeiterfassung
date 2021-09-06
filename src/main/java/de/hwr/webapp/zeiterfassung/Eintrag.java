package de.hwr.webapp.zeiterfassung;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Eintrag {
	
	//Attribute
	private int id;
	private int userID;
	private String user;
	private int projectID;
	private String project;
	private int modulID;
	private String modul;
	private String strDate;
	private java.util.Date date;
	private double hours;
	
	//Konstruktor
	public Eintrag(int id, int userID, String user, int projectID, String project, int modulID, String modul,
			String strDate, double hours) throws ParseException {
		
		this.id = id;
		this.userID = userID;
		this.user = user;
		this.projectID = projectID;
		this.project = project;
		this.modulID = modulID;
		this.modul = modul;
		this.strDate = strDate;
		this.hours = hours;
		this.date = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
		
	}
	
	//Getter, Setter & ToString
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getUserID() {
		return userID;
	}
	public void setUserID(int userID) {
		this.userID = userID;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public int getProjectID() {
		return projectID;
	}
	public void setProjectID(int projectID) {
		this.projectID = projectID;
	}
	public String getProject() {
		return project;
	}
	public void setProject(String project) {
		this.project = project;
	}
	public int getModulID() {
		return modulID;
	}
	public void setModulID(int modulID) {
		this.modulID = modulID;
	}
	public String getModul() {
		return modul;
	}
	public void setModul(String modul) {
		this.modul = modul;
	}
	public String getStrDate() {
		return strDate;
	}
	public void setStrDate(String strDate) {
		this.strDate = strDate;
	}
	public java.util.Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public double getHours() {
		return hours;
	}
	public void setHours(double hours) {
		this.hours = hours;
	}

	@Override
	public String toString() {
		return "Eintrag: [id=" + id + ", user=" + user + ", project=" + project + ", modul=" + modul + ", date=" + date
				+ ", hours=" + hours + "]"; 
	}
	
	
}
