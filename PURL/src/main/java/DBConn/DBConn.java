/*DBconn
 * 
 * holds functions that allow communication, and communicate with database
 * methods 
 * 1.DBConn - sets up conn, specify, serverURL, username, password of database
 * 2.Openconnection
 * 3.closeConnection
 * 4.retrieveURL - retrieve url requested
 * 5.insertPURL - insert purl, along with its url, who, what, when, erc
 * 6.editURL - edits url of a specified purl
 * @author: leland lopez
 */

package DBConn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

import Controller.PURLController;
import Model.model_Purl;

public class DBConn {

	final static Logger logger = Logger.getLogger(PURLController.class);
	//variables
	private String serverURL;
	private String username;
	private String password;
	private Connection conn = null;
	
	/*
	 * @param string serverURL
	 * @param string username
	 * @param string password
	 */
	public DBConn() throws IOException {
		getServerCredentials();
	}
	
	/*
	 * tries to connect.  returns false if connect failed.
	 */
	public boolean openConnection() {
		try {
			Class.forName("org.postgresql.Driver");
			if((conn = DriverManager.getConnection(serverURL, username, password)) != null) {
				return true;
			} else {
				return false;
			}
		} catch (Exception ee) {
			logger.error("Threw a BadException in DBConn::openConnection, full stack trace follows:", ee);
			return false;
		}
	}
	
	/*
	 * tries to disconnect.  returns false if disconnect failed.
	 */
	public boolean closeConnection() {
		try {
            if (conn != null) {
                conn.close();
                conn = null;
                return true;
            }
            return true;

        } catch (Exception ex) {
        	logger.error("Threw a BadException in DBConn::closeConnection, full stack trace follows:", ex);
            return false;
        }
	}
	
	/*
	 * @param string PURLID
	 * @param string username
	 * @param string password
	 */
	public String retrieveURL(String PURLID) {
		
	    ResultSet rs = null;
	    String url = null;
		
		try {
            if(conn != null) {	//if conn was made
                try {
                    Statement st = null;
                    st = conn.createStatement();
                    rs = st.executeQuery("SELECT \"strURL\" FROM \"PURL\" WHERE \"strPurl\" = '" + PURLID.toString() + "'");		//setup query and run
                	if(rs.next()) {	//if result was found
                		url = rs.getString(1);	//get result
                	} else {
                		logger.info("Purl: " + PURLID + " provided does not exist");	//log that PURLID doesn't exist
                	}

                } catch (Exception ee) {
                	logger.error("Threw a BadException in DBConn::retrieveURL, full stack trace follows:", ee);
                }  finally {

                    
                }
            }
        } catch (Exception ee) {
        	ee.printStackTrace();
            
        }
		return url;
		
	}
	
	/*
	 * insertPURL
	 * Input: String PURLID, String URL, String ERC, String Who, String What, String When
	 * executes query to edit URL of corresponding PURLID, replaces with provided URL
	 * OUTPUT: true if query was executed.  if not return false;
	 */
	public boolean insertPURL(String PURLID, String URL, String ERC, String Who, String What, String When) {
		ResultSet rs = null;
	    int numrows; //indicates how much rows have the same purlid
	    
	    try {
            if(conn != null) {
                try {
                    Statement st = null;
                    st = conn.createStatement();
                    String sql = "SELECT COUNT(*) FROM \"PURL\" WHERE \"strPurl\" = '" + PURLID.toString() + "'";
                    rs = st.executeQuery(sql);
                    if(rs.next()) {
                    	 numrows = Integer.parseInt(rs.getString(1));
                    } else {
                    	return false; //if it doesn't have the count in it, then something went wrong.
                    }
                    if(numrows == 0) {
	                    sql = "INSERT INTO \"PURL\" (\"strPurl\", \"strURL\", \"strERC\", \"strWho\", \"strWhat\", \"strWhen\") Values ('" + PURLID + "','"+ URL + "','" + ERC + "','" + Who + "','" + What + "','" + When + "')";	//setup query
	                    st.executeUpdate(sql); 	//execute query
	                    return true;	//return true
                    } else {
                    	return false;	//return false if PURLID specified was not unique
                    }
                  
                    
                } catch (Exception ee) {
                	logger.error("Threw a BadException in DBConn::insertPURL, full stack trace follows:", ee);
                    return false;
                }
            } else {
            	logger.error("Conn wasn't initialized error occured in DBConn::insertPURL");
            	return false;
            }
        } catch (Exception ee) {
        	logger.error("Threw a BadException in DBConn::insertPURL, full stack trace follows:", ee);
        	return false;
            
        }
	}
	
