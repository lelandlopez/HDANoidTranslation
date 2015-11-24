package com.hida;

import java.io.IOException;
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

    private final static DatabaseManager DATABASE_MANAGER
            = new DatabaseManager();
    private final String CHAR_MAP;
    private final HashMap<Character, String> BASE_MAP = new HashMap();

    // fields    
    private final String PREPEND;
    private final int AMOUNT;
    private final int LENGTH;
    private final String PREFIX;
    private final Set<String> ID_LIST;
    private final HashMap<String, String> tokenMaps = new HashMap();
    private final String DIGIT_TOKEN = "0123456789";
    private final String SANS_VOWEL_TOKEN = "bcdfghjklmnpqrstvwxzaeiouy";
    private final String VOWEL_TOKEN = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Instantiates an Ark minter that will generate the requested amount of ids
     *
     * @param CHAR_MAP
     * @param PREPEND
     * @param AMOUNT
     * @param LENGTH
     * @param PREFIX
     */
    public Minter(String CHAR_MAP, String PREPEND, int AMOUNT, int LENGTH,
            String PREFIX) {
        this.CHAR_MAP = CHAR_MAP;
        this.PREPEND = PREPEND;
        this.AMOUNT = AMOUNT;
        this.LENGTH = LENGTH - PREFIX.length();
        this.PREFIX = PREFIX;
        this.ID_LIST = new HashSet(AMOUNT);

        // char mappings
        this.tokenMaps.put("DIGIT", "0123456789");
        this.tokenMaps.put("LOWERCASE", "abcdefghijklmnopqrstuvwxyz");
        this.tokenMaps.put("UPPERCASE", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        this.tokenMaps.put("MIXCASE",
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        this.tokenMaps.put("EXTENDED", "0123456789abcdefghijklmnopqrstuvwxyz");
        this.tokenMaps.put("ALPHANUMERIC",
                "0123456789abcdefghijklmnopqrstuvwxyz"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        this.BASE_MAP.put('d', "DIGIT");
        this.BASE_MAP.put('l', "LOWERCASE");
        this.BASE_MAP.put('u', "UPPERCASE");
        this.BASE_MAP.put('m', "MIXCASE");
        this.BASE_MAP.put('e', "EXTENDED");
        this.BASE_MAP.put('a', "ALPHANUMERIC");

    }

    private String getMap(String token) {
        String map;
        if (token.equals("DIGIT")) {
            map = DIGIT_TOKEN;
        } else if (token.equals("LOWERCASE")) {
            map = VOWEL_TOKEN;
        } else if (token.equals("UPPERCASE")) {
            map = VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("MIXCASE")) {
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
            map = SANS_VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("SANS_VOWEL_LOWER_EXTENDED")) {
            map = DIGIT_TOKEN + SANS_VOWEL_TOKEN;
        } else if (token.equals("SANS_VOWEL_UPPER_EXTENDED")) {
            map = DIGIT_TOKEN + VOWEL_TOKEN.toUpperCase();
        } else if (token.equals("SANS_VOWEL_MIXED_EXTENDED")) {
            map = DIGIT_TOKEN + SANS_VOWEL_TOKEN + VOWEL_TOKEN.toUpperCase();
        } else {
            map = token;
        }
        return map;
    }

    private Set<Id> rollIds(Set<Id> original, boolean order, String token) {
        Set<Id> duplicateCache = new HashSet(AMOUNT);
        duplicateCache.addAll(original);

        // finds a list of ids that is unique to the database            
        do {
            int c = 0;
            // create iterator for tempIdList
            Iterator<Id> tempListIter = original.iterator();

            // create a new list to hold tempIds
            Set<Id> newTempList;

            if (order) {
                newTempList = new TreeSet();
            } else {
                newTempList = new LinkedHashSet(AMOUNT);
            }

            // iterates through tempIdList and adds unique and 
            // potentially unique values to newTempList
            while (tempListIter.hasNext()) {

                Id currentId = new Id(tempListIter.next());

                int counter = 0;
                System.out.println(currentId + " is " + currentId.isUnique());
                while (!currentId.isUnique() && !duplicateCache.add(currentId)) {
                    System.out.println("dupes " + counter + " contains " + currentId + " " + currentId.hashCode());
                    Id.incrementId(currentId.getBaseMap(), token);
                    System.out.println("\tnew id = " + currentId);
                    counter++;
                }
                System.out.println("c = " + (c++));

                if(!newTempList.contains(currentId)){
                    newTempList.add(currentId);
                }
                

            }
            original = newTempList;
        } while (!DATABASE_MANAGER.checkId(original));
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
     * @param token: There are 2 different modes to choose from. RANDOM: Value
     * by value, each value's order is randomized. SEQUENTIAL: Value by value,
     * each value's order is sequenced.
     * @return - The method checks to see if it is even possible to produce the
     * requested amount of ids using the given parameters. If it can't it'll
     * return an error message. Otherwise a reference to a JsonObject that
     * contains Json list of ids is returned.
     *
     */
    public String genIdAuto(String token) {

        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String tokenMap = getMap(token);
        System.out.println("tokenmap = " + tokenMap);
        long numberOfPrefix = calculatePermutations(true, tokenMap);
        if (numberOfPrefix >= AMOUNT) {

            Random rng = new Random();
            Set<Id> tempIdList = new LinkedHashSet(AMOUNT);

            for (int i = 0; i < AMOUNT; i++) {
                int[] tempIdBaseMap = new int[LENGTH];
                for (int j = 0; j < LENGTH; j++) {
                    tempIdBaseMap[j] = rng.nextInt(tokenMap.length());
                }
                Id currentId = new Id(PREFIX, tempIdBaseMap, tokenMap);
                System.out.println("id created: " + currentId);
                while (!tempIdList.add(currentId)) {
                    Id.incrementId(currentId.getBaseMap(), tokenMap);
                }
            }
            if (!DATABASE_MANAGER.checkId(tempIdList)) {
                tempIdList = rollIds(tempIdList, false, tokenMap);
            }
            DATABASE_MANAGER.addId(tempIdList);

            return convertListToJson(tempIdList);

        } else {
            // error message stating that ids can no longer be printed
            // using specified prefix and length

            return String.format("{\"status\":400, \"message\":\""
                    + "There are %d remaining ids can be generated given "
                    + "current prefix (%s) and length parameters (%d)\"}",
                    numberOfPrefix, PREFIX, LENGTH);
        }

    }

    /**
     * Sequentially generates ids
     *
     * @param token
     * @return
     */
    public String genIdSequential(String token) {
        // checks to see if its possible to produce or add requested amount of
        // ids to database
        String tokenMap = getMap(token);
        System.out.println("tokenmap = " + tokenMap);
        long numberOfPrefix = calculatePermutations(true, tokenMap);

        if (numberOfPrefix >= AMOUNT) {
            Set<Id> tempIdList = new TreeSet();

            int[] previousIdBaseMap = new int[LENGTH];
            Id firstId = new Id(PREFIX, previousIdBaseMap, tokenMap);

            tempIdList.add(firstId);

            for (int i = 0; i < AMOUNT - 1; i++) {
                int[] currentIdBaseMap = 
                        Arrays.copyOf(previousIdBaseMap, LENGTH);

                Id.incrementId(currentIdBaseMap, tokenMap);
                Id currentId = new Id(PREFIX, currentIdBaseMap, tokenMap);

                tempIdList.add(currentId);
                previousIdBaseMap = 
                        Arrays.copyOf(currentIdBaseMap, LENGTH);
            }

            if (!DATABASE_MANAGER.checkId(tempIdList)) {
                tempIdList = rollIds(tempIdList, true, tokenMap);
            }
            DATABASE_MANAGER.addId(tempIdList);
            
            return convertListToJson(tempIdList);

        } else {
            // error message stating that ids can no longer be printed
            // using specified prefix and length

            
            return String.format("{\"status\":400, \"message\":\""
                    + "There are %d remaining ids can be generated given "
                    + "current prefix (%s) and length parameters (%d)\"}",
                    numberOfPrefix, PREFIX, LENGTH);
        }
    }

    /**
     * A method used to see the amount of ids in the database contain the
     * requested prefix and length.
     *
     * There is a flaw in that this method does not use big integer. As of right
     * now it depends on user not choosing a format that will not produce 2^64
     * unique ids. Otherwise, values will wrap around.
     * 
     * Also needs to be modified to better accommodate for custom minter
     *
     * @param isAuto
     * @param token
     * @return - true if there is space available
     */
    private static long calculatePermutations(
            boolean isAuto, String token, String prefix, 
            int length, String charMap, String baseMap) {

        long matchingIds
                = DATABASE_MANAGER.checkPrefix(prefix, length);
        long numFreeDigits = length;

        if (matchingIds == -1) {
            System.out.println("somethings wrong in isPrefix");
            return -1;
        }

       // if (isAuto) {
            //int base = this.tokenMaps.get(token).length();
            int base = token.length();

            // counts the number of permutations
            long numPermutations = (long) Math.pow(base, numFreeDigits);
            System.out.println("numPermutations = " + numPermutations);
            return numPermutations - matchingIds;
        //} else {
            /*long numPermutations = 1;
            for (int i = 0; i < charMap.length(); i++) {
                /* when it isn't auto, we will not check the database as that
                 will not produce an accurate reseult. What will be returned 
                 instead is the comparison between the maximum number of 
                 permutations and the requested amount.
                 */
/*
                String charToken = BASE_MAP.get(CHAR_MAP.charAt(i));
                int base = tokenMaps.get(charToken).length();
                numPermutations *= base;
            }
*/
            ///return numPermutations;
        

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
     * @param isSequential - generates ids based on whether or not user
     * requested random strings
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    public String genIdCustom(boolean isSequential) {

        if (false) {
            //if (isPrefixAvailable(false, null)) {
            /*
             int[] baseMap = new int[CHAR_MAP.length()];

             // creates a list to hold redundant ids
             Set<String> redundantIdList;

             // sets the number of ids to make
             int numIdsToMake = this.AMOUNT;
             long maxIdsToMake = calculateCharMapPermutations();

             // continue making ids until the size of the list reaches the 
             // requested amount.
             do {
             if (isSequential) { // if the sequential option was chosen
             baseMap = sequentialAlgorithm(numIdsToMake, baseMap);
             } else { // if random option was chosen
             randomAlgorithm(numIdsToMake);
             }
             redundantIdList = DATABASE_MANAGER.checkId(ID_LIST);
             removeRedundantId(redundantIdList);

             // if no redundant ids were found, add new ids to database
             if (ID_LIST.size() == this.AMOUNT) {
             DATABASE_MANAGER.addId(ID_LIST);
             } else if (maxIdsToMake == redundantIdList.size()) {
             return "cannot make any more ids given parameters1";
             } else {
             // decreases the number of ids to make based on what was 
             // just added to ID_LIST
             numIdsToMake = this.AMOUNT - ID_LIST.size();
             }
             } while (ID_LIST.size() < this.AMOUNT);
             return convertListToJson();
             */
        } else {
            return "cannot make any more ids given parameters";
        }
        return "cannot make any more ids given parameters";
    }

    /**
     * Calculates the number of possible permutations in a given char mapping
     *
     * @return - number of permutations
     */
    private long calculateCharMapPermutations() {
        long numPermutations = 1;
        for (int i = 0; i < CHAR_MAP.length(); i++) {
            /* when it isn't auto, we will not check the database as that
             will not produce an accurate reseult. What will be returned 
             instead is the comparison between the maximum number of 
             permutations and the requested amount.
             */

            String charToken = BASE_MAP.get(CHAR_MAP.charAt(i));
            System.out.println("charToken = " + charToken);
            int base = tokenMaps.get(charToken).length();
            System.out.println("base = " + base);
            numPermutations *= base;
            System.out.println("numPermutations = " + numPermutations);
        }
        return numPermutations;
    }

    /**
     * Generates random ids based on char mappings. The ids are stored in
     * ID_LIST
     *
     * @param numIdsToMake - number of ids to make
     */
    private void randomAlgorithm(int numIdsToMake) {
        String tempId, charToken;
        String charRange;
        int randomNum;

        while (ID_LIST.size() < numIdsToMake) {
            tempId = PREFIX;
            for (int j = 0; j < CHAR_MAP.length(); j++) {
                charToken = BASE_MAP.get(CHAR_MAP.charAt(j));
                charRange = tokenMaps.get(charToken);
                randomNum = (int) (Math.random() * charRange.length());

                tempId += charRange.charAt(randomNum);

            }
            ID_LIST.add(tempId);
        }

    }

    /**
     * In the for loop, k represents last index of char mapping. This for loop
     * will incrementally increase the last digit until it reaches its max
     * value, designated by CHAR_MAP and BASE_MAP. If there's overflow, k is
     * incremented so that the next digit can also be incremented. This process
     * continues until there is no overflow. The last sequence is returned to
     * the caller.
     *
     * @param numIdsToMake
     * @param baseMap
     */
    private int[] sequentialAlgorithm(int numIdsToMake, int[] baseMap) {

        StringBuilder buffer = new StringBuilder();

        // define the last position of the map
        int lastPos = CHAR_MAP.length() - 1;

        buffer.append(PREFIX);

        // prepare storing the number of ids that will be created
        int idsCreated = 0;

        for (int k = 0; idsCreated < numIdsToMake;) {
            // increment the last digit - k
            baseMap[lastPos - k]++;

            // store the value of the digit in question
            int value = baseMap[lastPos - k];
            String token = BASE_MAP.get(CHAR_MAP.charAt(lastPos - k));

            // if the max value of a particular digit is reached, overflow
            if (value == tokenMaps.get(token).length()) {
                baseMap[lastPos - k] = 0;
                k++;
            } else {
                String tempId = PREFIX;

                for (int i = 0; i < CHAR_MAP.length(); i++) {
                    char character = CHAR_MAP.charAt(i);
                    String charToken = BASE_MAP.get(character);
                    String map = tokenMaps.get(charToken);

                    tempId += map.charAt(baseMap[i]) + "";
                }
                System.out.println("tempid = " + tempId);
                // add id to list
                ID_LIST.add(tempId);

                // if there wasn't over flow, increase id count
                idsCreated++;

                // reset k so it'll point to last digit again
                k = 0;
            }
        }
        return baseMap;
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
                String id = String.format(
                        "{\"id\":%d,\"name\":\"%s%s\"}",
                        i, PREPEND, iterator.next());

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
    
    public static String errorToJson(String code, String message){
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;
        
        try {
            // Object used to iterate through list of ids
            
                // map desired Json format
                String id = String.format(
                        "{\"status\":%s,\"name\":\"%s\"}",code, message);
                        
                formattedJson = mapper.readValue(id, Object.class);

                // append formatted json
                jsonString += mapper.writerWithDefaultPrettyPrinter().
                        writeValueAsString(formattedJson) + "\n";
            
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
        }

        return jsonString;
    }

    /**
     * Deciding whether or not to implement a check here or in Minter class
     *
     * @param ID - an id to check against a db
     * @return - true if the id exists in the database
     */
    public boolean checkID(String ID) {
        return this.ID_LIST.contains(ID);
    }

    /**
     * error checking
     */
    public void printID_ARRAY() {
        for (String index : this.ID_LIST) {
            System.out.println(index);
        }
    }

    

    /* typical getter and setter methods */
    public String getCHAR_MAP() {
        return this.CHAR_MAP;
    }

    public String getPREFIX() {
        return this.PREFIX;
    }

    public int getLENGTH() {
        return this.LENGTH;
    }

    public Set<String> getID_ARRAY() {
        return ID_LIST;
    }

    public DatabaseManager getDatabaseManager() {
        return DATABASE_MANAGER;
    }

    public String getPREPEND() {
        return PREPEND;
    }

    public int getAMOUNT() {
        return AMOUNT;
    }

}
