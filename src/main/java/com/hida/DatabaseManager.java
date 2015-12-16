package com.hida;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.regex.Pattern;
import org.sqlite.Function;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class used to manage http requests so that data integrity can be maintained
 *
 * @author lruffin
 */
public class DatabaseManager extends Function {

// Logger; logfile to be stored in resource folder
    private static final Logger Logger = LoggerFactory.getLogger(DatabaseManager.class);

// names of the columns used in the tables
    private final String ID_COLUMN = "ID";
    private final String PREFIX_COLUMN = "PREFIX";
    private final String AMOUNT_CREATED_COLUMN = "AMOUNT_CREATED";
    private final String SANS_VOWEL_COLUMN = "SANS_VOWEL";
    private final String TOKEN_TYPE_COLUMN = "TOKEN_TYPE";
    private final String ROOT_LENGTH_COLUMN = "ROOT_LENGTH";
    private final String PREPEND_COLUMN = "PREPEND";
    private final String AUTO_COLUMN = "IS_AUTO";
    private final String RANDOM_COLUMN = "IS_RANDOM";
    private final String CHAR_MAP_COLUMN = "CHAR_MAP";

    // names of the tables in the database
    private final String ID_TABLE = "MINTED_IDS";
    private final String FORMAT_TABLE = "FORMAT";
    private final String SETTINGS_TABLE = "SETTINGS";

    /**
     * A connection to a database
     */
    private Connection DatabaseConnection;

    /**
     * The JDBC driver which also specifies the location of the database. If the
     * database does not exist then it is created at the specified location
     * <p />
     * ex: "jdbc:sqlite:[path].db"
     */
    private final String DRIVER;

    /**
     * Set to false by default, the flag is raised (set to true) when the table
     * was created. This is used to prevent from querying for the tables
     * existence after each request.
     */
    private boolean isTableCreatedFlag = false;

    // holds information about the database. 
    private final String DatabasePath;
    private final String DatabaseName;

    /**
     * A constructor used to create a database at a given location and name.
     *
     * @param DatabasePath path the database will be built.
     * @param DatabaseName name of the database.
     */
    public DatabaseManager(String DatabasePath, String DatabaseName) {
        this.DatabasePath = DatabasePath;
        this.DatabaseName = DatabaseName;

        this.DRIVER = "jdbc:sqlite:" + DatabasePath + DatabaseName;
    }

    /**
     * Used by default, this method will create a database named PID.db wherever
     * the default location server is located.
     */
    public DatabaseManager() {
        this.DatabasePath = "";
        this.DatabaseName = "PID.db";
        this.DRIVER = "jdbc:sqlite:" + DatabaseName;
    }

