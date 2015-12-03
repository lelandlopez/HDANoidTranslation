package com.hida;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.regex.Pattern;
import org.sqlite.Function;
import java.util.ArrayList;

/**
 * A class used to manage http requests so that data integrity can be maintained
 *
 * @author lruffin
 */
public class DatabaseManager extends Function {

    /**
     * Name of the column in which ids are stored
     */
    private final String COLUMN_NAME = "ID";

    /**
     * Name of the table in which ids are stored
     */
    private final String TABLE_NAME = "MINTED_IDS";

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
    private final String DRIVER;// = "jdbc:sqlite:";

    /**
     * Set to false by default, the flag is raised (set to true) when the table
     * was created. This is used to prevent from querying for the tables
     * existence after each request.
     */
    private boolean isTableCreatedFlag = false;

    /**
     * Set to -1 by default, the FormatIndex is used to store the index of a
     * sought-after CachedFormat. The value -1 is a sentinel and it designates
     * that the format being sought-after does not exist.
     */
    private int FormatIndex = -1;

    /**
     * Holds a list of CachedFormats. Used in together with FormatIndex to find
     * a format.
     */
    private final ArrayList<CachedFormat> CachedFormatsList = new ArrayList<CachedFormat>();

    private String DatabasePath;
    private String DatabaseName;

    /**
     *
     * @param DatabasePath
     * @param DatabaseName
     */
    public DatabaseManager(String DatabasePath, String DatabaseName) {
        this.DatabasePath = DatabasePath;
        this.DatabaseName = DatabaseName;

        this.DRIVER = "jdbc:sqlite:" + DatabasePath + DatabaseName;
    }

    /**
     *
     */
    public DatabaseManager() {
        String url = DatabaseManager.class.getClassLoader().getResource("").getFile();
        //url.
/*
         DatabaseManager.class.File resourcesDirectory = new File(url);
         this.DatabasePath = resourcesDirectory.getAbsolutePath();
         this.DatabaseName = "/minterDatabase";
         */
        //this.DRIVER = "jdbc:sqlite:" + DatabasePath + DatabaseName + ".db";
        //this.DRIVER = "jdbc:sqlite:C:\\Users\\lruffin\\Desktop\\boop\\hi.db";
        this.DatabasePath = "";
        this.DatabaseName = "PID.db";
        this.DRIVER = "jdbc:sqlite:" + DatabaseName;
        //System.out.println(DatabasePath + DatabaseName + ".db");
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

        // connect to database
        Class.forName("org.sqlite.JDBC");
        DatabaseConnection = DriverManager.getConnection(DRIVER);

        // allow the database connection to use regular expressions
        Function.create(DatabaseConnection, "REGEXP", new DatabaseManager());

        if (!isDbSetup()) {
            System.out.println("creating table");

            // string to hold a query that sets up table in SQLite3 syntax
            String sqlQuery = String.format("CREATE TABLE %s "
                    + "(%s PRIMARY KEY NOT NULL);", TABLE_NAME, COLUMN_NAME);

            // a database statement that allows database/webservice communication 
            Statement databaseStatement = DatabaseConnection.createStatement();

            // create non-existing table and clean-up
            databaseStatement.executeUpdate(sqlQuery);
            databaseStatement.close();
        }
        return true;

    }

