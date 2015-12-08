package com.hida;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
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

    /**
     * Used to add and check ids against database.
     */
    private final DatabaseManager DatabaseManager;

    /**
     * The mapping used to describe range of possible characters at each of the
     * id's root's digits. There are total of 5 different ranges that a charMap
     * can contain:
     *
     * <pre>
     * d: digits only
     * l: lower case letters only
     * u: upper case letters only
     * m: letters only
     * e: any valid character specified by d, l, u, and m.
     * </pre>
     *
     *
     */
    private String CharMap;

    /**
     * Designates what characters are contained in the id's root. There are 7 types of
     * token maps, each describing a range of possible characters in the id. This
     * range is further affected by the variable SansVowel.
     * 
     * <pre>
     * DIGIT: Digit values only.
     * LOWERCASE: Lowercase letters only.
     * UPPERCASE: Uppercase letters only.
     * MIXEDCASE: Lowercase and Uppercase letters only.
     * LOWER_EXTENDED: Digit values and Lowercase letters only.
     * UPPER_EXTENDED: Digits and Uppercase letters only
     * MIXED_EXTENDED: All characters specified by previous tokens
     * </pre>
     * 
     */
    private String TokenType;

    /**
     * Contains the mappings for either tokens or the charMaps. The AutoMinter
     * constructor will assign token mappings. The CustomMinter constructor will
     * assign character mappings. The values will depend on whether or sansVowel
     * specified in the constructor.
     */
    private final HashMap<String, String> BaseMap = new HashMap<String, String>();

    /**
     * This value is not added to the database, however this will be displayed.
     * The value is usually used to determine the type of format, if requested,
     * of the id.
     */
    private String Prepend;

    /**
     * Designates the length of the id's root.
     */
    private int RootLength;

    /**
     * The string that will be at the front of every id
     */
    private String Prefix;

    /**
     * A variable that will affect whether or not vowels have the possibility of 
     * being included in each id. 
     */
    private boolean SansVowel;

    /**
     * Contains the range of all the digits
     */
    private final String DIGIT_TOKEN = "0123456789";

    /**
     * Contains the range of the English alphabet without vowels and y.
     */
    private final String SANS_VOWEL_TOKEN = "bcdfghjklmnpqrstvwxz";

    /**
     * Contains the range of the English alphabet with vowels and y.
     */
    private final String VOWEL_TOKEN = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Constructor for AutoMinters
     *
     * @param DatabaseManager Used to add and check ids against database.
     * @param Prepend Designates the format of the id. Will not appear in
     * database.
     * @param TokenMap
     * @param RootLength Designates the length of the id's root
     * @param Prefix The string that will be at the front of every id
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * If the root does not contain vowels, the sansVowel is true; false
     * otherwise.
     */
    public Minter(DatabaseManager DatabaseManager, String Prepend, String TokenMap, int RootLength, String Prefix,
            boolean sansVowel) {
        this.DatabaseManager = DatabaseManager;
        this.Prepend = Prepend;
        this.RootLength = RootLength;
        this.Prefix = Prefix;
        this.SansVowel = sansVowel;
        this.TokenType = TokenMap;

        this.BaseMap.put("DIGIT", DIGIT_TOKEN);
        if (sansVowel) {
            this.BaseMap.put("LOWERCASE", SANS_VOWEL_TOKEN);
            this.BaseMap.put("UPPERCASE", SANS_VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("MIXEDCASE", SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("LOWER_EXTENDED", DIGIT_TOKEN + SANS_VOWEL_TOKEN);
            this.BaseMap.put("UPPER_EXTENDED", DIGIT_TOKEN + SANS_VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("MIXED_EXTENDED",
                    DIGIT_TOKEN + SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase());
        } else {
            this.BaseMap.put("LOWERCASE", VOWEL_TOKEN);
            this.BaseMap.put("UPPERCASE", VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("MIXEDCASE", VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("LOWER_EXTENDED", DIGIT_TOKEN + VOWEL_TOKEN);
            this.BaseMap.put("UPPER_EXTENDED", DIGIT_TOKEN + VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("MIXED_EXTENDED",
                    DIGIT_TOKEN + VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase());
        }
    }

    /**
     * Constructor for CustomMinters
     *
     * @param DatabaseManager Used to add and check ids against database.
     * @param CharMap The mapping used to describe range of possible characters
     * at each of the id's root's digits
     * @param Prepend Designates the format of the id. Will not appear in
     * database.
     * @param Prefix The string that will be at the front of every id
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * If the root does not contain vowels, the sansVowel is true; false
     * otherwise.
     * @throws com.hida.BadParameterException
     */
    public Minter(DatabaseManager DatabaseManager, String Prepend, String CharMap, String Prefix,
            boolean sansVowel) throws BadParameterException {
        this.DatabaseManager = DatabaseManager;
        this.CharMap = CharMap;
        this.Prepend = Prepend;
        this.Prefix = Prefix;
        this.RootLength = CharMap.length();
        this.SansVowel = sansVowel;
        this.TokenType = convertToToken(CharMap);

        if (sansVowel) {
            this.BaseMap.put("d", DIGIT_TOKEN);
            this.BaseMap.put("l", SANS_VOWEL_TOKEN);
            this.BaseMap.put("u", SANS_VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("m", SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("e", DIGIT_TOKEN + SANS_VOWEL_TOKEN + SANS_VOWEL_TOKEN.toUpperCase());
        } else {
            this.BaseMap.put("d", DIGIT_TOKEN);
            this.BaseMap.put("l", VOWEL_TOKEN);
            this.BaseMap.put("u", VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("m", VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase());
            this.BaseMap.put("e", DIGIT_TOKEN + VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase());
        }
    }

    /**
     * Continuously increments a set of ids until the set is completely filled
     * with unique ids.
     *
     * @param original the original set of ds
     * @param order determines whether or not the ids will be ordered
     * @param isAuto determines whether or not the ids are AutoId or CustomId
     * @param amount the amount of ids to be created.
     * @return A set of unique ids
     * @throws SQLException - thrown whenever there is an error with the
     * database.
     */
    private Set<Id> rollIds(Set<Id> original, boolean order,long totalPermutations, long amount) 
            throws SQLException, NotEnoughPermutationsException {
        System.out.println("inside roll ids");

        // Used to count the number of unique ids. Size methods aren't used because int is returned
        long uniqueIdCounter = 0;

        // Declares and initializes a list that holds unique values. 
        // If order matters, tree set is used.
        Set<Id> uniqueList;
        if (order) {
            uniqueList = new TreeSet();
        } else {
            uniqueList = new LinkedHashSet(original.size());
        }

        // iterate through every id 
        for (Id currentId : original) {
            // counts the number of times an id has been rejected
            long counter = 0;

            // continuously increments invalid or non-unique ids
            while (!DatabaseManager.isValidId(currentId) || uniqueList.contains(currentId)) {

                /* if counter exceeds totalPermutations, then id has iterated through every 
                 possible permutation. Related format is updated as a quick-look-up reference
                 with how many ids have inadvertedly been created using other formats.
                 NotEnoughPermutationsException is thrown stating remaining number of ids.
                 */
                if (counter > totalPermutations) {
//                    System.out.println("\t\tuniqueCounter = " + uniqueIdCounter);
                    //                  System.out.println("\t\tcounter = " + counter);
                    //                System.out.println("\t\ttotalPermutations = " + totalPermutations);
                    long amountTaken = totalPermutations - uniqueIdCounter;

                    System.out.println("throwing exception");
                    DatabaseManager.setAmountCreated(
                            Prefix, TokenType, SansVowel, RootLength, amountTaken);
                    throw new NotEnoughPermutationsException(uniqueIdCounter, amount);
                }
                currentId.incrementId();
                counter++;
            }
            // unique ids are added to list and uniqueIdCounter is incremented.
            // Size methods aren't used because int is returned
            uniqueIdCounter++;
            uniqueList.add(currentId);
        }
        System.out.println("\tuniqueCounter = " + uniqueIdCounter);
        System.out.println("uniqueList size = " + uniqueList.size());

        return uniqueList;

    }

    /**
     * Generates random ids automatically based on root length. The contents of
     * the ids are determined by the tokenType.     
     *
     * @param amount the amount of ids to create
     * @return a JSON list of unique ids.
     * @throws SQLException - thrown whenever there is an error with the
     * database
     * @throws com.hida.BadParameterException
     * @throws java.io.IOException
     */
    public String genIdAutoRandom(long amount) throws SQLException, IOException, 
            BadParameterException, NotEnoughPermutationsException  {
        System.out.println("in genIdAutoRandom: " + amount);

        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String tokenMap = BaseMap.get(TokenType);

        Random rng = new Random();
        Set<Id> tempIdList = new LinkedHashSet((int) amount);

        for (int i = 0; i < amount; i++) {
            int[] tempIdBaseMap = new int[RootLength];
            for (int j = 0; j < RootLength; j++) {
                tempIdBaseMap[j] = rng.nextInt(tokenMap.length());
            }
            Id currentId = new AutoId(Prefix, tempIdBaseMap, tokenMap);
            //System.out.println("id created: " + currentId);
            while (!tempIdList.add(currentId)) {
                currentId.incrementId();
            }
        }
        long totalPermutations
                = DatabaseManager.getTotalPermutations(TokenType, RootLength, SansVowel);
        // Ensure that the ids are unique against the database
        tempIdList = rollIds(tempIdList, false, totalPermutations, amount);

        DatabaseManager.addIdList(tempIdList, amount, Prefix, TokenType, SansVowel, RootLength);

        //System.out.println("returning json list");
        return convertListToJson(tempIdList);

    }

    /**
     * Sequentially generates ids automatically based on root length. The
     * contents of the ids are determined by the tokenType.
     *
     * @param amount amount of ids to create
     * @return a JSON list of unique ids.
     * @throws SQLException thrown whenever there is an error with the database
     * @throws java.io.IOException
     */
    public String genIdAutoSequential(long amount)
            throws SQLException, IOException, BadParameterException, NotEnoughPermutationsException  {
        System.out.println("in genIdAutoSequential: " + amount);

        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String tokenMap = BaseMap.get(TokenType);

        Set<Id> tempIdList = new TreeSet();

        int[] previousIdBaseMap = new int[RootLength];
        AutoId firstId = new AutoId(Prefix, previousIdBaseMap, tokenMap);

        tempIdList.add(firstId);

        for (int i = 0; i < amount - 1; i++) {
            AutoId currentId = new AutoId(firstId);
            currentId.incrementId();
            tempIdList.add(currentId);
            firstId = new AutoId(currentId);
        }
        long totalPermutations
                = DatabaseManager.getTotalPermutations(TokenType, RootLength, SansVowel);
        // Ensure that the ids are unique against the database
        tempIdList = rollIds(tempIdList, true, totalPermutations, amount);

        DatabaseManager.addIdList(tempIdList, amount, Prefix, TokenType, SansVowel, RootLength);

        return convertListToJson(tempIdList);
    }

    /**
     * Creates random ids based on charMaping.
     *
     * @param amount amount of ids to create
     * @return a JSON list of unique ids.
     * @throws SQLException thrown whenever there is an error with the database
     * @throws java.io.IOException
     */
    public String genIdCustomRandom(long amount)
            throws SQLException, IOException, BadParameterException, NotEnoughPermutationsException  {

        System.out.println("in genIdCustomRandom: " + amount);
        String[] tokenMapArray = getBaseCharMapping();

        Random rng = new Random();
        Set<Id> tempIdList = new LinkedHashSet((int) amount);

        for (int i = 0; i < amount; i++) {
            int[] tempIdBaseMap = new int[RootLength];
            for (int j = 0; j < RootLength; j++) {
                tempIdBaseMap[j] = rng.nextInt(tokenMapArray[j].length());
            }
            Id currentId = new CustomId(Prefix, tempIdBaseMap, tokenMapArray);
            while (!tempIdList.add(currentId)) {
                currentId.incrementId();
            }
        }
        long totalPermutations = DatabaseManager.getTotalPermutations(CharMap, SansVowel);

        // Ensure that the ids are unique against the database
        tempIdList = rollIds(tempIdList, false, totalPermutations, amount);

        DatabaseManager.addIdList(tempIdList, amount, Prefix, TokenType, SansVowel, RootLength);
        return convertListToJson(tempIdList);
    }

    /**
     * Sequentially generates ids based on char mapping.    
     *
     * @param amount amount of ids to create
     * @return a JSON list of unique ids.
     * @throws SQLException thrown whenever there is an error with the database
     * @throws java.io.IOException
     */
    public String genIdCustomSequential(int amount)
            throws SQLException, IOException, BadParameterException, NotEnoughPermutationsException  {
        System.out.println("in genIdCustomSequential: " + amount);
        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String[] tokenMapArray = getBaseCharMapping();

        Set<Id> tempIdList = new TreeSet();

        int[] previousIdBaseMap = new int[RootLength];
        CustomId firstId = new CustomId(Prefix, previousIdBaseMap, tokenMapArray);

        tempIdList.add(firstId);

        for (int i = 0; i < amount - 1; i++) {
            CustomId currentId = new CustomId(firstId);
            currentId.incrementId();
            tempIdList.add(currentId);
            firstId = new CustomId(currentId);
        }
        long totalPermutations = DatabaseManager.getTotalPermutations(CharMap, SansVowel);

        // Ensure that the ids are unique against the database. Add when confirmed
        tempIdList = rollIds(tempIdList, true, totalPermutations, amount);
        DatabaseManager.addIdList(tempIdList, amount, Prefix, TokenType, SansVowel, RootLength);

        return convertListToJson(tempIdList);
    }

    /**
     * This method returns an equivalent token for any given charMap
     *
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits
     * @return the token equivalent to the charMap
     */
    protected String convertToToken(String charMap) throws BadParameterException {

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
     * 
     * @return 
     */
    private String[] getBaseCharMapping() {
        String[] baseTokenMapArray = new String[CharMap.length()];
        for (int i = 0; i < CharMap.length(); i++) {
            // more efficient than charMap.charAt(i) + ""
            String c = String.valueOf(CharMap.charAt(i));
            baseTokenMapArray[i] = BaseMap.get(c);
        }
        return baseTokenMapArray;
    }

    /**
     * Creates a Json object based off a list of ids given in the parameter
     *
     * @return A reference to a String that contains Json list of ids
     */
    private String convertListToJson(Set<Id> list) throws IOException {
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;

        // Object used to iterate through list of ids
        Iterator<Id> iterator = list.iterator();
        for (int i = 0; iterator.hasNext(); i++) {

            // Creates desired JSON format. Also adds prepend to the string to be displayed
            // to the client
            String id = String.format("{\"id\":%d,\"name\":\"%s%s\"}",
                    i, Prepend, iterator.next());

            formattedJson = mapper.readValue(id, Object.class);

            // append formatted json
            jsonString += mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(formattedJson) + "\n";
        }

        return jsonString;
    }

    /* typical getter and setter methods */
    public String getCharMap() {
        return CharMap;
    }

    public void setCharMap(String CharMap) {
        this.CharMap = CharMap;
    }

    public String getTokenType() {
        return TokenType;
    }

    public void setTokenType(String TokenType) {
        this.TokenType = TokenType;
    }

    public String getPrepend() {
        return Prepend;
    }

    public void setPrepend(String Prepend) {
        this.Prepend = Prepend;
    }

    public int getRootLength() {
        return RootLength;
    }

    public void setRootLength(int RootLength) {
        this.RootLength = RootLength;
    }

    public String getPrefix() {
        return Prefix;
    }

    public void setPrefix(String Prefix) {
        this.Prefix = Prefix;
    }

    public boolean isSansVowel() {
        return SansVowel;
    }

    public void setSansVowel(boolean SansVowel) {
        this.SansVowel = SansVowel;
    }

    public DatabaseManager getDatabaseManager() {
        return DatabaseManager;
    }

    /**
     * Created and used by AutoMinter
     */
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
        public String getRootName() {
            String charId = "";
            for (int i = 0; i < this.getBaseMap().length; i++) {
                charId += TokenMap.charAt(this.getBaseMap()[i]);
            }
            return charId;
        }

        @Override
        public String toString() {
            return Prefix + this.getRootName();
        }

        // getters and setters
        public String getTokenMap() {
            return TokenMap;
        }

        public void setTokenMap(String TokenMap) {
            this.TokenMap = TokenMap;
        }

    }

    /**
     * Created and used by CustomMinters
     */
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
 DatabaseManager, to every possible name an Id can have.
         *
         * @return - the name of an Id.
         */
        @Override
        public String getRootName() {
            String charId = "";

            for (int i = 0; i < this.getBaseMap().length; i++) {
                charId += TokenMapArray[i].charAt(this.getBaseMap()[i]);
            }
            return charId;
        }

        @Override
        public String toString() {
            return Prefix + this.getRootName();
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
