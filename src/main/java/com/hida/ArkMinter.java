package com.hida;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * A class that creates ids using the ark formatting convention. Currently
 * deciding whether or not to merge this with Minter
 * @author Brittany Cruz
 */
public class ArkMinter extends Minter {

    // fields
    private final String NAAN = "/:ark/70111/";
    private final int AMOUNT;
    private final int LENGTH;
    private final String PREFIX;
    private final String FORMAT;
    private final LinkedList<String> ID_ARRAY = new LinkedList();
    private final HashMap<String, String> tokenMaps = new HashMap();

    /**
     * Instantiates an Ark minter that will generate the requested amount of
     * ids
     * @param amount - amount of ids to mint
     */
    public ArkMinter(int amount) {
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

    /* constructors; currently unsed - subject to removal */
    
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
    @Override
    public JsonObject genIdAuto(String token) {
        String characters = (String) this.tokenMaps.get(token);

        int charactersLength = characters.length();
        for (int j = 0; j < this.AMOUNT; j++) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < this.LENGTH; i++) {
                double index = Math.random() * charactersLength;
                buffer.append(characters.charAt((int) index));
            }
            String temp = buffer.toString();
            this.ID_ARRAY.add(NAAN + this.PREFIX + temp);
        }
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
    @Override
    public JsonObject genIdCustom(boolean isRandom) {
        String format = this.FORMAT.toLowerCase();

        double index = 0.0D;
        for (int j = 0; j < this.AMOUNT; j++) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < format.length(); i++) {
                char c = format.charAt(i);
                if (isRandom) {
                    switch (c) {
                        case 'd':
                            index = Math.random() * 10.0D;
                            buffer.append(((String) this.tokenMaps.get("DIGIT")).charAt((int) index));
                            break;
                        case 'l':
                            index = Math.random() * 26.0D;
                            buffer.append(((String) this.tokenMaps.get("LOWERCASE")).charAt((int) index));
                            break;
                        case 'u':
                            index = Math.random() * 26.0D;
                            buffer.append(((String) this.tokenMaps.get("UPPERCASE")).charAt((int) index));
                            break;
                        case 'm':
                            index = Math.random() * 52.0D;
                            buffer.append(((String) this.tokenMaps.get("MIXCASE")).charAt((int) index));
                            break;
                        case 'e':
                            index = Math.random() * 36.0D;
                            buffer.append(((String) this.tokenMaps.get("EXTENDED")).charAt((int) index));
                            break;
                        case 'a':
                            index = Math.random() * 62.0D;
                            buffer.append(((String) this.tokenMaps.get("ALPHANUMERIC")).charAt((int) index));
                    }
                } else {
                    switch (c) {
                        case 'd':
                            buffer.append(((String) this.tokenMaps.get("DIGIT")).charAt((int) index));
                            index += 1.0D;
                            break;
                        case 'l':
                            buffer.append(((String) this.tokenMaps.get("LOWERCASE")).charAt((int) index));
                            index += 1.0D;
                            break;
                        case 'u':
                            buffer.append(((String) this.tokenMaps.get("UPPERCASE")).charAt((int) index));
                            index += 1.0D;
                            break;
                        case 'm':
                            buffer.append(((String) this.tokenMaps.get("MIXCASE")).charAt((int) index));
                            index += 1.0D;
                            break;
                        case 'e':
                            buffer.append(((String) this.tokenMaps.get("EXTENDED")).charAt((int) index));
                            index += 1.0D;
                            break;
                        case 'a':
                            buffer.append(((String) this.tokenMaps.get("ALPHANUMERIC")).charAt((int) index));
                            index += 1.0D;
                    }
                }
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
    private JsonObject convertListToJson() {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder list = factory.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (int i = 0; i < this.AMOUNT; i++) {
            arrayBuilder.add(Json.createObjectBuilder().add("id", i).add("name", (String) this.ID_ARRAY.get(i)));
        }
        return list.add("ark", arrayBuilder).build();
    }

    /**
     * Deciding whether or not to implement a check here or in Minter class
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
}
