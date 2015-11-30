package com.hida;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;
import java.util.TreeSet;

/**
 * Minter class creates formatted ids in json using a variety of parameters.
 *
 * @author Brittany Cruz
 * @author lruffin
 */
public class Minter {

    private final DatabaseManager DatabaseManager;
    private final String CharMap;
    private final HashMap<Character, String> BASE_MAP = new HashMap();

    // fields    
    private final String Prepend;
    private final int RootLength;
    private final String Prefix;
    private final String DIGIT_TOKEN = "0123456789";
    private final String SANS_VOWEL_TOKEN = "bcdfghjklmnpqrstvwxz";
    private final String VOWEL_TOKEN = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Constructor for autominters
     *
     * @param DatabaseManager
     * @param Prepend
     * @param RootLength
     * @param Prefix
     */
    public Minter(DatabaseManager DatabaseManager, String Prepend, int RootLength, String Prefix) {
        this.DatabaseManager = DatabaseManager;
        this.Prepend = Prepend;
        this.RootLength = RootLength;
        this.Prefix = Prefix;
        this.CharMap = "";

    }

    /**
     * Constructor for custom minters
     *
     * @param DatabaseManager
     * @param CharMap
     * @param Prepend
     * @param Prefix
     * @param sansVowel
     */
    public Minter(DatabaseManager DatabaseManager, String CharMap, String Prepend, String Prefix,
            boolean sansVowel) {
        this.DatabaseManager = DatabaseManager;
        this.CharMap = CharMap;
        this.Prepend = Prepend;
        this.Prefix = Prefix;

        this.RootLength = CharMap.length();

        if (sansVowel) {
            this.BASE_MAP.put('d', DIGIT_TOKEN);
            this.BASE_MAP.put('l', SANS_VOWEL_TOKEN);
            this.BASE_MAP.put('u', SANS_VOWEL_TOKEN.toUpperCase());
            this.BASE_MAP.put('m', SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase());
            this.BASE_MAP.put('e', DIGIT_TOKEN + SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase());
        } else {
            this.BASE_MAP.put('d', DIGIT_TOKEN);
            this.BASE_MAP.put('l', VOWEL_TOKEN);
            this.BASE_MAP.put('u', VOWEL_TOKEN.toUpperCase());
            this.BASE_MAP.put('m', VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase());
            this.BASE_MAP.put('e', DIGIT_TOKEN + VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase());
        }
    }

    /**
     * 
     * @param token
     * @return 
     */
    public String getMap(String token) {
        String map;
        if (token.equals("DIGIT")) {
            map = DIGIT_TOKEN;
        } else if (token.equals("LOWERCASE")) {
            map = VOWEL_TOKEN;
        } else if (token.equals("UPPERCASE")) {
            map = VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("MIXEDCASE")) {
            map = VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("LOWER_EXTENDED")) {
            map = DIGIT_TOKEN + VOWEL_TOKEN;
        } else if (token.equals("UPPER_EXTENDED")) {
            map = DIGIT_TOKEN + VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("MIXED_EXTENDED")) {
            map = DIGIT_TOKEN + VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("SANS_VOWEL_LOWER")) {
            map = SANS_VOWEL_TOKEN;
        } else if (token.equals("SANS_VOWEL_UPPER")) {
            map = SANS_VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("SANS_VOWEL_MIXED")) {
            map = SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("SANS_VOWEL_LOWER_EXTENDED")) {
            map = DIGIT_TOKEN + SANS_VOWEL_TOKEN;
        } else if (token.equals("SANS_VOWEL_UPPER_EXTENDED")) {
            map = DIGIT_TOKEN + SANS_VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("SANS_VOWEL_MIXED_EXTENDED")) {
            map = DIGIT_TOKEN + SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase();
        } else {
            map = token;
        }
        return map;
    }

