package com.hida;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A class used to manage http requests so that data integrity can be maintained
 *
 * @author lruffin
 */
public class DatabaseManager {

    // fields
    private final String COLUMN_NAME = "ID";
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
                System.out.println("creating table");

                // string to hold a query that sets up table in SQLite3 syntax
                String sqlQuery = String.format("CREATE TABLE %s "
                        + "(%s PRIMARY KEY NOT NULL);", TABLE_NAME, COLUMN_NAME);

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
     */
    public synchronized void addId(Set<Id> list) {
        //ArrayList<String> redundantIdList = new ArrayList();
        System.out.println("adding ids...");
        for (Id id : list) {
            try {

                // a string to hold sqlite3 queries
                String sqlQuery;

                // assign a sqlite3 query that adds an id to the database
                
                sqlQuery = String.format("INSERT INTO %s (%s) "
                        + "VALUES('%s')", TABLE_NAME, COLUMN_NAME, id);

                // a statement that allows database/webservice communication
                Statement insertStatement
                        = DATABASE_CONNECTION.createStatement();

                // execute query
                insertStatement.executeUpdate(sqlQuery);
                

                // clean-up                
                insertStatement.close();
            } catch (SQLException exception) {
                System.out.println("duplicate id: " + id);
                System.err.println(exception.getMessage() + " in addId");
                System.err.println(Arrays.toString(exception.getStackTrace()));
            }
        }        
        System.out.println("done; printed to database");
        // prints out to server what ids were created
        //printData();

    }

    
    
    /**
     * Checks a list of ids against a database to ensure that no redundancies
     * are added.
     *
     * @param list - list of ids to check.
     * @return - a list containing ids that already exist in database. Returns
     * an empty list of all the ids given in the param list are unique.
     */
    public boolean checkId(Set<Id> list) {
        System.out.println("in checkId");
        // a flag that'll be raised if 
        boolean containsUniqueIds = true;
        // a list containing redundant ids
        try {

            // a string to query a specific id
            String sqlQuery;

            for (Id id : list) {

                // a string to query a specific id for existence in database
                sqlQuery = String.format("SELECT %s FROM %2$s WHERE "
                        + "%1$s = '%3$s'", COLUMN_NAME, TABLE_NAME, id);
                
                Statement databaseStatement
                        = DATABASE_CONNECTION.createStatement();

                // execute statement and retrieves the result
                ResultSet databaseResponse
                        = databaseStatement.executeQuery(sqlQuery);

                // adds id to redundantIdList if it already exists in database
                if (databaseResponse.next()) {
                    System.out.print("redundant id: " + id + " " + id.hashCode());
                    id.setIsUnique(false);

                    containsUniqueIds = false;
                }else{
                    id.setIsUnique(true);
                }

                // clean-up                
                databaseResponse.close();
                databaseStatement.close();

            }

        } catch (SQLException exception) {
            System.err.println(exception.getMessage() + " in checkId");
            System.err.println(Arrays.toString(exception.getStackTrace()));
        }
        return containsUniqueIds;
    }

    /**
     * Using a given prefix and length, this method will count and return the
     * number of matching ids in the database.
     *
     * @param prefix - given prefix
     * @param length - length of each id
     * @return - returns the number of matches
     */
    public long checkPrefix(String prefix, int length) {
        try {
            // create statement
            Statement databaseStatement = DATABASE_CONNECTION.createStatement();

            // create a pad where '_' represents one character in sqlite3           
            String pad = "";
            for (int i = 0; i < length; i++) {
                pad += "_";
            }

            System.out.println("database data: ");
            // create sql query to find all ids that match given parameters            
            String rowCount = "ROWCOUNT";
            String sqlQuery = String.format("SELECT COUNT(*) AS %5$s FROM %1$s "
                    + "WHERE %4$s LIKE '%2$s%3$s';",
                    TABLE_NAME, prefix, pad, COLUMN_NAME, rowCount);
            System.out.println(String.format("SELECT COUNT(*) AS %5$s FROM %1$s "
                    + "WHERE %4$s LIKE '%2$s%3$s';",
                    TABLE_NAME, prefix, pad, COLUMN_NAME, rowCount));

            // retrieve results
            ResultSet databaseResponse
                    = databaseStatement.executeQuery(sqlQuery);
            if (databaseResponse.next()) {
                int numId = databaseResponse.getInt(rowCount);
                System.out.println("numId = " + numId);
                return numId;
            } else {
                System.out.println("returning 0");
                return 0;
            }

            
        } catch (SQLException exception) {
            System.err.println(exception.getMessage() + " in checkPrefix");
            System.err.println(Arrays.toString(exception.getStackTrace()));
        }
        // error value, will revise later
        return -1;

    }

    /**
     * Prints a list of ids to the server. Strictly used for error checking.
     */
    private void printData() {
        System.out.println("current list of items: ");
        try {
            Statement s = DATABASE_CONNECTION.createStatement();
            String sql = String.format("SELECT * FROM %s;", TABLE_NAME);
            ResultSet r = s.executeQuery(sql);

            for (int i = 0; r.next(); i++) {
                String curr = r.getString("id");
                System.out.printf("id(%d):  %s", i, curr);
            }
            System.out.println("done");
        } catch (SQLException e) {
            System.err.println("printData: " + e.getMessage());
        }

    }

    /**
     * Used by an external method to close this connection.
     */
    public synchronized void closeConnection() {
        try {
            DATABASE_CONNECTION.close();
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
        }
    }

    /**
     * Checks to see if the database already has a table created. This method is
     * used to prevent constant checking on whether or not the database was
     * created along with a table.
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