    /**
     * Using a cached format as a guide, this method acts as a counter by
     * incrementing a the value of token's designated index.
     *
     * <pre>
     * designated indices for each token:
     * index 0 = DIGIT
     * index 1 = LOWERCASE
     * index 2 = UPPERCASE
     * index 3 = MIXEDCASE
     * index 4 = LOWER_EXTENDED
     * index 5 = UPPER_EXTENDED
     * index 6 = MIXED_EXTENDED
     * </pre>
     *
     * @param format the cached format.
     * @param idName the name of the id.
     * @param tokenIndexArray the array who's values will be incremented.
     */
    private void incrementMatchingFormats(CachedFormat format, String idName, int[] tokenIndexArray) 
        throws BadParameterException{
        /*
         Get regular expressions from the method retrieveRegex for each token. The
         2nd value is set to false as the regular expression produced will also 
         account for vowels
         */
        String regexDigit = retrieveRegex("DIGIT", false);
        String regexLower = retrieveRegex("LOWERCASE", false);
        String regexUpper = retrieveRegex("UPPERCASE", false);
        String regexMixed = retrieveRegex("MIXEDCASE", false);
        String regexLE = retrieveRegex("LOWER_EXTENDED", false);
        String regexUE = retrieveRegex("UPPER_EXTENDED", false);
        String regexExtended = retrieveRegex("MIXED_EXTENDED", false);

        String prefix = format.getPrefix();
        int rootLength = format.getRootLength();

        // increment the values at a designated index if the id matches a token pattern
        if (idName.matches(String.format("^(%2$s)%1$s{%3$s}$", regexDigit, prefix, rootLength))) {
            tokenIndexArray[0]++;
        }
        if (idName.matches(String.format("^(%2$s)%1$s{%3$s}$", regexLower, prefix, rootLength))) {
            tokenIndexArray[1]++;
        }
        if (idName.matches(String.format("^(%2$s)%1$s{%3$s}$", regexUpper, prefix, rootLength))) {
            tokenIndexArray[2]++;
        }
        if (idName.matches(String.format("^(%2$s)%1$s{%3$s}$", regexMixed, prefix, rootLength))) {
            tokenIndexArray[3]++;
        }
        if (idName.matches(String.format("^(%2$s)%1$s{%3$s}$", regexLE, prefix, rootLength))) {
            tokenIndexArray[4]++;
        }
        if (idName.matches(String.format("^(%2$s)%1$s{%3$s}$", regexUE, prefix, rootLength))) {
            tokenIndexArray[5]++;
        }
        if (idName.matches(String.format("^(%2$s)%1$s{%3$s}$", regexExtended, prefix, rootLength))) {
            tokenIndexArray[6]++;
        }
    }

    /**
     * Searches through the {@link #CachedFormatsList} to see if there are any
     * amounts to add to each formats that are similar to baseFormat. The
     * formats, as well as the amount to add, are given by tokenIndexArray. Two
     * formats are considered to be similar when they have the same RootLength,
     * SansVowel, and Prefix values.
     *
     * @param baseFormat the format that all formats in CachedFormatsList will
     * be matched against.
     * @param tokenIndexArray the array that specifies which formats, based on
     * their tokenTypes, will have their AmountCreated value increased.
     */
    private void updateFormats(CachedFormat baseFormat, int[] tokenIndexArray) {
        for (CachedFormat c : CachedFormatsList) {
            if (baseFormat.getPrefix().equals(c.getPrefix())
                    && baseFormat.SansVowel == c.isSansVowel()
                    && baseFormat.RootLength == c.RootLength) {
                if (c.TokenType.equals("DIGIT")) {
                    System.out.println("adding digit");
                    c.addAmount(tokenIndexArray[0]);
                } else if (c.TokenType.equals("LOWERCASE")) {
                    System.out.println("adding lower");
                    c.addAmount(tokenIndexArray[1]);
                } else if (c.TokenType.equals("UPPERCASE")) {
                    System.out.println("adding upper");
                    c.addAmount(tokenIndexArray[2]);
                } else if (c.TokenType.equals("MIXEDCASE")) {
                    System.out.println("adding mix");
                    c.addAmount(tokenIndexArray[3]);
                } else if (c.TokenType.equals("LOWER_EXTENDED")) {
                    System.out.println("adding le");
                    c.addAmount(tokenIndexArray[4]);
                } else if (c.TokenType.equals("UPPER_EXTENDED")) {
                    System.out.println("adding ue");
                    c.addAmount(tokenIndexArray[5]);
                } else if (c.TokenType.equals("MIXED_EXTENDED")) {
                    System.out.println("adding me");
                    c.addAmount(tokenIndexArray[6]);
                }
            }
        }
    }

