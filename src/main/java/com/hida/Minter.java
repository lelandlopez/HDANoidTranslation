package com.hida;

import java.util.HashMap;
import javax.json.JsonObject;

/**
 * A template for all minters should inherit from.
 *
 * As of right now tentative functionality includes...
 * <p>
 * - storing and retrieving user preferences (maybe with java beans?)
 * </p>
 * <p>
 * - connecting to database via connectDb method
 * </p>
 * <p>
 * - a check id method that will check a given id against a database
 * </p>
 * <p>
 * - implementation of a gen id method
 * </p>
 *
 * Currently deciding whether or not to merge Arkminter and this class into a
 * single class because, as far as I can tell, ids will always be generated in
 * the same, random or sequential. Ids differ only slightly from schema to
 * schema
 *
 * @author lruffin
 */
public abstract class Minter {

    private String charMap = "12345abc";
    private String idPrefix = "xyz1";
    private int idLength = 10;
    private boolean isRandom = true;
    private HashMap<String, String> tokenMaps;

    /**
     * Proposed use to retrieve any stored data from database. Deciding how
     * it'll interact with java beans. Need to read up more on it
     */
    public void retrieveSettings() {
        this.charMap = "12345abc";
        this.idPrefix = "xyz1";
        this.idLength = 10;
    }

    /**
     * Proposed use is to connect to a database so that ids can be checked and
     * user settings can be retrieved
     *
     * @return - true if connection is successful
     */
    public boolean connectDb() {
        return true;
    }

    /**
     * Checks a given id against a database.
     *
     * Perhaps another method to overload this can be used to check a list of
     * ids.
     *
     * @param id - given id
     * @return - true if id is true
     */
    public boolean checkId(String id) {
        return true;
    }

    /**
     * A method to overwritten by a subclass. This method will automatically
     * generate ids according a received token. More information on the token in
     * the ArkMinters implementation
     *
     * @param token - decides how ids are minted
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    public abstract JsonObject genIdAuto(String token);

    /**
     * A method to overwritten by a subclass. This method will generate ids
     * sequentially or randomly according the charMapping given by user
     *
     * @param isRandom - if true then create ids randomly
     * @return - A reference to a JsonObject that contains Json list of ids
     */
    public abstract JsonObject genIdCustom(boolean isRandom);

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
}
