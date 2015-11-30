package com.hida;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
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

    // fields
    private final String COLUMN_NAME = "ID";
    private final String TABLE_NAME = "MINTED_IDS";
    private Connection DATABASE_CONNECTION;
    private final String DRIVER = "jdbc:sqlite:PID.db";
    private boolean isTableCreatedFlag = false;
    private int formatIndex = -1;
    private final ArrayList<CachedFormat> CachedFormatsList = new ArrayList<CachedFormat>();

    /**
     * Attempt to connect to the database.
     *
     * @return - true if connection is successful, false otherwise
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     */
    public synchronized boolean createConnection() throws ClassNotFoundException, SQLException {

        // connect to database
        Class.forName("org.sqlite.JDBC");
        DATABASE_CONNECTION = DriverManager.getConnection(DRIVER);

        // allow the database connection to use regular expressions
        Function.create(DATABASE_CONNECTION, "REGEXP", new DatabaseManager());

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

    }

    /**
     * Adds a requested amount of formatted ids to the database.
     *
     * @param list - list of ids to check.
     */
    public synchronized void addId(Set<Id> list) throws SQLException {
        System.out.println("adding ids...");
        for (Id id : list) {

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

        }
        CachedFormat format;
        format = CachedFormatsList.get(formatIndex);
        format.setAmountCreated(format.getAmountCreated() + list.size());

        System.out.println("done; printed to database");
        System.out.println("addformat: " + format);
        for (CachedFormat formatk : CachedFormatsList) {
            System.out.println(formatk);
        }

    }

    /**
     * Checks a list of ids against a database to ensure that no redundancies
     * are added.
     *
     * @param list - list of ids to check.
     * @return - a list containing ids that already exist in database. Returns
     * an empty list of all the ids given in the param list are unique.
     * @throws java.sql.SQLException
     */
    public boolean checkId(Set<Id> list) throws SQLException {
        System.out.println("in checkId");
        // a flag that'll be raised if 
        boolean containsUniqueIds = true;
        // a list containing redundant ids

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
                id.setUnique(false);

                containsUniqueIds = false;
            } else {
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
     * modify the index of formatIndex to the iteration that it was found in.
     * Otherwise assign formatIndex to -1, a sentinel value that specifies that
     * a format was not found.
     *
     * @param prefix
     * @param token
     * @param rootLength
     */
    private void findCachedFormat(String prefix, String idType, int rootLength, boolean sansVowel) {
        // if the prefix, tokenType, and rootLength matches, return the cached format
        for (int i = 0; i < CachedFormatsList.size(); i++) {
            if (CachedFormatsList.get(i).getPrefix().equals(prefix)
                    && CachedFormatsList.get(i).getIdMap().equals(idType)
                    && CachedFormatsList.get(i).getRootLength() == rootLength
                    && CachedFormatsList.get(i).isSansVowel() == sansVowel) {
                formatIndex = i;
                return;
            }
        }
        formatIndex = -1;
    }

    /**
     * For any valid token, a regular expression is returned that'll match that
     * token's mapping.
     *
     * @param token - a token that maps to a valid tokenMap
     * @return a regular expression
     */
    private String retrieveRegex(String token) {

        String regex;
        if (token.equals("DIGIT")) {
            return String.format("([\\d])");
        } else if (token.equals("LOWERCASE")) {
            return "([a-z])";
        } else if (token.equals("UPPERCASE")) {
            return "([A-Z])";
        } else if (token.equals("MIXEDCASE")) {
            return "(([a-z])|([A-Z]))";
        } else if (token.equals("LOWER_EXTENDED")) {
            return "((\\d)|([a-z]))";
        } else if (token.equals("UPPER_EXTENDED")) {
            return "((\\d)|([A-Z]))";
        } else if (token.equals("MIXED_EXTENDED")) {
            return "((\\d)|([a-z])|([A-Z]))";
        } else if (token.equals("SANS_VOWEL_LOWER")) {
            return "([^aeiouyA-Z])";
        } else if (token.equals("SANS_VOWEL_UPPER")) {
            return "([^a-zAEIOUY])";
        } else if (token.equals("SANS_VOWEL_MIXED")) {
            return "([^aeiouyAEIOUY])";
        } else if (token.equals("SANS_VOWEL_LOWER_EXTENDED")) {
            return "((\\d)|([^aeiouyA-Z]))";
        } else if (token.equals("SANS_VOWEL_UPPER_EXTENDED")) {
            return "((\\d)|([^a-zAEIOUY]))";
        } else if (token.equals("SANS_VOWEL_MIXED_EXTENDED")) {
            return "((\\d)|([^aeiouyAEIOUY]))";
        } else {
            regex = token;
            System.out.println("theres an error with regex");
        }
        return regex;
    }

    /**
     * Used by Auto Minters to determine the number of permutations are
     * available. Each id will be matched against a format, specified by this
     * method's parameter.
     *
     * @param prefix
     * @param token
     * @param tokenMap
     * @param rootLength
     * @return
     * @throws SQLException
     */
    public long getPermutations(String prefix, String token, String tokenMap, int rootLength)
            throws SQLException {
        int base = tokenMap.length();
        long totalPermutations = (long) Math.pow(base, rootLength);

        findCachedFormat(prefix, token, rootLength, false);

        if (formatIndex != -1) {
            CachedFormat currentFormat = CachedFormatsList.get(formatIndex);
            return totalPermutations - currentFormat.getAmountCreated();
        } else {
            CachedFormat format = new CachedFormat(prefix, token, rootLength);
            CachedFormatsList.add(format);

            // create sql query to retrieve results
            Statement databaseStatement = DATABASE_CONNECTION.createStatement();
            String regex = retrieveRegex(token);

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
            formatIndex = CachedFormatsList.size() - 1;

            // close connections
            databaseResponse.close();
            databaseStatement.close();

            // counts the number of remaining permutations
            System.out.println("numPermutations1 = " + totalPermutations);
            System.out.println("getformat: " + format);
            for (CachedFormat formatk : CachedFormatsList) {
                System.out.println(formatk);
            }
            return totalPermutations - matchingIds;
        }
    }

    /**
     * Used by Custom Minters to determine the number of permutations are
     * available. Each id will be matched against a format, specified by this
     * method's parameter.
     *
     * @param prefix - String to be attached at the front of each id
     * @param sansVowel - Determines whether or not vowels are included
     * @param charMap - The mapping used to
     * @return
     * @throws SQLException
     */
    public long getPermutations(String prefix, boolean sansVowel, String charMap)
            throws SQLException {
        // determine the base
        int totalPermutations = 1;
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
        findCachedFormat(prefix, charMap, 0, sansVowel);

        if (formatIndex != -1) {
            // if the format was found, return the difference between total number of 
            // permutations however many was created
            System.out.println("format found");
            CachedFormat currentFormat = CachedFormatsList.get(formatIndex);
            return totalPermutations - currentFormat.getAmountCreated();
        } else {
            // if the format doesn't exist, create the format
            System.out.println("format not found");
            String regex = regexBuilder(sansVowel, charMap);
            CachedFormat format = new CachedFormat(prefix, charMap, sansVowel);
            CachedFormatsList.add(format);

            // create sql query to retrieve results
            Statement databaseStatement = DATABASE_CONNECTION.createStatement();

            String rowCount = "ROWCOUNT";
            String sqlQuery = String.format("SELECT COUNT(*) AS %5$s FROM %1$s "
                    + "WHERE %4$s REGEXP '^(%2$s)%3$s$';",
                    TABLE_NAME, prefix, regex, COLUMN_NAME, rowCount);
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
            formatIndex = CachedFormatsList.size() - 1;

            // close connections
            databaseResponse.close();
            databaseStatement.close();

            // counts the number of remaining permutations
            System.out.println("Over numPermutations1 = " + totalPermutations);
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
     * @param sansVowel - determines whether or not vowels are accounted for
     * @param charMap - contains characters that refer to a specific tokenMap.
     * @return the regular expression
     */
    private String regexBuilder(boolean sansVowel, String charMap) {
        String regex = "";
        for (int i = 0; i < charMap.length(); i++) {
            char c = charMap.charAt(i);
            String token;
            // get the correct token
            if (sansVowel) {
                token = (c == 'd') ? "DIGIT" : (c == 'l') ? "SANS_VOWEL_LOWER"
                        : (c == 'u') ? "SANS_VOWEL_UPPER" : (c == 'm') ? "SANS_VOWEL_MIXED"
                                        : "SANS_VOWEL_MIXED_EXTENDED";
            } else {
                token = (c == 'd') ? "DIGIT" : (c == 'l') ? "LOWERCASE" : (c == 'u') ? "UPPERCASE"
                        : (c == 'm') ? "MIXEDCASE" : "MIXED_EXTENDED";
            }
            // get the token map and append int to regex
            String retrievedRegex = retrieveRegex(token);
            regex += String.format("[%s]", retrievedRegex);
        }
        return regex;
    }

    /**
     * Using a given prefix and length, this method will count and return the
     * number of matching ids in the database.
     *
     * @param prefix - given prefix
     * @param length - length of each id
     * @return - returns the number of matches
     * @throws java.sql.SQLException
     */
    public long checkPrefix(String prefix, int length) throws SQLException {

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
    }

    /**
     * Prints a list of ids to the server. Strictly used for error checking.
     */
    private void printData() throws SQLException {
        System.out.println("current list of items: ");
        Statement s = DATABASE_CONNECTION.createStatement();
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
     * @throws java.sql.SQLException
     */
    public synchronized void closeConnection() throws SQLException {
        DATABASE_CONNECTION.close();
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

    /**
     * Creates a method to allow the use of regular expressions
     *
     * @throws SQLException
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

    /**
     * Used to store the formats of previously created ids to limit unnecessary,
     * repeated database access when counting matching ids.
     */
    private class CachedFormat {

        // Fields; detailed description in Minter class
        private final String Prefix;
        private final String IdMap;     // used to store both TokenType and CharMap
        private final int RootLength;
        private final boolean SansVowel;
        private long AmountCreated;     // amount of ids created using this particular format

        /**
         * Constructor used to store the format of autoMinters.
         *
         * @param prefix
         * @param idMap
         * @param rootLength
         */
        private CachedFormat(String prefix, String idMap, int rootLength) {
            this.Prefix = prefix;
            this.IdMap = idMap;
            this.RootLength = rootLength;
            this.AmountCreated = 0;
            this.SansVowel = false;
        }

        /**
         * Constructor used to store the format of customMinsters.
         *
         * @param prefix
         * @param idMap
         * @param SansVowel
         */
        private CachedFormat(String prefix, String idMap, boolean SansVowel) {
            this.Prefix = prefix;
            this.IdMap = idMap;
            this.AmountCreated = 0;
            this.SansVowel = SansVowel;
            this.RootLength = 0;
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

        public String getIdMap() {
            return IdMap;
        }

        public int getRootLength() {
            return RootLength;
        }

        public boolean isSansVowel() {
            return SansVowel;
        }

        /**
         * Used for testing; subject to deletion
         *
         * @return
         */
        @Override
        public String toString() {
            return String.format("p=%s, t=%s, l=%d, v=%b,a=%d", Prefix, IdMap, RootLength, SansVowel, AmountCreated);
        }

    }
}
