package com.hida;

import java.io.FileInputStream;
import javax.json.JsonObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * A controller class that paths the user to all jsp files
 *
 * @author lruffin
 */
@Controller
public class MinterController {

    // fields for minter's default values
    private final String CONFIG_FILE = "minter_config.properties";
    private String prependedString;
    private String prefix;
    private boolean isSequential;
    private boolean isAuto;
    private String idType;
    private int length;
    private String charMap;

    /**
     * Creates a path to mint ids
     *
     * @param input - requested number of ids to mint
     * @param model - serves as a holder for the model so that attributes can be
     * added
     * @param prepend
     * @param prefix
     * @param isSequential
     * @param idType
     * @param length
     * @param charMap
     * @param isAuto
     * @return - paths user to mint.jsp
     */
    @RequestMapping(value = {"/mint/{input}"},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String printPids(@PathVariable String input, ModelMap model ///*
            ,
            @RequestParam(value = "prepend") String prepend,
            @RequestParam(value = "prefix") String prefix,
            @RequestParam(value = "isSequential") String isSequential,
            @RequestParam(value = "idType") String idType,
            @RequestParam(value = "length") String length,
            @RequestParam(value = "charMap") String charMap,
            @RequestParam(value = "isAuto") String isAuto
    //*/
    ) {
        String message;
        try {
            int amount = Integer.parseInt(input);

            Minter minter = new Minter(amount);

            // retrieve default setting
            this.retrieveDefaultSetting();

            /*// bad
             this.setDefaultSetting(minter,
             prependedString, prefix, isAuto + "", idType,
             length + "", charMap, isAuto + "");
             */
            ///* // good
            this.setDefaultSetting(minter,
                    prepend, prefix, isSequential, idType,
                    length, charMap, isAuto);
            //*/
            if (minter.getDatabaseManager().createConnection()) {
                if (this.isAuto) {
                    message = minter.genIdAuto(idType);
                } else {
                    message = minter.genIdCustom(this.isSequential);
                }
                minter.getDatabaseManager().closeConnection();
                model.addAttribute("message", message);
            } else {

            }

            //createArkMinter(amount, model);
        } // detects number fomatting errors in input
        catch (NumberFormatException exception) {
            message = String.format(
                    "input error %s", exception.getMessage());
            model.addAttribute("message", message);
        } // detects any unimplemented methods
        catch (UnsupportedOperationException exception) {
            message = String.format(
                    "%s", exception.getMessage());
            model.addAttribute("message", message);
        }
        return "mint";
    }

    public void setDefaultSetting(Minter minter, String prepend, String prefix,
            String isSequential, String idType, String length, String charMap,
            String isAuto) {
        if (prepend == null) {
            minter.setPREPEND("");
        } else {
            minter.setPREPEND(this.prependedString);
        }
        if (prefix == null) {
            minter.setIdPrefix(this.prefix);
        } else {
            minter.setIdPrefix(prefix);
        }
        if (isSequential != null) {
            this.setIsSequential(Boolean.parseBoolean(isSequential));
        }
        if (idType != null) {
            this.setIdType(idType);
        }
        if (length == null) {
            minter.setIdLength(this.length);
        } else {
            minter.setIdLength(Integer.parseInt(length));
        }
        if (charMap == null) {
            minter.setCharMap(this.charMap);
        } else {
            minter.setCharMap(charMap);
        }
        if (isAuto != null) {
            this.setIsAuto(Boolean.parseBoolean(isAuto));
        }

    }

    /**
     * Used to create PseudoMinter. Will be implemented after deciding how data
     * should be received from user. JavaBeans?
     *
     * @param amount - requested number of ids to mint
     * @param model - serves as a holder for the model so that attributes can be
     * added
     */
    /*
     private void createPseudoMinter(int amount, ModelMap model) {
     PseudoMinter pminter = new PseudoMinter(amount);

     //pminter.retrieveSettings();
     JsonObject idList = pminter.genIdAuto("EXTENDED");

     model.addAttribute("message", idList);
     }
     */
    /**
     * Used to create ArkMinter.
     *
     * @param amount - requested number of ids to mint
     * @param model - serves as a holder for the model so that attributes can be
     * added
     */
    /*
     private void createArkMinter(int amount, ModelMap model) {
     Minter arkminter = new Minter(amount);

     arkminter.retrieveSettings();

     JsonObject idList;// = arkminter.genIdAuto("EXTENDED");

     model.addAttribute("message", idList);
     }
     */
    /**
     * Maps to home page.
     *
     * @return
     */
    @RequestMapping(value = {""},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String printIndex() {
        return "index";
    }

    /**
     * maps to settings.jsp so that the user may input data in a form.
     *
     * @param model
     * @return
     */
    @RequestMapping(value = {"/settings"},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String handleForm(ModelMap model) {
        return "settings";
    }

    /**
     * Retrieve default settings for minter from property file
     */
    private void retrieveDefaultSetting() {
        try {

            // load property file
            Properties properties = new Properties();
            InputStream input = new FileInputStream(CONFIG_FILE);

            properties.load(input);

            // get configurations from property file
            setIsSequential(
                    Boolean.parseBoolean(
                            properties.getProperty("isSequential")));
            setCharMap(properties.getProperty("charMap"));
            setIdType(properties.getProperty("idType"));
            setLength(Integer.parseInt(properties.getProperty("idType")));
            setPrefix(properties.getProperty("prefix"));
            setPrependedString((properties.getProperty("prependedString")));
        } catch (IOException exception) {

        }
    }

    /* typical getter and setter methods */
    public String getPrependedString() {
        return prependedString;
    }

    public void setPrependedString(String prependedString) {
        this.prependedString = prependedString;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isIsSequential() {
        return isSequential;
    }

    public void setIsSequential(boolean isSequential) {
        this.isSequential = isSequential;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getCharMap() {
        return charMap;
    }

    public void setCharMap(String charMap) {
        this.charMap = charMap;
    }

    public boolean isIsAuto() {
        return isAuto;
    }

    public void setIsAuto(boolean isAuto) {
        this.isAuto = isAuto;
    }

}
