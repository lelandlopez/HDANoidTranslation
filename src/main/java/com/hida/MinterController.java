package com.hida;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

/**
 * A controller class that paths the user to all jsp files in WEB_INF/jsp.
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
     * Creates a path to mint ids. If parameters aren't given then printPids
     * will resort to using the default values found in minter_config.properties
     *
     * @param input - requested number of ids to mint
     * @param model - serves as a holder for the model so that attributes can be
     * added.
     * @param parameters - parameters given by user to instill variety in ids
     * @return - paths user to mint.jsp
     */
    @RequestMapping(value = {"/mint/{input}"},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String printPids(@PathVariable String input, ModelMap model,
            @RequestParam Map<String, String> parameters) {

        // message variable to be sent to mint.jsp
        String message;
        try {
            int amount = Integer.parseInt(input);

            System.out.print("retrieving data...");
            // retrieve default setting
            this.retrieveDefaultSetting();

            // choose between default and given client parameters
            this.setDefaultSetting(parameters);

            // make minter object with loaded results            
            Minter minter = new Minter(this.getCharMap(),
                    this.getPrependedString(),
                    amount,
                    this.getLength(),
                    this.getPrefix());

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
        } // used to see if values were properly retrieved from property file
        catch (NullPointerException exception) {
            System.out.println(String.format("prepend=%s\nprefix=%s\n"
                    + "length=%d\ncharMap=%s\nisAuto=%b\n"
                    + "isSequential=%b\nidType=%s", prependedString, prefix,
                    length, charMap, isAuto, isSequential, idType));
            System.out.println(Arrays.toString(exception.getStackTrace()));
        }

        return "mint";
    }

    /**
     * Method that sets the fields to any given parameters. Defaults to values
     * found in minter_config.properties file.
     * @param parameters - list of given parameters.
     */
    public void setDefaultSetting(Map<String, String> parameters) {

        if (parameters.containsKey("prepend")) {
            this.setPrependedString(parameters.get("prepend"));
        }
        if (parameters.containsKey("prefix")) {
            this.setPrefix(parameters.get("prefix"));
            System.out.println("prefix=" + parameters.get("prefix"));
        }

        if (parameters.containsKey("length")) {
            this.setLength(Integer.parseInt(parameters.get("length")));
        }
        if (parameters.containsKey("charMap")) {
            this.setCharMap(parameters.get("charMap"));
        }
        if (parameters.containsKey("isAuto")) {
            this.setIsAuto(Boolean.parseBoolean(parameters.get("isAuto")));
        }
        if (parameters.containsKey("isSequential")) {
            this.setIsSequential(
                    this.isSequential
                    = Boolean.parseBoolean(
                            parameters.get("isSequential")));
        }
        if (parameters.containsKey("idType")) {
            this.setIdType(parameters.get("idType"));
        }

    }

    
    /**
     * Maps to home page.
     *
     * @return
     */
    @RequestMapping(value = {""},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String displayIndex() {
        return "index";
    } // end printIndex

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
    } // end handleForm

    /**
     * Retrieve default settings for minter from property file.
     */
    private void retrieveDefaultSetting() {
        try {

            // load property file
            Properties properties = new Properties();
            
            properties.load(Thread.currentThread().
                    getContextClassLoader().getResourceAsStream(
                            String.format("%s", CONFIG_FILE)));
            
            // retrieve values found in minter_config.properties file
            setIsSequential(
                    Boolean.parseBoolean(
                            properties.getProperty("isSequential")));
            
            setCharMap(properties.getProperty("charMap"));

            setIdType(properties.getProperty("idType"));

            setLength(Integer.parseInt(properties.getProperty("length")));

            setPrefix(properties.getProperty("prefix"));

            setPrependedString((properties.getProperty("prependedString")));

            setIsAuto(
                    Boolean.parseBoolean((properties.getProperty("isAuto"))));

        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
    } // end retrieveDefaultSetting

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
} // end class