    /**
     * Continuously increments ids until a unique id has been found.
     * @param original
     * @param order
     * @param isAuto
     * @param amount
     * @return
     * @throws SQLException 
     */
    private Set<Id> rollIds(Set<Id> original, boolean order, boolean isAuto, int amount)
            throws SQLException {
        Set<Id> duplicateCache = new HashSet(amount);
        duplicateCache.addAll(original);

        // finds a list of ids that is unique to the database            
        while(!DatabaseManager.checkId(original)){
            // create iterator for tempIdList
            Iterator<Id> tempListIter = original.iterator();

            // create a new list to hold tempIds
            Set<Id> newTempList;

            if (order) {
                newTempList = new TreeSet();
            } else {
                newTempList = new LinkedHashSet(amount);
            }

            // iterates through tempIdList and adds unique and potentially unique 
            // values to newTempList
            while (tempListIter.hasNext()) {
                Id currentId;
                if (isAuto) {
                    currentId = new AutoId((AutoId) tempListIter.next());
                } else {
                    currentId = new CustomId((CustomId) tempListIter.next());
                }

                System.out.println(currentId + " is " + currentId.isUnique());
                while (!currentId.isUnique() && !duplicateCache.add(currentId)) {
                    currentId.incrementId();
                    System.out.println("\tnew id = " + currentId);
                }

                if (!newTempList.contains(currentId)) {
                    newTempList.add(currentId);
                }
            }
            original = newTempList;
        }
        return original;
    }

    /**
     * genIDauto() Method description: This generates an automated ID format.
     * Note that this method will take longer and longer to create unique ids
     * because the ids it produces will randomize. Need to figure out a way to
     * quickly discover unminted ids in database.
     *
     * User would give a string that excepts 6 letters: {d, l, u, m, e, a}.
     * Similar to the Mode settings in genIDauto() method. d:
     *
     * <p>
     * DIGIT: {0123456789} Digit values only.</p>
     * <p>
     * LOWERCASE: {abc...xyz} Lowercase letters only. </p>
     * <p>
     * UPPERCASE: {ABC...XYZ} Uppercase letters only.</p>
     * <p>
     * MIXCASE: {abc...XYZ} Lowercase and Uppercase letters only. </p>
     * <p>
     * EXTENDED: {012...xyz} Digit values and Lowercase letters only. </p>
     * <p>
     * ALPHANUMERIC: {012...abc...XYZ} Digit values, Lowercase and Uppercase
     * letters.</p>
     *
     * @param amount
     * @param token: There are 2 different modes to choose from. RANDOM: Value
     * by value, each value's order is randomized. SEQUENTIAL: Value by value,
     * each value's order is sequenced.
     * @return - The method checks to see if it is even possible to produce the
     * requested amount of ids using the given parameters. If it can't it'll
     * return an error message. Otherwise a reference to a JsonObject that
     * contains Json list of ids is returned.
     * @throws java.sql.SQLException     
     */
    public String genIdAutoRandom(int amount, String token) throws SQLException {

        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String tokenMap = getMap(token);
        System.out.println("tokenmap = " + tokenMap);

        Random rng = new Random();
        Set<Id> tempIdList = new LinkedHashSet(amount);

        for (int i = 0; i < amount; i++) {
            int[] tempIdBaseMap = new int[RootLength];
            for (int j = 0; j < RootLength; j++) {
                tempIdBaseMap[j] = rng.nextInt(tokenMap.length());
            }
            Id currentId = new AutoId(Prefix, tempIdBaseMap, tokenMap);
            System.out.println("id created: " + currentId);
            while (!tempIdList.add(currentId)) {
                currentId.incrementId();
            }
        }
        if (!DatabaseManager.checkId(tempIdList)) {
            tempIdList = rollIds(tempIdList, false, true, amount);
        }
        DatabaseManager.addId(tempIdList);

        return convertListToJson(tempIdList);

    }

    /**
     * Sequentially generates ids
     *
     * @param amount
     * @param token
     * @return
     */
    public String genIdAutoSequential(int amount, String token) throws SQLException {
        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String tokenMap = getMap(token);
        System.out.println("tokenmap = " + tokenMap);

        Set<Id> tempIdList = new TreeSet();

        int[] previousIdBaseMap = new int[RootLength];
        AutoId firstId = new AutoId(Prefix, previousIdBaseMap, tokenMap);

        tempIdList.add(firstId);

        for (int i = 0; i < amount - 1; i++) {
            AutoId currentId = new AutoId(firstId);
            currentId.incrementId();
            tempIdList.add(currentId);
            firstId = new AutoId(currentId);
            System.out.println("curr2=" + currentId);
        }

        if (!DatabaseManager.checkId(tempIdList)) {
            tempIdList = rollIds(tempIdList, true, true, amount);
        }
        DatabaseManager.addId(tempIdList);

        return convertListToJson(tempIdList);
    }