    /**
     * Adds a requested amount of formatted ids to the database.
     *
     * @param list list of ids to check.
     * @throws java.sql.SQLException
     */
    public void addIdList(Set<Id> list) throws SQLException, BadParameterException {
        System.out.println("adding ids...");
        int[] matchingTokenIndex = new int[7];
        CachedFormat format = CachedFormatsList.get(FormatIndex);
        String sqlQuery1 = "";
        int counter = 0;
        for (Id id : list) {

            if (counter == 0) {
                sqlQuery1 = "INSERT INTO " + TABLE_NAME;
                sqlQuery1 += String.format(" SELECT %s AS ", id, this.COLUMN_NAME);
            } else {
                sqlQuery1 += String.format(" UNION ALL SELECT %s ", id);
            }
            if (counter == 500) {
                // a statement that allows database/webservice communication
                Statement insertStatement = DatabaseConnection.createStatement();

                // execute query
                insertStatement.executeUpdate(sqlQuery1);

                // clean-up                
                insertStatement.close();

                // increment matching tokens
                incrementMatchingFormats(format, id.toString(), matchingTokenIndex);

                counter = 0;
            }
            // increment matching tokens
            incrementMatchingFormats(format, id.toString(), matchingTokenIndex);

            /*
             // assign a sqlite3 query that adds an id to the database
             String sqlQuery = String.format("INSERT INTO %s (%s) VALUES('%s')", TABLE_NAME, COLUMN_NAME, id);

             // a statement that allows database/webservice communication
             Statement insertStatement = DatabaseConnection.createStatement();

             // execute query
             insertStatement.executeUpdate(sqlQuery);

             

             // increment matching tokens
             incrementMatchingFormats(format, id.toString(), matchingTokenIndex);
             */
        }
        
        for(int i : matchingTokenIndex){
            System.out.println(i);
        }

        // update the formats
        System.out.println("updating format list");
        updateFormats(format, matchingTokenIndex);

        System.out.println("done; printed ids to database");
        System.out.println("added new format: " + format);

    }

    /**
     * Checks a list of ids against a database to ensure that no redundancies
     * are added.
     *
     * @param list list of ids to check.
     * @return a list containing ids that already exist in database. Returns an
     * empty list of all the ids given in the param list are unique.
     * @throws java.sql.SQLException
     */
    public boolean isIdListUnique(Set<Id> list) throws SQLException {
        System.out.println("in checkId");
        // a flag that'll be raised if 
        boolean containsUniqueIds = true;
        // a list containing redundant ids

        for (Id id : list) {

            // a string to query a specific id for existence in database
            String sqlQuery = String.format("SELECT %s FROM %2$s WHERE "
                    + "%1$s = '%3$s'", COLUMN_NAME, TABLE_NAME, id);

            Statement databaseStatement = DatabaseConnection.createStatement();

            // execute statement and retrieves the result
            ResultSet databaseResponse = databaseStatement.executeQuery(sqlQuery);

            // adds id to redundantIdList if it already exists in database
            if (databaseResponse.next()) {
                System.out.print("redundant id: " + id);

                // the id is not unique and is therefore set to false
                id.setUnique(false);

                // because one of the ids weren't unique, isIdListUnique returns false
                containsUniqueIds = false;
            } else {
                // the id was unique and is therefore set to true
                id.setUnique(true);
            }
            // clean-up                
            databaseResponse.close();
            databaseStatement.close();
        }
        return containsUniqueIds;
    }

    /**
     * A method used to find any matching formats. If a format was found then
     * modify the index of FormatIndex to the iteration that it was found in.
     * Otherwise assign FormatIndex to -1, a sentinel value that specifies that
     * a format was not found.
     *
     * @param prefix The string that will be at the front of every id
     * @param token Designates what characters are contained in the id's root
     * @param rootLength Designates the length of the id's root
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * If the root does not contain vowels, the sansVowel is true; false
     * otherwise.
     */
    private void findCachedFormat(String prefix, String token, int rootLength, boolean sansVowel) {
        // if the prefix, token, and rootLength matches, return the cached format
        for (int i = 0; i < CachedFormatsList.size(); i++) {
            if (CachedFormatsList.get(i).getPrefix().equals(prefix)
                    && CachedFormatsList.get(i).getTokenType().equals(token)
                    && CachedFormatsList.get(i).getRootLength() == rootLength
                    && CachedFormatsList.get(i).isSansVowel() == sansVowel) {
                // store the index and return 
                FormatIndex = i;
                return;
            }
        }
        // assign -1 to designate that the format does not exist
        FormatIndex = -1;
    }