    /**
     * Attempts to connect to the database.
     *
     * Upon connecting to the database, the method will try to detect whether or
     * not a table exists. A table is created if it does not already exists.
     *
     * @return true if connection and table creation was successful, false
     * otherwise
     * @throws ClassNotFoundException thrown whenever the JDBC driver is not
     * found
     * @throws SQLException thrown whenever there is an error with the database
     */
    public boolean createConnection() throws ClassNotFoundException, SQLException {
        System.out.println("connection created: " + DRIVER);

        // connect to database
        Class.forName("org.sqlite.JDBC");
        DatabaseConnection = DriverManager.getConnection(DRIVER);
        //Logger.info("Database Connection Created Using: org.sqlite.JDBC");
        // allow the database connection to use regular expressions
        Function.create(DatabaseConnection, "REGEXP", new DatabaseManager());
        if (this.isTableCreatedFlag) {
            return true;
        } else {
            System.out.println("creating table");
            //Logger.info("Database Created created using Regular Expressions from \"REGEX\"");
            if (!tableExists(ID_TABLE)) {
                Logger.warn("Database Not Setup, Creating Tables");
                System.out.println("creating table");

                // string to hold a query that sets up table in SQLite3 syntax
                String createIdTable = String.format("CREATE TABLE %s "
                        + "(%s PRIMARY KEY NOT NULL);", ID_TABLE, ID_COLUMN);

                Logger.info("Table Created with Query: " + createIdTable);

                // a database statement that allows database/webservice communication 
                //Logger.info("Created BUS from Database to WebService");
                Statement databaseStatement = DatabaseConnection.createStatement();
                databaseStatement.executeUpdate(createIdTable);
                databaseStatement.close();

                Logger.info("Creating Table: " + ID_TABLE + " with Column Name: " + ID_COLUMN);
            }
            if (!tableExists(FORMAT_TABLE)) {

                Logger.info("Creating Table: " + FORMAT_TABLE);
                String createFormatTable = String.format("CREATE TABLE %s "
                        + "(%s INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "%s TEXT NOT NULL, "
                        + "%s BOOLEAN NOT NULL, "
                        + "%s TEXT NOT NULL, "
                        + "%s INT NOT NULL, "
                        + "%s UNSIGNED BIG INT NOT NULL);",
                        FORMAT_TABLE, ID_COLUMN, PREFIX_COLUMN, SANS_VOWEL_COLUMN,
                        TOKEN_TYPE_COLUMN, ROOT_LENGTH_COLUMN, AMOUNT_CREATED_COLUMN);
                Logger.info("Format Table created with: " + createFormatTable);

                //Logger.info("Cleaning up Connection and Table");
                Statement databaseStatement = DatabaseConnection.createStatement();
                databaseStatement.executeUpdate(createFormatTable);
                databaseStatement.close();
            }
            if (!tableExists(SETTINGS_TABLE)) {
                Logger.info("Creating Table: " + SETTINGS_TABLE + " with Column Name: " + ID_COLUMN);
                String createSettingsTable = String.format("CREATE TABLE %s "
                        + "(%s TEXT, "
                        + "%s TEXT, "
                        + "%s TEXT, "
                        + "%s INT, "
                        + "%s TEXT, "
                        + "%s BOOLEAN NOT NULL, "
                        + "%s BOOLEAN NOT NULL, "
                        + "%s BOOLEAN NOT NULL);",
                        SETTINGS_TABLE, PREPEND_COLUMN, PREFIX_COLUMN, TOKEN_TYPE_COLUMN,
                        ROOT_LENGTH_COLUMN, CHAR_MAP_COLUMN, AUTO_COLUMN, RANDOM_COLUMN,
                        SANS_VOWEL_COLUMN);
                Logger.info("Format Table created with: " + createSettingsTable);

                //Logger.info("Cleaning up Connection and Table");
                Statement databaseStatement = DatabaseConnection.createStatement();
                databaseStatement.executeUpdate(createSettingsTable);
                databaseStatement.close();

                // populate the settings table with default settings
                assignDefaultSettings();
            }
            this.isTableCreatedFlag = true;
            return true;
        }
    }

