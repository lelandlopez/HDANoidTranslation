package com.hida;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * A class used to manage http requests so that data integrity can be
 * maintained in the database
 *
 * @author lruffin
 */
public class DatabaseManager {

    // fields
    private final String TABLE_NAME = "MINTED_IDS";
    private Connection DATABASE_CONNECTION;
    private final String DRIVER = "jdbc:sqlite:PID.db";
    private boolean isTableCreatedFlag = false;

    
    /**
     * Attempt to connect to the database.
     *
     * @return - true if connection is successful, false otherwise
     */
    public synchronized boolean createConnection() {
        try {
            // connect to database
            Class.forName("org.sqlite.JDBC");
            DATABASE_CONNECTION = DriverManager.getConnection(DRIVER);
            if (!isDbSetup()) {

                // string to hold a query that sets up table in SQLite3 syntax
                String sqlQuery = String.format("CREATE TABLE %s "
                        + "(ID PRIMARY KEY NOT NULL);", TABLE_NAME);

                // a database bus that allows database/webservice communication 
                Statement databaseBus = DATABASE_CONNECTION.createStatement();

                // create non-existing table and clean-up
                databaseBus.executeUpdate(sqlQuery);
                databaseBus.close();
            }
            return true;
        } catch (SQLException exception) {
            System.err.println(exception.getMessage());
            return false;
        } catch (ClassNotFoundException exception) {
            System.err.println(exception.getMessage());
            return false;
        }

    }

    /**
     * Adds a requested amount of formatted ids to the database.
     *
     * @param list - list of ids to check.
     * @return - a list containing ids that were found in database, contains
     * null pointer if an error was found.
     */
    public synchronized ArrayList<String> addId(ArrayList<String> list) {
        
        try {

            // a string to query a specific id
            String sqlQuery;

            for (String id : list) {
                // a database bus that allows database/webservice communication 
                //Statement databaseBus = DATABASE_CONNECTION.createStatement();

                // a string to query a specific id
                sqlQuery = String.format("SELECT id FROM %1$s WHERE "
                        + "ID = '%2$s'", TABLE_NAME, id);

                // a statement that allows database/webservice communication
                Statement databaseStatement = 
                        DATABASE_CONNECTION.createStatement();

                ResultSet databaseResponse = 
                        databaseStatement.executeQuery(sqlQuery);

                String retrievedId = databaseResponse.getString("id");
                
                // clean-up                
                databaseResponse.close();
                databaseStatement.close();
                if (id.matches(retrievedId)) {
                    list.remove(id);
                } else {
                    sqlQuery = String.format("INSERT INTO %s (ID) "
                            + "VALUES('%s')", TABLE_NAME, id);

                    Statement insertStatement = 
                            DATABASE_CONNECTION.createStatement();
                    insertStatement.executeUpdate(sqlQuery);
                }
                
                databaseStatement.close();

            }

        } catch (SQLException exception) {
            System.err.println(exception.getMessage() + " in addId");
            System.err.println(Arrays.toString(exception.getStackTrace()));
        }
        return list;
    }

    public synchronized void closeConnection() {
        try {
            DATABASE_CONNECTION.close();
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
        }
    }

    /**
     * Checks to see if the database already has a table created.
     *
     * @param c - connection to the database
     * @return - true if table exists in database, false otherwise
     * @throws SQLException - thrown when there are database errors
     */
    private boolean isDbSetup() throws SQLException {
        try {
            if (!isTableCreatedFlag) {
                DatabaseMetaData metaData = DATABASE_CONNECTION.getMetaData();
                ResultSet databaseResponse
                        = metaData.getTables(null, null, TABLE_NAME, null);

                if (!databaseResponse.next()) {
                    databaseResponse.close();
                    return false;
                }

                // set the flag to prevent database being called every request
                databaseResponse.close();
                isTableCreatedFlag = true;
            }

            return isTableCreatedFlag;
        } catch (SQLException exception) {
            System.err.println(exception.getMessage() + " in isDbSetup");
            return false;
        }
    }
       
}