    /**
     * For any valid token, a regular expression is returned that'll match that
     * token's mapping.
     *
     * @param token Designates what characters are contained in the id's root
     * @return a regular expression
     */
    private String retrieveRegex(String token, boolean sansVowel) throws BadParameterException{

        if (token.equals("DIGIT")) {
            return String.format("([\\d])");
        } else if (token.equals("LOWERCASE")) {
            return (sansVowel) ? "([^aeiouyA-Z\\d])" : "([a-z])";
        } else if (token.equals("UPPERCASE")) {
            return (sansVowel) ? "([^a-zAEIOUY\\d])" : "([A-Z])";
        } else if (token.equals("MIXEDCASE")) {
            return (sansVowel) ? "([^aeiouyAEIOUY\\d])" : "(([a-z])|([A-Z]))";
        } else if (token.equals("LOWER_EXTENDED")) {
            return (sansVowel) ? "((\\d)|([^aeiouyA-Z]))" : "((\\d)|([a-z]))";
        } else if (token.equals("UPPER_EXTENDED")) {
            return (sansVowel) ? "((\\d)|([^a-zAEIOUY]))" : "((\\d)|([A-Z]))";
        } else if (token.equals("MIXED_EXTENDED")) {
            return (sansVowel) ? "((\\d)|([^aeiouyAEIOUY]))" : "((\\d)|([a-z])|([A-Z]))";
        } else {
            throw new BadParameterException(token, "caused an error in retrieveRegex");
        }
        
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
     * @param token Designates what characters are contained in the id's root
     * @param tokenMap Designates the range of possible characters that can
     * exist in the id's root.
     * @param rootLength Designates the length of the id's root
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * If the root does not contain vowels, the sansVowel is true; false
     * otherwise.
     * @return the number of possible permutations that can be added to the
     * database with the given parameters
     * @throws SQLException thrown whenever there is an error with the database
     */
    public long getPermutations(String prefix, String token, int rootLength,
            boolean sansVowel)
            throws SQLException, BadParameterException {
        // calculate the total number of possible permuations
        int base;
        if (token.equals("DIGIT")) {
            base = 10;
        } else if (token.equals("LOWERCASE") || token.equals("UPPERCASE")) {
            base = (sansVowel) ? 20 : 26;
        } else if (token.equals("MIXEDCASE")) {
            base = (sansVowel) ? 40 : 52;
        } else if (token.equals("LOWER_EXTENDED") || token.equals("UPPER_EXTENDED")) {
            base = (sansVowel) ? 30 : 36;
        } else if (token.equals("MIXED_EXTENDED")) {
            base = (sansVowel) ? 50 : 62;
        } else {
            // throw bad parameters error
            base = -1;
        }

        long totalPermutations = (long) Math.pow(base, rootLength);

        findCachedFormat(prefix, token, rootLength, sansVowel);

        if (FormatIndex != -1) {
            // return the number of remaining permutations
            CachedFormat currentFormat = CachedFormatsList.get(FormatIndex);
            return totalPermutations - currentFormat.getAmountCreated();
        } else {
            CachedFormat format = new CachedFormat(prefix, token, rootLength, sansVowel);
            CachedFormatsList.add(format);

            // create sql query to retrieve results
            Statement databaseStatement = DatabaseConnection.createStatement();
            String regex = retrieveRegex(token, sansVowel);

            System.out.println("regex = " + regex);
            String rowCount = "ROWCOUNT";
            String sqlQuery = String.format("SELECT COUNT(*) AS %5$s FROM %1$s "
                    + "WHERE %4$s REGEXP '^(%2$s)%3$s{%6$d}$';",
                    TABLE_NAME, prefix, regex, COLUMN_NAME, rowCount, rootLength);
            System.out.println(sqlQuery);

            // retrieve results
            ResultSet databaseResponse = databaseStatement.executeQuery(sqlQuery);
            long matchingIds;
            if (databaseResponse.next()) {
                matchingIds = databaseResponse.getInt(rowCount);
                format.setAmountCreated(matchingIds);
                System.out.println("numId = " + matchingIds);
            } else {
                System.out.println("returning 0");
                matchingIds = 0;
            }
            FormatIndex = CachedFormatsList.size() - 1;

            // close connections
            databaseResponse.close();
            databaseStatement.close();

            // return the number of remaining permutations
            return totalPermutations - matchingIds;
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
     * @return the number of possible permutations that can be added to the
     * database with the given parameters
     * @throws SQLException thrown whenever there is an error with the database
     */
    public long getPermutations(String prefix, boolean sansVowel, String charMap)
            throws SQLException, BadParameterException {
        // determine the base
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
            }
        }
        // get the index of the format
        String token = getToken(charMap);

        findCachedFormat(prefix, token, charMap.length(), sansVowel);
        if (FormatIndex != -1) {
            // if the format was found, return the difference between total number of 
            // permutations however many was created
            System.out.println("format found");
            CachedFormat currentFormat = CachedFormatsList.get(FormatIndex);
            return totalPermutations - currentFormat.getAmountCreated();
        } else {
            // if the format doesn't exist, create the format
            System.out.println("format not found");
            String regex = regexBuilder(sansVowel, charMap);

            CachedFormat format = new CachedFormat(prefix, token, charMap.length(), sansVowel);
            CachedFormatsList.add(format);

            // create sql query to retrieve results
            Statement databaseStatement = DatabaseConnection.createStatement();

            String rowCount = "ROWCOUNT";
            String sqlQuery = String.format("SELECT COUNT(*) AS %5$s FROM %1$s "
                    + "WHERE %4$s REGEXP '^(%2$s)%3$s$';",
                    TABLE_NAME, prefix, regex, COLUMN_NAME, rowCount);
            System.out.println(sqlQuery);

            System.out.println("regex match? " + "bun78nHB".matches(regex));
            System.out.println("regex match? " + "bunjSzMn".matches(regex));
            System.out.println("regex match? " + "bunTcfrw".matches(regex));
            System.out.println("regex match? " + "bunJcg7M".matches(regex));
            System.out.println("regex match? " + "bunwdflg".matches(regex));
            // retrieve results
            ResultSet databaseResponse = databaseStatement.executeQuery(sqlQuery);
            long matchingIds;
            //if (databaseResponse.next()) {
                matchingIds = databaseResponse.getInt(rowCount);
                format.setAmountCreated(matchingIds);
                System.out.println("numId = " + matchingIds);

           // } else {
                System.out.println("returning 0");
              //  matchingIds = 0;
            //}
            FormatIndex = CachedFormatsList.size() - 1;

            // close connections
            databaseResponse.close();
            databaseStatement.close();

            // counts the number of remaining permutations
            System.out.println("Over numPermutations12 = " + totalPermutations);
            System.out.println("Over getformat: " + format);
            for (CachedFormat formatk : CachedFormatsList) {
                System.out.println(formatk);
            }
            return totalPermutations - matchingIds;

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
            }

            // get the token map and append int to regex
            String retrievedRegex = retrieveRegex(token, sansVowel);
            regex += String.format("[%s]", retrievedRegex);
        }
        return regex;
    }