    /**
     * genIDcustom() Method description: This generates an customized ID format.
     * User would give a string that excepts 6 letters: {d, l, u, m, e, a}.
     * Similar to the Mode settings in genIDauto() method. d:
     *
     * <p>
     * DIGIT: {0123456789} Digit values only.</p>
     * <p>
     * LOWERCASE: {abc...xyz} Lowercase letters only. </p>
     * <p>
     * UPPERCASE: {ABC...XYZ} Uppercase letters only.</p>
     * <p>
     * MIXCASE: {abc...XYZ} Lowercase and Uppercase letters only. </p>
     * <p>
     * EXTENDED: {012...xyz} Digit values and Lowercase letters only. </p>
     * <p>
     * ALPHANUMERIC: {012...abc...XYZ} Digit values, Lowercase and Uppercase
     * letters.</p>
     *
     * @param amount
     * @param sansVowels - generates ids based on whether or not user requested
     * random strings
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    public String genIdCustomRandom(int amount, boolean sansVowels) throws SQLException {

        String[] tokenMapArray = getBaseCharMapping();

        System.out.println("in genIdCustomRandom");
        for (String s : tokenMapArray) {
            System.out.println(s);
        }
        Random rng = new Random();
        Set<Id> tempIdList = new LinkedHashSet(amount);

        for (int i = 0; i < amount; i++) {
            int[] tempIdBaseMap = new int[RootLength];
            for (int j = 0; j < RootLength; j++) {
                tempIdBaseMap[j] = rng.nextInt(tokenMapArray[j].length());
            }
            Id currentId = new CustomId(Prefix, tempIdBaseMap, tokenMapArray);
            System.out.println("id created: " + currentId);
            while (!tempIdList.add(currentId)) {
                currentId.incrementId();
            }
        }
        if (!DatabaseManager.checkId(tempIdList)) {
            tempIdList = rollIds(tempIdList, false, false, amount);
        }
        DatabaseManager.addId(tempIdList);

        return convertListToJson(tempIdList);
    }

    public String genIdCustomSequential(int amount, boolean sansVowels) throws SQLException {
        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String[] tokenMapArray = getBaseCharMapping();
        System.out.println("in genIdCustomSequential");
        for (String s : tokenMapArray) {
            System.out.println(s);
        }
        Set<Id> tempIdList = new TreeSet();

        int[] previousIdBaseMap = new int[RootLength];
        CustomId firstId = new CustomId(Prefix, previousIdBaseMap, tokenMapArray);

        tempIdList.add(firstId);

        for (int i = 0; i < amount - 1; i++) {
            CustomId currentId = new CustomId(firstId);
            currentId.incrementId();
            tempIdList.add(currentId);
            firstId = new CustomId(currentId);
            System.out.println("curr2=" + currentId);
        }

        if (!DatabaseManager.checkId(tempIdList)) {
            tempIdList = rollIds(tempIdList, true, false, amount);
        }
        DatabaseManager.addId(tempIdList);

        return convertListToJson(tempIdList);
    }

    private String[] getBaseCharMapping() {
        String[] baseTokenMapArray = new String[CharMap.length()];
        for (int i = 0; i < CharMap.length(); i++) {
            char c = CharMap.charAt(i);
            System.out.println("c = " + c);
            baseTokenMapArray[i] = BASE_MAP.get(c);
        }
        return baseTokenMapArray;
    }

    /**
     * Creates a Json object based off the ids in of ID_ARRAY.
     *
     * @return - A reference to a String that contains Json list of ids
     */
    private String convertListToJson(Set<Id> list) {
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;
        try {
            // Object used to iterate through list of ids
            Iterator<Id> iterator = list.iterator();
            for (int i = 0; iterator.hasNext(); i++) {

                // map desired Json format
                String id = String.format("{\"id\":%d,\"name\":\"%s%s\"}",
                        i, Prepend, iterator.next());

                formattedJson = mapper.readValue(id, Object.class);

                // append formatted json
                jsonString += mapper.writerWithDefaultPrettyPrinter().
                        writeValueAsString(formattedJson) + "\n";
            }
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }

        return jsonString;
    }

