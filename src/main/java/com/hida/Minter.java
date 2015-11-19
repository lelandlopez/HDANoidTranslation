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
    }

    /**
     * genIDauto() Method description: This generates an automated ID format.
     * Note that this method will take longer and longer to create unique ids
     * because the ids it produces will randomize. Need to figure out a way
     * to quickly discover unminted ids in database. 
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
     * @return - The method checks to see if it is even possible to produce
     * the requested amount of ids using the given parameters. If it can't 
     * it'll return an error message. Otherwise a reference to a JsonObject 
     * that contains Json list of ids is returned. 
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
                while(ID_LIST.size() < numIdsToMake) {
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
                if(ID_LIST.size() == this.AMOUNT){
                    DATABASE_MANAGER.addId(ID_LIST);
                }else{     
                    // decreases the number of ids to make based on what was 
                    // just added to ID_LIST
                    numIdsToMake = this.AMOUNT - ID_LIST.size();
                }
                

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
     * There is a flaw in that this method does not use big integer. As of
     * right now it depends on user not choosing a format that will not produce
     * 2^64 unique ids. 
     *
     * @param isAuto
     * @param token
     * @return - true if there is space available
     */
    private boolean isPrefixAvailable(boolean isAuto, String token) {
        
        int matchingIds = DATABASE_MANAGER.checkPrefix(PREFIX, LENGTH);
        int numFreeDigits = this.LENGTH - this.PREFIX.length();
        int base = this.tokenMaps.get(token).length();
        
        if(matchingIds == -1){
            System.out.println("somethings wrong in isPrefix");
            return false;
        }

        if (isAuto) {
            // counts the number of permutations
            long numPermutations = (long) Math.pow(base, numFreeDigits);
            return numPermutations - matchingIds > this.AMOUNT;
        } else {
            return false;
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
     * @param isRandom - generates ids based on whether or not user requested
     * random strings
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    public String genIdCustom(boolean isRandom) {
        String format = this.CHAR_MAP.toLowerCase();
        String tokenType = "";
        double index = 0.0D;
        for (int j = 0; j < this.AMOUNT; j++) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < format.length(); i++) {
                char c = format.charAt(i);
                //if (isRandom) {
                switch (c) {
                    case 'd':
                        index = (isRandom) ? Math.random() * 10.0D : index + 1.0D;
                        tokenType = "DIGIT";
                        //buffer.append(((String) this.tokenMaps.get("DIGIT")).charAt((int) index));
                        break;
                    case 'l':
                    case 'u':
                        index = (isRandom) ? Math.random() * 26.0D : index + 1.0D;
                        //index = Math.random() * 26.0D;
                        //buffer.append(((String) this.tokenMaps.get("LOWERCASE")).charAt((int) index));
                        tokenType = "LOWERCASE";
                        break;
                    /*
                     case 'u':
                     index = (isRandom)? Math.random() * 10.0D : index + 1.0D;
                     index = Math.random() * 26.0D;
                     //buffer.append(((String) this.tokenMaps.get("UPPERCASE")).charAt((int) index));
                     tokenType = "UPPERCASE";
                     break;
                     */
                    case 'm':
                        index = (isRandom) ? Math.random() * 52.0D : index + 1.0D;
                        //index = Math.random() * 52.0D;
                        //buffer.append(((String) this.tokenMaps.get("MIXCASE")).charAt((int) index));
                        tokenType = "MIXCASE";
                        break;
                    case 'e':
                        index = (isRandom) ? Math.random() * 36.0D : index + 1.0D;
                        //index = Math.random() * 36.0D;
                        //buffer.append(((String) this.tokenMaps.get("EXTENDED")).charAt((int) index));
                        tokenType = "EXTENDED";
                        break;
                    case 'a':
                        index = (isRandom) ? Math.random() * 62.0D : index + 1.0D;
                        //index = Math.random() * 62.0D;
                        //buffer.append(((String) this.tokenMaps.get("ALPHANUMERIC")).charAt((int) index));
                        tokenType = "ALPHANUMERIC";
                        break;
                }
                buffer.append(
                        (this.tokenMaps.get(tokenType)).charAt((int) index));

                /*} else {
                 switch (c) {
                 case 'd':
                 //buffer.append(((String) this.tokenMaps.get("DIGIT")).charAt((int) index));
                 tokenType = "DIGIT";
                 index += 1.0D;
                 break;
                 case 'l':
                 //buffer.append(((String) this.tokenMaps.get("LOWERCASE")).charAt((int) index));
                 tokenType = "LOWERCASE";
                 index += 1.0D;
                 break;
                 case 'u':
                 //buffer.append(((String) this.tokenMaps.get("UPPERCASE")).charAt((int) index));
                 tokenType = "UPPERCASE";
                 index += 1.0D;
                 break;
                 case 'm':
                 //buffer.append(((String) this.tokenMaps.get("MIXCASE")).charAt((int) index));
                 tokenType = "MIXCASE";
                 index += 1.0D;
                 break;
                 case 'e':
                 //buffer.append(((String) this.tokenMaps.get("EXTENDED")).charAt((int) index));
                 tokenType = "EXTENDED";
                 index += 1.0D;
                 break;
                 case 'a':
                 //buffer.append(((String) this.tokenMaps.get("ALPHANUMERIC")).charAt((int) index));
                 tokenType = "ALPHANUMERIC";
                 index += 1.0D;
                 break;
                 }
                 }
                 */
                buffer.append(
                        (this.tokenMaps.get(tokenType)).charAt((int) index));
            }
            String temp = buffer.toString();
            this.ID_LIST.add(this.PREFIX + temp);
        }
        return convertListToJson();
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
            for (int i = 0;iterator.hasNext(); i++) {
                
                // map desired Json format
                String id = String.format(
                        "{\"id\":%d,\"name\":\"%s\"}", i, iterator.next());

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