	/*
	 * editURL
	 * Input: String PURLID, String URL
	 * executes query to edit URL of corresponding PURLID, replaces with provided URL
	 * OUTPUT: true if query was executed.  if not return false;
	 */
	public boolean editURL(String PURLID, String URL) {
	    
	    try {
            if(conn != null) {	//if connection was made
                try {
                    Statement st = null;
                    st = conn.createStatement();
                    st.executeUpdate("UPDATE \"PURL\" SET \"strURL\" = '" + URL + "' WHERE \"strPurl\" = '" + PURLID + "'");	//setup query
                    return true;
                    
                } catch (Exception ee) {
                    ee.printStackTrace();
                    return false;
                }
            } else {
            	System.out.println("Conn was null");
            	return false;
            }
        } catch (Exception ee) {
        	logger.error("Threw a BadException in DBConn::editURL, full stack trace follows:", ee);
        	return false;
            
        }
	}
	
	/*
	 * deletePURL
	 * Input: String PURLID, String URL, String ERC, String Who, String What, String When
	 * executes query to edit URL of corresponding PURLID, replaces with provided URL
	 * OUTPUT: true if query was executed.  if not return false;
	 */
	public boolean deletePURL(String PURLID) {
	    
	    try {
            if(conn != null) {
                try {
                	Statement st = null;
                    st = conn.createStatement();
                    String sql = "DELETE FROM \"PURL\" WHERE \"strPurl\" = '" + PURLID.toString() + "'";
                    st.executeUpdate(sql);
                    return true;
                } catch (Exception ee){
                	logger.error("Threw a BadException in DBConn::deletePurl, full stack trace follows:", ee);
                	return false;
                } 
            } else {
            	logger.error("Conn wasn't initialized error occured in DBConn::deletePURL");
            	return false;
            }
        } catch (Exception ee) {
        	logger.error("Threw a BadException in DBConn::deletePURL, full stack trace follows:", ee);
        	return false;
            
        }
	}
	
	/*
	 * retrieveModel
	 * Input: String PURLID
	 * executes query to return model of corresponding PURLID
	 * OUTPUT: model_Purl, returns null if query was unnsuccessfull.
	 */
	public model_Purl retrieveModel(String PURLID) {
		
	    ResultSet rs = null;
	    
		model_Purl PURL = new model_Purl(PURLID);
		
		try {
            if(conn != null) {	// if there is a conection
                try {
                    Statement st = null;
                    st = conn.createStatement();
                    rs = st.executeQuery("SELECT * FROM \"PURL\" WHERE \"strPurl\" = '" + PURLID.toString() + "'");		//prepares a query string
                	if(rs.next()) {		//extracts variables
                		PURL.setURL(rs.getString(2));
                		PURL.setERC(rs.getString(3));
                		PURL.setWho(rs.getString(4));
                		PURL.setWhat(rs.getString(5));
                		PURL.setWhen(rs.getString(6));
                	} else {
                		return null;	//if result was empty return null, meaning PURLID was not in table
                	}

                } catch (Exception ee) {
                	logger.error("Threw a BadException in DBConn::retrieveModel, full stack trace follows:", ee);
                    return null;
                }
            }
        } catch (Exception ee) {
        	logger.error("Threw a BadException in DBConn::retrieveModel, full stack trace follows:", ee);
        	return null;
            
        }
		
		return PURL;		//return the PURL model
	}
	
	public String getServerCredentials() throws IOException {
 
		String result = "";
		InputStream inputStream = null;
		
		try {
			Properties prop = new Properties();
			String propFileName = "serverCredential.properties";
 
			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
 
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
			
			this.serverURL = prop.getProperty("server");
			this.username = prop.getProperty("username");
			this.password = prop.getProperty("password");
 
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			inputStream.close();
		}
		return result;
	}

}