    /**
     * Assigns default values to the settings table. Default values are as
     * follows:
     * <pre>
     * prepend = ""
     * prefix = "xyz"
     * tokenType = "DIGIT"
     * rootLength = 5
     * charMap = "ddlume"
     * auto = true
     * random = true
     * sansVowels = true
     * </pre>
     *
     * @throws SQLException thrown whenever there is an error with the database
     */
    private void assignDefaultSettings() throws SQLException {
        System.out.println("in assignDefaultSettings");
        Statement databaseStatement = DatabaseConnection.createStatement();

        String sqlQuery = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s) "
                + "VALUES ('%s', '%s', %d, '%s', %d, %d, %d);",
                SETTINGS_TABLE, PREFIX_COLUMN, TOKEN_TYPE_COLUMN,
                ROOT_LENGTH_COLUMN, CHAR_MAP_COLUMN, AUTO_COLUMN, RANDOM_COLUMN,
                SANS_VOWEL_COLUMN, "xyz", "MIXED_EXTENDED", 5, "ddlume", 1, 1, 1);

        databaseStatement.executeUpdate(sqlQuery);

        databaseStatement.close();

        printCache();

    }

    /**
     *
     * @param prepend the prepended String to be attached to each id
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param rootLength Designates the length of the id's root.
     * @param isAuto Determines which minter will be used
     * @param isRandom Determines whether the ids will be generated at random or
     * sequentially
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @throws SQLException
     */
    public void assignSettings(String prepend, String prefix, TokenType tokenType, int rootLength,
            boolean isAuto, boolean isRandom, boolean sansVowel) throws SQLException {
        System.out.println("in assignSettings 1");
        Statement databaseStatement = DatabaseConnection.createStatement();

        int autoFlag = (isAuto) ? 1 : 0;
        int randomFlag = (isRandom) ? 1 : 0;
        int vowelFlag = (sansVowel) ? 1 : 0;
        String sqlQuery = String.format("UPDATE %s SET %2$s = '%9$s', %3$s = '%10$s', "
                + "%4$s = '%11$s', %5$s = %12$s, %6$s = %13$s, %7$s = %14$s, %8$s = %15$s",
                SETTINGS_TABLE, PREPEND_COLUMN, PREFIX_COLUMN, TOKEN_TYPE_COLUMN,
                ROOT_LENGTH_COLUMN, AUTO_COLUMN, RANDOM_COLUMN,
                SANS_VOWEL_COLUMN, prepend, prefix, tokenType, rootLength,
                autoFlag, randomFlag, vowelFlag);

        databaseStatement.executeUpdate(sqlQuery);

        databaseStatement.close();

        printCache();
    }

    /**
     * Assigns values given by the parameter to the settings table
     *
     * @param prepend the prepended String to be attached to each id
     * @param prefix The string that will be at the front of every id.
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits.
     * @param isAuto Determines which minter will be used
     * @param isRandom Determines whether the ids will be generated at random or
     * sequentially
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @throws SQLException thrown whenever there is an error with the database.
     */
    public void assignSettings(String prepend, String prefix, String charMap, boolean isAuto,
            boolean isRandom, boolean sansVowel) throws SQLException {
        System.out.println("in assignSettings 2");
        Statement databaseStatement = DatabaseConnection.createStatement();

        int autoFlag = (isAuto) ? 1 : 0;
        int randomFlag = (isRandom) ? 1 : 0;
        int vowelFlag = (sansVowel) ? 1 : 0;

        String sqlQuery = String.format("UPDATE %s SET %2$s = '%8$s', %3$s = '%9$s', "
                + "%4$s = '%10$s', %5$s = %11$s, %6$s = %12$s, %7$s = %13$s",
                SETTINGS_TABLE, PREPEND_COLUMN, PREFIX_COLUMN, CHAR_MAP_COLUMN, AUTO_COLUMN, 
                RANDOM_COLUMN, SANS_VOWEL_COLUMN, prepend, prefix, charMap, 
                autoFlag, randomFlag, vowelFlag);

        databaseStatement.executeUpdate(sqlQuery);

        databaseStatement.close();
    }

    /**
     * Retrieves the default setting stored at the column in Settings table
     *
     * @param column name of the parameter to be changed
     * @return setting stored at that location
     * @throws SQLException thrown whenever there is an error with the database.
     */
    public Object retrieveSetting(String column) throws SQLException {
        System.out.println("in retrieve settings");
        Statement databaseStatement = DatabaseConnection.createStatement();

        String sqlQuery = String.format("SELECT %s FROM %s;",
                column, SETTINGS_TABLE);

        Object parameter = null;
        ResultSet result = databaseStatement.executeQuery(sqlQuery);

        if (result.next()) {
            parameter = result.getObject(column);
        }

        databaseStatement.close();
        return parameter;
    }

    /**
     * Adds a requested amount of formatted ids to the database.
     *
     * @param list list of ids to check.
     * @param amountCreated Holds the true size of the list as list.size method
     * can only return the maximum possible value of an integer.
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     * @throws SQLException thrown whenever there is an error with the database
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    public void addIdList(Set<Id> list, long amountCreated, String prefix, TokenType tokenType,
            boolean sansVowel, int rootLength) throws SQLException, BadParameterException {
        System.out.println("adding ids...");
        Logger.info("Database being updated");
        String valueQuery = "";
        String insertQuery = "INSERT INTO " + ID_TABLE + "('" + ID_COLUMN + "') VALUES ";
        Logger.info("ID inserted into: " + ID_TABLE + ", Column: " + ID_COLUMN);
        int counter = 1;
        Iterator<Id> listIterator = list.iterator();
        while (listIterator.hasNext()) {
            Id id = listIterator.next();

            // concatenate valueQuery with the current id as a value to the statement
            if (counter == 1) {
                valueQuery += String.format(" ('%s')", id);
            } else {
                valueQuery += String.format(", ('%s')", id);
            }

            /* 500 is used because, to my knowledge, the max number of values that 
             can be inserted at once is 500. The condition is also met whenever the
             id is the last id in the list.            
             */
            if (counter == 500 || !listIterator.hasNext()) {
                // finalize query
                String completeQuery = insertQuery + valueQuery + ";";
                Statement updateDatabase = DatabaseConnection.createStatement();

                // execute statement and cleanup
                updateDatabase.executeUpdate(completeQuery);
                updateDatabase.close();
                Logger.info("Database Update Finished: IDs Added");
                // reset counter and valueQuery
                counter = 0;
                valueQuery = "";
            }

            counter++;
        }

        // update table format
        System.out.println("updating table format...");
        this.addAmountCreated(prefix, tokenType, sansVowel, rootLength, amountCreated);
        System.out.print("done; printed ids to database\n");
        //Logger.info("Finished; IDs printed to Database");
        this.printFormat();
    }

    /**
     * Checks to see if the Id is valid in the database.
     *
     * @param id Id to check
     * @return true if the id doesn't exist in the database; false otherwise
     * @throws SQLException thrown whenever there is an error with the database
     */
    public boolean isValidId(Id id) throws SQLException {

        boolean isUnique;
        // a string to query a specific id for existence in database
        String sqlQuery = String.format("SELECT %s FROM %2$s WHERE "
                + "%1$s = '%3$s'", ID_COLUMN, ID_TABLE, id);

        Statement databaseStatement = DatabaseConnection.createStatement();

        // execute statement and retrieves the result
        ResultSet databaseResponse = databaseStatement.executeQuery(sqlQuery);

        // adds id to redundantIdList if it already exists in database
        if (databaseResponse.next()) {

            // the id is not unique and is therefore set to false
            id.setUnique(false);

            // because one of the ids weren't unique, isIdListUnique returns false
            isUnique = false;
        } else {
            // the id was unique and is therefore set to true
            id.setUnique(true);
            isUnique = true;
        }
        // clean-up                
        databaseResponse.close();
        databaseStatement.close();

        return isUnique;
    }

    /**
     * For any valid DatabaseManager, a regular expression is returned that'll
     * match that DatabaseManager's mapping.
     *
     * @param tokenType Designates what characters are contained in the id's
     * root
     * @return a regular expression
     */
    private String retrieveRegex(String tokenType, boolean sansVowel)
            throws BadParameterException {

        if (tokenType.equals("DIGIT")) {
            return String.format("([\\d])");
        } else if (tokenType.equals("LOWERCASE")) {
            return (sansVowel) ? "([^aeiouyA-Z\\d])" : "([a-z])";
        } else if (tokenType.equals("UPPERCASE")) {
            return (sansVowel) ? "([^a-zAEIOUY\\d])" : "([A-Z])";
        } else if (tokenType.equals("MIXEDCASE")) {
            return (sansVowel) ? "([^aeiouyAEIOUY\\d])" : "(([a-z])|([A-Z]))";
        } else if (tokenType.equals("LOWER_EXTENDED")) {
            return (sansVowel) ? "((\\d)|([^aeiouyA-Z]))" : "((\\d)|([a-z]))";
        } else if (tokenType.equals("UPPER_EXTENDED")) {
            return (sansVowel) ? "((\\d)|([^a-zAEIOUY]))" : "((\\d)|([A-Z]))";
        } else if (tokenType.equals("MIXED_EXTENDED")) {
            return (sansVowel) ? "((\\d)|([^aeiouyAEIOUY]))" : "((\\d)|([a-z])|([A-Z]))";
        } else {
            Logger.error("Error found in REGEX used for retrieveRegex");

            throw new BadParameterException(tokenType, "caused an error in retrieveRegex");
        }

    }

    /**
     * Gets the total number of permutations using the given parameters.
     * Primarily used by AutoMinters
     *
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param rootLength Designates the length of the id's root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @return the number of total possible permutations.
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    public long getTotalPermutations(TokenType tokenType, int rootLength, boolean sansVowel)
            throws BadParameterException {
        System.out.println("in getTotalPermutations 1");

        // get the base of each character
        int base;
        switch (tokenType) {
            case DIGIT:
                base = 10;
                break;
            case LOWERCASE:
            case UPPERCASE:
                base = (sansVowel) ? 20 : 26;
                break;
            case MIXEDCASE:
                base = (sansVowel) ? 40 : 52;
                break;
            case LOWER_EXTENDED:
            case UPPER_EXTENDED:
                base = (sansVowel) ? 30 : 36;
                break;
            case MIXED_EXTENDED:
                base = (sansVowel) ? 50 : 62;
                break;
            default:
                throw new BadParameterException(tokenType, "Token Type");
        }

        // raise it to the power of how ever long the rootLength is
        System.out.println("total permutations = " + ((long) Math.pow(base, rootLength)));
        return ((long) Math.pow(base, rootLength));
    }

    /**
     * Gets the total number of permutations using the given parameters.
     * Primarily used by CustomMinters
     *
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @return the number of total possible permutations
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    public long getTotalPermutations(String charMap, boolean sansVowel)
            throws BadParameterException {
        System.out.println("in getTotalPermutations 2");
        long totalPermutations = 1;
        for (int i = 0; i < charMap.length(); i++) {
            if (charMap.charAt(i) == 'd') {
                totalPermutations *= 10;
            } else if (charMap.charAt(i) == 'l' || charMap.charAt(i) == 'u') {
                totalPermutations *= (sansVowel) ? 20 : 26;
            } else if (charMap.charAt(i) == 'm') {
                totalPermutations *= (sansVowel) ? 40 : 52;
            } else if (charMap.charAt(i) == 'e') {
                totalPermutations *= (sansVowel) ? 50 : 62;
            } else {
                Logger.error("Error in Total permutations");
                throw new BadParameterException(charMap,
                        "Char Map");
            }
        }
        return totalPermutations;
    }

    /**
     * Creates a format using the given parameters. The format will be stored in
     * the FORMAT table of the database. The amount created column of the format
     * will always be initialized to a value of 0.
     *
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     * @throws SQLException thrown whenever there is an error with the database
     */
    protected void createFormat(String prefix, TokenType tokenType, boolean sansVowel,
            int rootLength) throws SQLException {
        System.out.println("in createFormat");
        Statement databaseStatement = DatabaseConnection.createStatement();

        int vowelFlag = (sansVowel) ? 1 : 0;
        String sqlQuery = String.format("INSERT INTO %s (%s, %s, %s, %s, %s) "
                + "VALUES ('%s', '%s', %d, %d, 0)",
                FORMAT_TABLE, PREFIX_COLUMN, TOKEN_TYPE_COLUMN, SANS_VOWEL_COLUMN,
                ROOT_LENGTH_COLUMN, AMOUNT_CREATED_COLUMN, prefix, tokenType, vowelFlag,
                rootLength);
        Logger.info("Format Updated: " + sqlQuery);
        databaseStatement.executeUpdate(sqlQuery);

        databaseStatement.close();
    }

    /**
     * Retrieves the value in the amountCreated column of a format and adds onto
     * it. The parameters given describe the format to be affected.
     *
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     * @param amountCreated The number of ids to add to the format.
     * @throws SQLException thrown whenever there is an error with the database.
     */
    public void addAmountCreated(String prefix, TokenType tokenType, boolean sansVowel,
            int rootLength, long amountCreated) throws SQLException {
        System.out.println("in addAmountCreated");
        Statement databaseStatement = DatabaseConnection.createStatement();

        long currentAmount = getAmountCreated(prefix, tokenType, sansVowel, rootLength);

        int vowelFlag = (sansVowel) ? 1 : 0;

        long newAmount = currentAmount + amountCreated;

        String sqlQuery = String.format("UPDATE %s SET %s = %d WHERE %s = '%s' AND "
                + "%s = '%s' AND %s = %d AND %s = %d;",
                FORMAT_TABLE, AMOUNT_CREATED_COLUMN, newAmount, PREFIX_COLUMN, prefix,
                TOKEN_TYPE_COLUMN, tokenType, SANS_VOWEL_COLUMN, vowelFlag,
                ROOT_LENGTH_COLUMN, rootLength);

        databaseStatement.executeUpdate(sqlQuery);
        databaseStatement.close();
    }

    /**
     * Sets the value of the amountCreated column of a format without regarding
     * the previous value. The parameters given describe the format to be
     * affected.
     *
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     * @param amountCreated The number of ids to insert into the format.
     * @throws SQLException thrown whenever there is an error with the database.
     */
    protected void setAmountCreated(String prefix, TokenType tokenType, boolean sansVowel,
            int rootLength, long amountCreated) throws SQLException {
        System.out.println("in setAmountCreated");
        Statement databaseStatement = DatabaseConnection.createStatement();

        int vowelFlag = (sansVowel) ? 1 : 0;

        String sqlQuery = String.format("UPDATE %s SET %s = %d WHERE %s = '%s' AND "
                + "%s = '%s' AND %s = %d AND %s = %d;",
                FORMAT_TABLE, AMOUNT_CREATED_COLUMN, amountCreated, PREFIX_COLUMN, prefix,
                TOKEN_TYPE_COLUMN, tokenType, SANS_VOWEL_COLUMN, vowelFlag,
                ROOT_LENGTH_COLUMN, rootLength);

        databaseStatement.executeUpdate(sqlQuery);
        databaseStatement.close();
    }

    /**
     * Retrieves the value in the amountCreated column of a format, identified
     * by the given parameters.
     *
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     * @return The value stored in the amountCreated column. Returns -1 if a
     * format was not found.
     * @throws SQLException thrown whenever there is an error with the database.
     */
    public long getAmountCreated(String prefix, TokenType tokenType, boolean sansVowel,
            int rootLength) throws SQLException {

        System.out.println("in getAmountCreated");
        Statement databaseStatement = DatabaseConnection.createStatement();

        int vowelFlag = (sansVowel) ? 1 : 0;
        String sqlQuery = String.format("SELECT %s FROM %s WHERE %s = '%s' AND "
                + "%s = '%s' AND %s = %d AND %s = %d;",
                AMOUNT_CREATED_COLUMN, FORMAT_TABLE, PREFIX_COLUMN, prefix,
                TOKEN_TYPE_COLUMN, tokenType, SANS_VOWEL_COLUMN, vowelFlag,
                ROOT_LENGTH_COLUMN, rootLength);
        System.out.println(sqlQuery);
        ResultSet result = databaseStatement.executeQuery(sqlQuery);
        long value;
        if (result.next()) {
            value = result.getLong(AMOUNT_CREATED_COLUMN);
        } else {
            // format wasn't found
            value = -1;
        }

        result.close();
        databaseStatement.close();
        return value;

    }

    /**
     * Checks the database to see if a specific format exists using the given
     * parameters below.
     *
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     * @return true if the format exists, false otherwise.
     * @throws SQLException thrown whenever there is an error with the database.
     */
    public boolean formatExists(String prefix, TokenType tokenType, boolean sansVowel,
            int rootLength) throws SQLException {
        System.out.println("in formatExists");
        Statement databaseStatement = DatabaseConnection.createStatement();

        int vowelFlag = (sansVowel) ? 1 : 0;
        String sqlQuery = String.format("SELECT %s FROM %s WHERE %s = '%s' AND "
                + "%s = '%s' AND %s = %d AND %s = %d;",
                AMOUNT_CREATED_COLUMN, FORMAT_TABLE, PREFIX_COLUMN, prefix,
                TOKEN_TYPE_COLUMN, tokenType, SANS_VOWEL_COLUMN, vowelFlag,
                ROOT_LENGTH_COLUMN, rootLength);
        Logger.info("Format Created: " + sqlQuery);
        System.out.println(sqlQuery);
        ResultSet result = databaseStatement.executeQuery(sqlQuery);
        boolean value;
        if (result.next()) {
            value = true;
        } else {
            // format wasn't found
            value = false;
        }

        result.close();
        databaseStatement.close();
        return value;
    }

    /**
     * Used by AutoMinters to determine the number of permutations are
     * available.
     *
     * Each id will be matched against a CachedFormat, specified by this
     * method's parameter. If a matching CachedFormat does not exist, then a
     * CachedFormat will be created for it and the database will be searched to
     * see how many other ids exist in the similar formats.
     *
     * Once a format exists the database will calculate the remaining number of
     * permutations that can be created using the given parameters
     *
     * @param prefix The string that will be at the front of every id
     * @param tokenType Designates what characters are contained in the id's
     * root
     * @param rootLength Designates the length of the id's root
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @return the number of possible permutations that can be added to the
     * database with the given parameters
     * @throws SQLException thrown whenever there is an error with the database
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    public long getPermutations(String prefix, TokenType tokenType, int rootLength,
            boolean sansVowel)
            throws SQLException, BadParameterException {
        System.out.println("in getPermutations 1");
        //Logger.info("in Permutations 1");

        // calculate the total number of possible permuations        
        long totalPermutations = getTotalPermutations(tokenType, rootLength, sansVowel);
        long amountCreated = getAmountCreated(prefix, tokenType, sansVowel, rootLength);

        // format wasn't found
        if (amountCreated == -1) {
            System.out.println("\tformat doesnt exist");
            Logger.warn("Format Doesn't Exist");
            this.createFormat(prefix, tokenType, sansVowel, rootLength);
            return totalPermutations;
        } else {
            // format was found
            System.out.println("\tformat exists");
            Logger.info("format Exists");
            return totalPermutations - amountCreated;
        }

    }

    /**
     * Used by CustomMinters to determine the number of permutations are
     * available.
     *
     * Each id will be matched against a CachedFormat, specified by this
     * method's parameter. If a matching CachedFormat does not exist, then a
     * CachedFormat will be created for it and the database will be searched to
     * see how many other ids exist in the similar formats.
     *
     * Once a format exists the database will calculate the remaining number of
     * permutations that can be created using the given parameters
     *
     * @param prefix The string that will be at the front of every id
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * If the root does not contain vowels, the sansVowel is true; false
     * otherwise.
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits
     * @param tokenType Designates what characters are contained in the id's
     * root
     * @return the number of possible permutations that can be added to the
     * database with the given parameters
     * @throws SQLException thrown whenever there is an error with the database
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    public long getPermutations(String prefix, boolean sansVowel, String charMap,
            TokenType tokenType) throws SQLException, BadParameterException {
        System.out.println("in getPermutations 2");
        // calculate the total number of possible permuations        
        long totalPermutations = this.getTotalPermutations(charMap, sansVowel);
        long amountCreated = getAmountCreated(prefix, tokenType, sansVowel, charMap.length());

        // format wasn't found
        if (amountCreated == -1) {
            System.out.println("\tformat doesnt exist");
            Logger.warn("Format Doesn;t Exist");
            this.createFormat(prefix, tokenType, sansVowel, charMap.length());
            return totalPermutations;
        } else {
            // format was found
            System.out.println("\tformat exists");
            Logger.info("format exists");
            return totalPermutations - amountCreated;
        }
    }

    /**
     * Refers to retrieveRegex method to build regular expressions to match each
     * character of charMap.
     *
     * The characters correspond to a unique tokenMapping and is dependent on
     * sansVowel. SansVowel acts as a toggle between regular expressions that
     * include or excludes vowels.
     *
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * If the root does not contain vowels, the sansVowel is true; false
     * otherwise.
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits
     * @return the regular expression
     */
    private String regexBuilder(boolean sansVowel, String charMap) throws BadParameterException {
        String regex = "";
        for (int i = 0; i < charMap.length(); i++) {
            char c = charMap.charAt(i);
            String token;
            // get the correct token
            if (c == 'd') {
                token = "DIGIT";
            } else if (c == 'l') {
                token = "LOWERCASE";
            } else if (c == 'u') {
                token = "UPPERCASE";
            } else if (c == 'm') {
                token = "MIXEDCASE";
            } else if (c == 'e') {
                token = "MIXED_EXTENDED";
            } else {
                token = "error";
                Logger.error("error found in Regex Builder");
            }

            // get the token map and append int to regex
            String retrievedRegex = retrieveRegex(token, sansVowel);
            regex += String.format("[%s]", retrievedRegex);
        }
        return regex;
    }

    /**
     * Prints a list of ids to the server. Strictly used for error checking.
     *
     * @throws SQLException thrown whenever there is an error with the database
     */
    private void printData() throws SQLException {
        System.out.println("current list of items: ");
        Statement s = DatabaseConnection.createStatement();
        String sql = String.format("SELECT * FROM %s;", ID_TABLE);
        ResultSet r = s.executeQuery(sql);

        for (int i = 0; r.next(); i++) {
            String curr = r.getString("id");
            System.out.printf("id(%d):  %s", i, curr);
            //Logger.info("Generated ID: "+i+" "+curr);
        }
        System.out.println("done");
        //Logger.info("Done with Print");
    }

    /**
     * Prints entire list of formats to console
     *
     * @throws SQLException thrown whenever there is an error with the database
     */
    public void printFormat() throws SQLException {
        System.out.println("current list of items: ");
        Statement s = DatabaseConnection.createStatement();
        String sql = String.format("SELECT * FROM %s;", FORMAT_TABLE);
        ResultSet r = s.executeQuery(sql);

        while (r.next()) {
            int id = r.getInt("id");
            String prefix = r.getString("PREFIX");
            String tokenType = r.getString("TOKEN_TYPE");
            boolean sansVowel = r.getBoolean("SANS_VOWEL");
            int rootLength = r.getInt("ROOT_LENGTH");
            long amountCreated = r.getLong("AMOUNT_CREATED");

            System.out.printf("%10d):%20s%20s%20b%20d%20d\n",
                    id, prefix, tokenType, sansVowel, rootLength, amountCreated);
        }

        System.out.println("done");
    }

    /**
     * Prints the current default settings
     *
     * @throws SQLException
     */
    public void printCache() throws SQLException {
        System.out.println("current list of items: ");
        Statement s = DatabaseConnection.createStatement();
        String sql = String.format("SELECT * FROM %s;", SETTINGS_TABLE);
        ResultSet r = s.executeQuery(sql);

        while (r.next()) {
            String prepend = r.getString(this.getPREPEND_COLUMN());
            String prefix = r.getString(this.getPREFIX_COLUMN());
            String tokenType = r.getString(this.getTOKEN_TYPE_COLUMN());
            String charMap = r.getString(this.getCHAR_MAP_COLUMN());
            boolean sansVowel = r.getBoolean(this.getSANS_VOWEL_COLUMN());
            boolean isAuto = r.getBoolean(this.getAUTO_COLUMN());
            boolean isRandom = r.getBoolean(this.getRANDOM_COLUMN());
            int rootLength = r.getInt(this.getROOT_LENGTH_COLUMN());

            System.out.printf("%20s%20s%20s%20s%20b%20b%20b%20d\n",
                    prepend, prefix, tokenType, charMap, sansVowel, isAuto, isRandom, rootLength);
        }

        System.out.println("done");
    }

    /**
     * Used by an external method to close this connection.
     *
     * @throws SQLException thrown whenever there is an error with the database
     */
    public void closeConnection() throws SQLException {
        Logger.info("DB Connection Closed");
        DatabaseConnection.close();
    }

    /**
     * Checks to see if the database already has a minted and format tables
     * created. This method is used to prevent constant checking on whether or
     * not the database was created along with a table on every request.
     *
     * @param c connection to the database
     * @return true if table exists in database, false otherwise
     * @throws SQLException thrown whenever there is an error with the database
     */
    private boolean isDbSetup() throws SQLException {

        if (!isTableCreatedFlag) {
            DatabaseMetaData metaData = DatabaseConnection.getMetaData();
            ResultSet idTableExists = metaData.getTables(null, null, ID_TABLE, null);
            ResultSet formatTableExists = metaData.getTables(null, null, FORMAT_TABLE, null);

            if (!(idTableExists.next() && formatTableExists.next())) {
                idTableExists.close();
                formatTableExists.close();
                Logger.info("Database has not been Setup");

                return false;
            }

            // set the flag to prevent database being called for table existence every request
            idTableExists.close();
            formatTableExists.close();
            Logger.info("Database has been configured");

            isTableCreatedFlag = true;
        }
        // raise the flag to prevent constant requerying
        return isTableCreatedFlag;

    }

    /**
     * Checks to see if the database for the existence of a particular table. If
     * the database returns a null value then the given tableName does not exist
     * within the database.
     *
     * @param c connection to the database
     * @return true if table exists in database, false otherwise
     * @throws SQLException thrown whenever there is an error with the database
     */
    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData metaData = DatabaseConnection.getMetaData();

        ResultSet databaseResponse = metaData.getTables(null, null, tableName, null);
        boolean flag;
        System.out.println("in tableExists");

        if (!databaseResponse.next()) {
            flag = false;
        } else {
            flag = true;
        }

        // close connection
        databaseResponse.close();
        return flag;
    }

    /**
     * Creates a method to allow a database connection to use regular
     * expressions.
     *
     * @throws SQLException thrown whenever there is an error with the database
     */
    @Override
    protected void xFunc() throws SQLException {
        String expression = value_text(0);
        String value = value_text(1);
        if (value == null) {
            value = "";
        }

        Pattern pattern = Pattern.compile(expression);
        result(pattern.matcher(value).find() ? 1 : 0);
    }

    /* typical getters and setters */
    public String getPREFIX_COLUMN() {
        return PREFIX_COLUMN;
    }

    public String getSANS_VOWEL_COLUMN() {
        return SANS_VOWEL_COLUMN;
    }

    public String getTOKEN_TYPE_COLUMN() {
        return TOKEN_TYPE_COLUMN;
    }

    public String getROOT_LENGTH_COLUMN() {
        return ROOT_LENGTH_COLUMN;
    }

    public String getPREPEND_COLUMN() {
        return PREPEND_COLUMN;
    }

    public String getAUTO_COLUMN() {
        return AUTO_COLUMN;
    }

    public String getRANDOM_COLUMN() {
        return RANDOM_COLUMN;
    }

    public String getCHAR_MAP_COLUMN() {
        return CHAR_MAP_COLUMN;
    }

    public String getSETTINGS_TABLE() {
        return SETTINGS_TABLE;
    }

    public String getID_COLUMN() {
        return ID_COLUMN;
    }

    public String getID_TABLE() {
        return ID_TABLE;
    }

    public String getFORMAT_TABLE() {
        return FORMAT_TABLE;
    }

    public String getDRIVER() {
        return DRIVER;
    }

    public String getDatabasePath() {
        return DatabasePath;
    }

    public String getDatabaseName() {
        return DatabaseName;
    }

}
