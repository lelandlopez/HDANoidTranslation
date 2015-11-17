package com.hida;

import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * A class that creates ids using the ark formatting convention. Currently
 * deciding whether or not to merge this with Minter
 *
 * @author Brittany Cruz
 * @author lruffin
 */
public class Minter {

    private static DatabaseManager DATABASE_MANAGER = new DatabaseManager();
    private String charMap; //= "12345abc";
    private String idPrefix;// = "xyz1";
    private int idLength;// = 10;

    // fields    
    private final String NAAN = "/:ark/70111/";    
    private String PREPEND;
    private final int AMOUNT;
    private final int LENGTH;
    private final String PREFIX;
    private final String FORMAT;
    private ArrayList<String> ID_ARRAY = new ArrayList();
    private final HashMap<String, String> tokenMaps = new HashMap();

    /**
     * Instantiates an Ark minter that will generate the requested amount of ids
     *
     * @param amount - amount of ids to mint
     */
    public Minter(int amount) {
        retrieveSettings();
        this.AMOUNT = amount;
        this.PREFIX = getIdPrefix();
        this.FORMAT = getCharMap();
        this.LENGTH = (10 - this.PREFIX.length());

        /*
         deciding whether or not to store these values in a database or just 
         constantly generate them. If the latter, should I store them 
         somewhere else, such as in a separate class or in the minter        
         */
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
     * Proposed use to retrieve any stored data from database. Deciding how
     * it'll interact with java beans. Need to read up more on it
     */
    public void retrieveSettings() {
        this.charMap = "12345abc";
        this.idPrefix = "xyz1";
        this.idLength = 10;
    }

    /* constructors; currently unsed - subject to removal */
    /*
     public ArkMinter(int amount, int length) {
     this.PREFIX = "";
     this.AMOUNT = amount;
     this.LENGTH = length;
     this.FORMAT = "auto";
     }

     public ArkMinter(int amount, String prefix, int length) {
     this.PREFIX = prefix;
     this.AMOUNT = amount;
     this.LENGTH = length;
     this.FORMAT = "auto";
     }

     public ArkMinter(int amount, String format) {
     this.PREFIX = "";
     this.AMOUNT = amount;
     this.LENGTH = format.length();
     this.FORMAT = format;
     }

     public ArkMinter(int amount, String prefix, String format) {
     this.PREFIX = prefix;
     this.AMOUNT = amount;
     this.LENGTH = (prefix.length() + format.length());
     this.FORMAT = format;
     }
     */
    /**
     * * genIDauto() Method description: This generates an automated ID format.
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
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    public String genIdAuto(String token) {
        String characters = (String) this.tokenMaps.get(token);

        int numIdsToMake = this.AMOUNT;
        do {
            int charactersLength = characters.length();
            for (int j = 0; j < numIdsToMake; j++) {
                StringBuilder buffer = new StringBuilder();
                for (int i = 0; i < this.LENGTH; i++) {
                    double index = Math.random() * charactersLength;
                    buffer.append(characters.charAt((int) index));
                }
                String temp = buffer.toString();
                this.ID_ARRAY.add(this.PREFIX + temp);
            }
            ID_ARRAY = DATABASE_MANAGER.addId(ID_ARRAY);

            numIdsToMake -= ID_ARRAY.size();
        } while (numIdsToMake > 0);
        return convertListToJson();
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
        String format = this.FORMAT.toLowerCase();
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
            this.ID_ARRAY.add(NAAN + this.PREFIX + temp);
        }
        return convertListToJson();
    }

    /**
     * Creates a Json object based off the ids in of ID_ARRAY
     *
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    private String convertListToJson() {
        /*
         JsonBuilderFactory factory = Json.createBuilderFactory(null);
         JsonObjectBuilder list = factory.createObjectBuilder();
         JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
         for (int i = 0; i < this.AMOUNT; i++) {
         arrayBuilder.add(Json.createObjectBuilder().add("id", i).
         add("name", (String) this.ID_ARRAY.get(i)));
         }
         */
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;
        try {
            for (int i = 0; i < ID_ARRAY.size(); i++) {
                // map desired Json format
                String id = String.format(
                        "{\"id\":%d,\"name\":\"%s\"}", i, ID_ARRAY.get(i));

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
        return this.ID_ARRAY.contains(ID);
    }

    /**
     * error checking
     */
    public void printID_ARRAY() {
        for (String index : this.ID_ARRAY) {
            System.out.println(index);
        }
    }

    /* typical getter and setter methods */
    public String getCharMap() {
        return this.charMap;
    }

    public void setCharMap(String charMap) {
        this.charMap = charMap;
    }

    public String getIdPrefix() {
        return this.idPrefix;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public int getIdLength() {
        return this.idLength;
    }

    public void setIdLength(int idLength) {
        this.idLength = idLength;
    }

    public ArrayList<String> getID_ARRAY() {
        return ID_ARRAY;
    }

    public void setID_ARRAY(ArrayList<String> ID_ARRAY) {
        this.ID_ARRAY = ID_ARRAY;
    }
    
    
    public DatabaseManager getDatabaseManager(){
        return DATABASE_MANAGER;
    }

    public String getPREPEND() {
        return PREPEND;
    }

    public void setPREPEND(String PREPEND) {
        this.PREPEND = PREPEND;
    }
    
    
    

}
