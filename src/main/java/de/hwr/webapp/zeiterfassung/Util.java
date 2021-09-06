/*
 * Util.java
 * JSF 2.3
 */

package de.hwr.webapp.zeiterfassung;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.Connection;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;


/**
 * Diese Klasse stellt verschiedene Hilfsfunktionen zur Verf�gung
 *
 * @author Wolfgang Lang
 * @version 2019-12-04
 * @see    "Foliensatz zur Vorlesung"
 */
public class Util {
  
  /**
   * Wichtig ist, dass hier der Bean-Name steht, der in der 
   * faces-config mit &lt;managed-bean-name&gt;xxx&lt;/managed-bean-name&gt; 
   * definiert wurde
   */
  final static String DATABASE_BEAN_NAME = "mb_db";
  
  final String CLASSNAME = getClass().getName();
  
  private MbDb mbDb = null;

  /*--------------------------------------------------------------------------*/
  
  public Util() {}

  /*--------------------------------------------------------------------------*/
  
  /**
   * Datenbankverbindung schlie�en
   * @param con : Aktuelle DB-Verbindung
   */  
  public void closeConnection( Connection con ){
    if( mbDb == null ) mbDb = (MbDb) getBean( DATABASE_BEAN_NAME );
    if( mbDb != null ) mbDb.closeConnection( con );
    else System.err.println( 
              "Util.closeConnection(): Fehler beim Schlie�en der Connection!" );
  }
  
  /*--------------------------------------------------------------------------*/
  
  /**
  * Gibt ein Connection-Objekt aus dem Pool zur�ck
  * @return : Connection-Objekt
  */
  public Connection getCon(){
          
		final String MNAME = ".getCon()";  
		final String TAG = CLASSNAME + MNAME;
		
		Connection con = null;
		
		log( TAG + ": entering..." );
		FacesContext fc = FacesContext.getCurrentInstance();
		if( fc == null ) log( TAG + ": FacesContext is null." );
		else {
			Application app = fc.getApplication();
			if( app == null ) log( TAG + ": Application is null." );
			else {
				con = app.evaluateExpressionGet( fc, "#{" + DATABASE_BEAN_NAME + ".con}", 
						                             Connection.class );
				if( con == null ) log( TAG + ": Connection is null." );
			}
		}
		
		log( TAG + ": ...exiting" );
		return con;
  }
  
  /*--------------------------------------------------------------------------*/
  
  public void log( String s ) {
  	if( mbDb == null ) mbDb = (MbDb) getBean( DATABASE_BEAN_NAME );
    if( mbDb != null ) mbDb.log( s );
  }
  
  /*--------------------------------------------------------------------------*/
  
  // Nur der Systematik wegen (2012-09-16):
  public Connection getConnection(){ return getCon(); }
  
  /*--------------------------------------------------------------------------*/
  
  /**
   * Gibt eine Referenz auf eine Managed Bean zur�ck
   *
   * @param sBean Name der Bean
   * @return : Referenz auf Managed Bean
   */
  public Object getBean( String sBean ){
      
  	Object o = null;
  	
    if( sBean != null ) {
    	FacesContext fc = FacesContext.getCurrentInstance();
    	if( fc != null ) {
    		Application app = fc.getApplication();
    		if( app != null ) {
    			o = app.evaluateExpressionGet( fc, "#{" + sBean +"}", Object.class );
    		}    				                                        
    	} else System.err.println( "FacesContext in getBean ist null!");
    }
    	/*	return FacesContext.getCurrentInstance().getApplication().
            evaluateExpressionGet( FacesContext.getCurrentInstance(),
            "#{" + sBean +"}", Object.class ); */ 
      
      	/* 	FacesContext fc = FacesContext.getCurrentInstance();
      		Application app = fc.getApplication();
      		Object o = app.evaluateExpressionGet( fc, "#{" + sBean +"}", Object.class );
      		return o; */      
    
    return o;
  }
  
  /*--------------------------------------------------------------------------*/
  
  /**
   * Verschl�sselung eines Passworts
   * Aus Kennung und Passwort wird mittels SHA-Hash das verschl�sselete Passwort
   * generiert. 
   * @param user Kennung
   * @param pw   Passwort im Klartext
   * @return out Verschl�sseltes Passwort oder unverschl�sselte 
   *             Input-Parameter bei Fehler
   */
  public String cryptpw( String user, String pw ) {
    
    String in = user + pw;
    String out = in;
    
    try{
      MessageDigest md = MessageDigest.getInstance( "SHA" );
      byte[] bHash = md.digest( in.getBytes() ); // oder getBytes( "UTF-8" ) ?
      StringBuffer sb = new StringBuffer();
      
      for( int i = 0; i < bHash.length; i++ ){
        sb.append( Integer.toHexString( 0xF0 & bHash[i] ).charAt(0) );
        sb.append( Integer.toHexString( 0x0F & bHash[i] ) );
      }
      out = sb.toString();
    }
    catch( NoSuchAlgorithmException ex ){
      ex.printStackTrace();   
    }
    
    return out;
  }
}