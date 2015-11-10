package HDA.PURL.DBConn;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import HDA.PURL.Model.model_Purl;

public class DBConn {

	//variables
	private String serverURL;
	private String username;
	private String password;
	private Connection conn = null;
	
	/*
	 * required String serverURL, username, password
	 */
	public DBConn(String serverURL, String username, String password) {
		this.serverURL = serverURL;
		this.username = username;
		this.password = password;
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
			ee.printStackTrace();
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
            ex.printStackTrace();
            return false;
        }
	}
	
	/*
	 * retrieveURL
	 * Input: String PURLID
	 * executes query to retrieve URL of corresponding PURLID
	 * OUTPUT: String URL STRing
	 */
	public String retrieveURL(String PURLID) {
		
	    PreparedStatement pst = null;
	    ResultSet rs = null;
	    String url = null;
		
		try {
            if(conn != null) {
                try {
                    Statement st = null;
                    st = conn.createStatement();
                    rs = st.executeQuery("SELECT \"strURL\" FROM \"PURL\" WHERE \"strPurl\" = '" + PURLID.toString() + "'");
                    // get the number of rows from the result set
                	if(rs.next()) {
                		url = rs.getString(1);
                	} else {
                		return "error: no such PURL exists";
                	}

                } catch (Exception ee) {
                    ee.printStackTrace();
                }  finally {

                    
                }
            }
        } catch (Exception ee) {
        	ee.printStackTrace();
            
        }
		return url;
		
	}
	
	/*
	 * editURL
	 * Input: String PURLID, String URL
	 * executes query to edit URL of corresponding PURLID, replaces with provided URL
	 * OUTPUT: true if query was executed.  if not return false;
	 */
	public boolean editURL(String PURLID, String URL) {
		PreparedStatement pst = null;
	    String url = null;
	    
	    try {
            if(conn != null) {
                try {
                    Statement st = null;
                    st = conn.createStatement();
                    System.out.println("UPDATE \"PURL\" SET \"strURL\" = '" + URL.toString() + "' WHERE \"strPurl\" = '" + PURLID.toString() + "'");
                    st.executeUpdate("UPDATE \"PURL\" SET \"strURL\" = '" + URL + "' WHERE \"strPurl\" = '" + PURLID + "'");
                    return true;
                    // get the number of rows from the result set
                    
                } catch (Exception ee) {
                    ee.printStackTrace();
                    return false;
                }
            } else {
            	System.out.println("Conn was null");
            	return false;
            }
        } catch (Exception ee) {
        	ee.printStackTrace();
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
		
		PreparedStatement pst = null;
	    ResultSet rs = null;
	    String url = null;
	    
		model_Purl PURL = new model_Purl(PURLID);
		
		try {
            if(conn != null) {
                try {
                    Statement st = null;
                    st = conn.createStatement();
                    rs = st.executeQuery("SELECT * FROM \"PURL\" WHERE \"strPurl\" = '" + PURLID.toString() + "'");
                    // get the number of rows from the result set
                	if(rs.next()) {
                		PURL.setURL(rs.getString(2));
                		PURL.setERC(rs.getString(3));
                		PURL.setWho(rs.getString(4));
                		PURL.setWhat(rs.getString(5));
                		PURL.setWhen(rs.getString(6));
                	} else {
                		return null;
                	}

                } catch (Exception ee) {
                    ee.printStackTrace();
                    return null;
                }
            }
        } catch (Exception ee) {
        	ee.printStackTrace();
        	return null;
            
        }
		
		return PURL;
	}
	
}