    public static String errorToJson(String code, String message) {
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;

        try {
            // Object used to iterate through list of ids

            // map desired Json format
            String id = String.format(
                    "{\"status\":%s,\"name\":\"%s\"}", code, message);

            formattedJson = mapper.readValue(id, Object.class);

            // append formatted json
            jsonString += mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(formattedJson) + "\n";

        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }

        return jsonString;
    }


    /* typical getter and setter methods */
    public String getCHAR_MAP() {
        return this.CharMap;
    }

    public String getPrefix() {
        return this.Prefix;
    }

    public int getRootLength() {
        return this.RootLength;
    }

    public DatabaseManager getDatabaseManager() {
        return DatabaseManager;
    }

    public String getPrepend() {
        Id id = new AutoId(null, null, null);
        return Prepend;
    }

    private class AutoId extends Id {

        private String TokenMap;

        public AutoId(AutoId id) {
            super(id);
            this.TokenMap = id.getTokenMap();
        }

        public AutoId(String prefix, int[] baseMap, String tokenMap) {
            super(baseMap, prefix);
            this.TokenMap = tokenMap;
        }

        @Override
        public boolean incrementId() {
            int range = this.getBaseMap().length - 1;

            boolean overflow = true;
            for (int k = 0; k < this.getBaseMap().length && overflow; k++) {
                // record value of current index
                int value = this.getBaseMap()[range - k];

                if (value == TokenMap.length() - 1) {
                    this.getBaseMap()[range - k] = 0;
                } else {
                    this.getBaseMap()[range - k]++;
                    overflow = false;
                }
            }

            return !overflow;
        }

        @Override
        public String convert() {
            String charId = "";
            for (int i = 0; i < this.getBaseMap().length; i++) {
                charId += TokenMap.charAt(this.getBaseMap()[i]);
            }
            return charId;
        }

        @Override
        public String toString() {
            return Prefix + this.convert();
        }

        // getters and setters
        public String getTokenMap() {
            return TokenMap;
        }

        public void setTokenMap(String TokenMap) {
            this.TokenMap = TokenMap;
        }

    }

    private class CustomId extends Id {

        private String[] TokenMapArray;

        public CustomId(CustomId id) {
            super(id);
            this.TokenMapArray = Arrays.copyOf(id.getTokenMapArray(), id.getTokenMapArray().length);
        }

        public CustomId(String prefix, int[] baseMap, String[] tokenMapArray) {
            super(baseMap, prefix);
            this.TokenMapArray = Arrays.copyOf(tokenMapArray, tokenMapArray.length);
        }

        @Override
        public boolean incrementId() {
            int range = this.getBaseMap().length - 1;

            boolean overflow = true;
            for (int k = 0; k < this.getBaseMap().length && overflow; k++) {
                // record value of current index
                int value = this.getBaseMap()[range - k];

                if (value == TokenMapArray[k].length() - 1) {
                    this.getBaseMap()[range - k] = 0;
                } else {
                    this.getBaseMap()[range - k]++;
                    overflow = false;
                }
            }
            return !overflow;
        }

        /**
         * Converts the BaseMap into a String representation of this id's name.
         *
         * There is a one-to-one mapping of BaseMap, dependent on a given
         * TokenMap, to every possible name an Id can have.
         *
         * @return - the name of an Id.
         */
        @Override
        public String convert() {
            String charId = "";

            for (int i = 0; i < this.getBaseMap().length; i++) {
                charId += TokenMapArray[i].charAt(this.getBaseMap()[i]);
            }
            return charId;
        }

        @Override
        public String toString() {
            return Prefix + this.convert();
        }

        // getters and setters
        public String[] getTokenMapArray() {
            return TokenMapArray;
        }

        public void setTokenMapArray(String[] TokenMapArray) {
            this.TokenMapArray = TokenMapArray;
        }
    }
}
