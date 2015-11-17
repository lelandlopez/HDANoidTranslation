package com.hida;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A template for all minters should inherit from.
 *
 * As of right now tentative functionality includes...
 * <p>
 * - storing and retrieving user preferences (maybe with java beans?)
 * </p>
 * <p>
 * - connecting to database via connectDb method
 * </p>
 * <p>
 * - a check id method that will check a given id against a database
 * </p>
 * <p>
 * - implementation of a gen id method
 * </p>
 *
 * Currently deciding whether or not to merge Arkminter and this class into a
 * single class because, as far as I can tell, ids will always be generated in
 * the same, random or sequential. Ids differ only slightly from schema to
 * schema
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
     * Proposed use to retrieve any stored data from database. Deciding how
     * it'll interact with java beans. Need to read up more on it
     */
    /*
     public void retrieveSettings() {
     this.charMap = "12345abc";
     this.idPrefix = "xyz1";
     this.idLength = 10;
     }
     */
    /**
     * Proposed use is to connect to a database so that ids can be checked and
     * user settings can be retrieved
     *
     * @return - true if connection is successful
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
        ArrayList<String> redundantIdList = new ArrayList();
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

    /**
     * Checks a given id against a database.
     *
     * Perhaps another method to overload this can be used to check a list of
     * ids.
     *
     * @param id - given id
     * @return - true if id is true
     */
    public boolean checkId(String id) {

        return true;
    }

    /**
     * A method to overwritten by a subclass. This method will automatically
     * generate ids according a received token. More information on the token in
     * the ArkMinters implementation
     *
     * @param token - decides how ids are minted
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    //public abstract JsonObject genIdAuto(String token);
    /**
     * A method to overwritten by a subclass. This method will generate ids
     * sequentially or randomly according the charMapping given by user
     *
     * @param isRandom - if true then create ids randomly
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    //public abstract JsonObject genIdCustom(boolean isRandom);

    /* typical getter and setter methods */
}
