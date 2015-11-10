package com.hida;

import java.util.Random;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Creates a generic minter strictly for testing purposes
 *
 * @author lruffin
 */
public class PseudoMinter extends Minter {

    // fields
    private final int AMOUNT;

    /**
     * Creates a pseduo minter to generate random ids, disregarding format.
     * Strictly for testing and demonstration purposes only
     *
     * @param amount
     */
    public PseudoMinter(int amount) {
        retrieveSettings();
        this.AMOUNT = amount;
    }

    /**
     * Creates a ids based on what token is received. More info can be found 
     * in ArkMinter's version
     * @param token - token deciding the format
     * @return 
     */
    @Override
    public JsonObject genIdAuto(String token) {
        Random rng = new Random();
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObjectBuilder list = factory.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (int i = 0; i < this.AMOUNT; i++) {
            String id = getIdPrefix();

            int maxLength = getIdLength() - getIdPrefix().length();
            for (int j = 0; id.length() < maxLength; j++) {
                int randomNum = rng.nextInt(36);

                id = id + (randomNum >= 10 ? Character.valueOf((char) (randomNum + 87))
                        : new StringBuilder().append(randomNum).append("").toString());
            }
            arrayBuilder.add(Json.createObjectBuilder().add("id", i).add("name", id));
        }
        return list.add("pseudo", arrayBuilder).build();
    }

    /**
     * Not implemented as it is currently unnecessary to do so 
     * @param isRandom
     * @return 
     */
    @Override
    public JsonObject genIdCustom(boolean isRandom) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