    /**
     * This method returns an equivalent token for any given charMap
     *
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits
     * @return the token equivalent to the charMap
     */
    private String getToken(String charMap) throws BadParameterException {

        // true if charMap only contains character 'd'
        if (charMap.matches("^[d]+$")) {
            return "DIGIT";
        } // true if charMap only contains character 'l'.
        else if (charMap.matches("^[l]+$")) {
            return "LOWERCASE";
        } // true if charMap only contains character 'u'
        else if (charMap.matches("^[u]+$")) {
            return "UPPERCASE";
        } // true if charMap only contains character groups 'lu' or 'm'
        else if (charMap.matches("(^(?=[lum]*l)(?=[lum]*u)[lum]*$)" + "|"
                + "(^(?=[lum]*m)[lum]*$)")) {
            return "MIXEDCASE";
        } // true if charMap only contains characters 'dl'
        else if (charMap.matches("(^(?=[dl]*l)(?=[ld]*d)[dl]*$)")) {
            return "LOWER_EXTENDED";
        } // true if charMap only contains characters 'du'
        else if (charMap.matches("(^(?=[du]*u)(?=[du]*d)[du]*$)")) {
            return "UPPER_EXTENDED";
        } // true if charMap at least contains character groups 'dlu' or 'md' or 'e' respectively
        else if (charMap.matches("(^(?=[dlume]*d)(?=[dlume]*l)(?=[dlume]*u)[dlume]*$)" + "|"
                + "(^(?=[dlume]*m)(?=[dlume]*d)[dlume]*$)" + "|"
                + "(^(?=[dlume]*e)[dlume]*$)")) {
            return "MIXED_EXTENDED";
        } else {
            throw new BadParameterException(charMap, "detected in getToken method");
        }
    }

