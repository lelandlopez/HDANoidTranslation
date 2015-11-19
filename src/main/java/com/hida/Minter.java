package com.hida;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;

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
    private final String NAAN = "/:ark/70111/";
    private final String PREPEND;
    private final int AMOUNT;
    private final int LENGTH;
    private final String PREFIX;
    private final Set<String> ID_LIST;
    private final HashMap<String, String> tokenMaps = new HashMap();

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
        this.LENGTH = LENGTH;
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
        if (isPrefixAvailable(true, token)) {
            // creates a list to hold redundant ids
            Set<String> redundantIdList = new HashSet();

            // a range of characters that can be used, retrieved by tokenMap
            String characters = (String) this.tokenMaps.get(token);

            // sets the number of ids to make
            int numIdsToMake = this.AMOUNT;
            int prefixSize = this.PREFIX.length();
            int charactersLength = characters.length();

            do {

                // creates tentative ids that'll be checked for uniqueness
                while (ID_LIST.size() < AMOUNT) {
                    StringBuilder buffer = new StringBuilder();
                    for (int i = 0; i < this.LENGTH - prefixSize; i++) {
                        double index = Math.random() * charactersLength;
                        buffer.append(characters.charAt((int) index));
                    }
                    String temp = buffer.toString();
                    this.ID_LIST.add(this.PREFIX + temp);
                }
                redundantIdList.addAll(DATABASE_MANAGER.checkId(ID_LIST));

                // removes any redundant ids
                removeRedundantId(redundantIdList);

                // if no redundant ids were found, add new ids to database
                if (ID_LIST.size() == this.AMOUNT) {
                    DATABASE_MANAGER.addId(ID_LIST);
                } 
                    // decreases the number of ids to make based on what was 
                    // just added to ID_LIST
                    numIdsToMake = this.AMOUNT - ID_LIST.size();
                
                System.out.println("numIdsToMake = " + numIdsToMake);
                System.out.println("size = " + ID_LIST.size());
                System.out.println("amount = " + this.AMOUNT);
            } while (ID_LIST.size() < this.AMOUNT);
            return convertListToJson();

        } else {
            // error message stating that ids can no longer be printed
            // using specified prefix and length
            return "cannot make any more ids given parameters";
        }

    }

    /**
     * A method used to see the amount of ids in the database contain the
     * requested prefix and length.
     *
     * There is a flaw in that this method does not use big integer. As of right
     * now it depends on user not choosing a format that will not produce 2^64
     * unique ids.
     *
     * @param isAuto
     * @param token
     * @return - true if there is space available
     */
    private boolean isPrefixAvailable(boolean isAuto, String token) {

        int matchingIds = DATABASE_MANAGER.checkPrefix(PREFIX, LENGTH);
        int numFreeDigits = this.LENGTH - this.PREFIX.length();

        if (matchingIds == -1) {
            System.out.println("somethings wrong in isPrefix");
            return false;
        }

        if (isAuto) {
            int base = this.tokenMaps.get(token).length();

            // counts the number of permutations
            long numPermutations = (long) Math.pow(base, numFreeDigits);
            return numPermutations - matchingIds > this.AMOUNT;
        } else {
            long numPermutations = 1;
            for (int i = 0; i < CHAR_MAP.length(); i++) {
                /* when it isn't auto, we will not check the database as that
                 will not produce an accurate reseult. What will be returned 
                 instead is the comparison between the maximum number of 
                 permutations and the requested amount.
                 */

                String charToken = BASE_MAP.get(CHAR_MAP.charAt(i));
                int base = tokenMaps.get(charToken).length();
                numPermutations *= base;
            }

            return numPermutations >= AMOUNT;
        }

    }

    /**
     * Removes redundant ids from ID_LIST based on given redundantList.
     *
     * @param redundantList - list of redundancies
     */
    private void removeRedundantId(Set<String> redundantList) {
        for (String redundantId : redundantList) {
            ID_LIST.remove(redundantId);
            System.out.println("removed: " + redundantId);
        }
        System.out.println("printing current list of ids");
        for (String id : ID_LIST) {

            System.out.println(id);
        }
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
     * @param isSequential - generates ids based on whether or not user requested
     * random strings
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    public String genIdCustom(boolean isSequential) {
        if (isPrefixAvailable(false, null)) {

            int[] baseMap = new int[CHAR_MAP.length()];
            
            // creates a list to hold redundant ids
            Set<String> redundantIdList;

            // sets the number of ids to make
            int numIdsToMake = this.AMOUNT;
            long maxIdsToMake = calculateCharMapPermutations();
            
            // continue making ids until the size of the list reaches the 
            // requested amount.
            do{
                if(isSequential){ // if the sequential option was chosen
                    baseMap = sequentialAlgorithm(numIdsToMake, baseMap);
                }else{ // if random option was chosen
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
        } else {
            return "cannot make any more ids given parameters";
        }

    }

    /**
     * Calculates the number of possible permutations in a given char mapping
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
     * @param numIdsToMake - number of ids to make     
     */
    private void randomAlgorithm(int numIdsToMake){          
        String tempId, charToken;
        String charRange;
        int randomNum;
                
        while (ID_LIST.size() < numIdsToMake) {
            tempId = PREFIX; 
            for (int j = 0; j < CHAR_MAP.length(); j++) {
                charToken = BASE_MAP.get(CHAR_MAP.charAt(j));
                charRange = tokenMaps.get(charToken);
                randomNum = (int)(Math.random() * charRange.length());
                
                tempId += charRange.charAt(randomNum);
                                                
            }
            ID_LIST.add(tempId);
        }
                
    }

    /**     
     * In the for loop, k represents last index of char mapping. This for 
     * loop will incrementally increase the last digit until it reaches its
     * max value, designated by CHAR_MAP and BASE_MAP. If there's 
     * overflow, k is incremented so that the next digit can also be
     * incremented. This process continues until there is no overflow. The
     * last sequence is returned to the caller. 
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
    private String convertListToJson() {
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;
        try {
            // Object used to iterate through list of ids
            Iterator<String> iterator = ID_LIST.iterator();
            for (int i = 0; iterator.hasNext(); i++) {

                // map desired Json format
                String id = String.format(
                        "{\"id\":%d,\"name\":\"%s\"}", i, iterator.next());

                formattedJson
                        = mapper.readValue(id, Object.class
                        );

                // append formatted json
                jsonString += mapper.writerWithDefaultPrettyPrinter().
                        writeValueAsString(formattedJson) + "\n";
            }
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