    /**
     * Prints a list of ids to the server. Strictly used for error checking.
     *
     * @throws SQLException thrown whenever there is an error with the database
     */
    private void printData() throws SQLException {
        System.out.println("current list of items: ");
        Statement s = DatabaseConnection.createStatement();
        String sql = String.format("SELECT * FROM %s;", TABLE_NAME);
        ResultSet r = s.executeQuery(sql);

        for (int i = 0; r.next(); i++) {
            String curr = r.getString("id");
            System.out.printf("id(%d):  %s", i, curr);
        }
        System.out.println("done");
    }

    /**
     * Used by an external method to close this connection.
     *
     * @throws SQLException thrown whenever there is an error with the database
     */
    public synchronized void closeConnection() throws SQLException {
        DatabaseConnection.close();
    }

    /**
     * Checks to see if the database already has a table created. This method is
     * used to prevent constant checking on whether or not the database was
     * created along with a table.
     *
     * @param c connection to the database
     * @return true if table exists in database, false otherwise
     * @throws SQLException thrown whenever there is an error with the database
     */
    private boolean isDbSetup() throws SQLException {

        if (!isTableCreatedFlag) {
            DatabaseMetaData metaData = DatabaseConnection.getMetaData();
            ResultSet databaseResponse = metaData.getTables(null, null, TABLE_NAME, null);

            if (!databaseResponse.next()) {
                databaseResponse.close();
                return false;
            }

            // set the flag to prevent database being called every request
            databaseResponse.close();
            isTableCreatedFlag = true;
        }
        // raise the flag to prevent constant requerying
        return isTableCreatedFlag;

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

    public String getCOLUMN_NAME() {
        return COLUMN_NAME;
    }

    public String getTABLE_NAME() {
        return TABLE_NAME;
    }

    public ArrayList<CachedFormat> getCachedFormatsList() {
        return CachedFormatsList;
    }

    public String getDatabasePath() {
        return DatabasePath;
    }

    public String getDatabaseName() {

        return DatabaseName;
    }

    /**
     * Used to store the formats of previously created ids to limit unnecessary,
     * repeated database access when counting matching ids.
     */
    private class CachedFormat {

        // Fields; detailed description in Minter class
        private final String Prefix;
        private final String TokenType;
        private final int RootLength;
        private final boolean SansVowel;

        /**
         * Stores the amount of ids that were created using the format definied
         * by the other fields: Prefix, TokenType, RootLength, and SansVowel.
         */
        private long AmountCreated;

        /**
         * Create a CachedFormat to store the formats of a specific kind of id.
         * Each format will differ based on the parameters given below.
         *
         * @param prefix The string that will be at the front of every id
         * @param token Designates what characters are contained in the id's
         * root
         * @param rootLength Designates the length of the id's root
         * @param sansVowel Designates whether or not the id's root contains
         * vowels. If the root does not contain vowels, the sansVowel is true;
         * false otherwise.
         */
        private CachedFormat(String prefix, String tokenmap, int rootLength, boolean sansVowel) {
            this.Prefix = prefix;
            this.TokenType = tokenmap;
            this.SansVowel = sansVowel;
            this.RootLength = rootLength;
            this.AmountCreated = 0;
        }

        /**
         * Adds the amount to AmountCreated.
         *
         * @param amount the amount of ids to add
         * @return true if successful
         */
        public boolean addAmount(long amount) {
            AmountCreated += amount;
            return true;
        }

        // getters and setters 
        public long getAmountCreated() {
            return AmountCreated;
        }

        public void setAmountCreated(long AmountCreated) {
            this.AmountCreated = AmountCreated;
        }

        public String getPrefix() {
            return Prefix;
        }

        public String getTokenType() {
            return TokenType;
        }

        public int getRootLength() {
            return RootLength;
        }

        public boolean isSansVowel() {
            return SansVowel;
        }

        /**
         * Used for testing; subject to deletion or modification
         *
         * @return
         */
        @Override
        public String toString() {
            return String.format("p=%s, t=%s, l=%d, v=%b,a=%d", Prefix, TokenType, RootLength, SansVowel, AmountCreated);
        }
    }
}
