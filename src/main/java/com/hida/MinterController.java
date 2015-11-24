package com.hida;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.sql.SQLException;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A controller class that paths the user to all jsp files in WEB_INF/jsp.
 *
 * @author lruffin
 */
@Controller
public class MinterController {

    // fields for minter's default values, cached values
    private final String CONFIG_FILE = "minter_config.properties";
    private String prependedString;
    private String prefix;
    private String minterType;
    private String tokenType;
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
                if (minterType.equals("AUTO")) {
                    
                    message = minter.genIdAuto(tokenType);
                } else if (minterType.equals("SEQUENTIAL")) {
                    message = minter.genIdSequential(tokenType);
                } else if (minterType.equals("CUSTOM")) {
                    message = "unsupported";
                    //message = minter.genIdCustom(tokenType);
                } else {
                    message = "error";
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
                    + "length=%d\ncharMap=%s\nminterType=%s\ntokenType=%s",
                    prependedString, prefix, length, charMap,
                    minterType, tokenType));
            System.out.println("here, null found");
            System.out.println(Arrays.toString(exception.getStackTrace()));
            System.out.println("here, null found");
        }
        catch (TooManyPermutationsException exception){
            
        }
        return "mint";
    }

    /**
     * Method that sets the fields to any given parameters. Defaults to values
     * found in minter_config.properties file.
     *
     * @param parameters - list of given parameters.
     */
    public void setDefaultSetting(Map<String, String> parameters) {

        if (parameters.containsKey("prepend")) {
            this.setPrependedString(parameters.get("prepend"));
        }
        if (parameters.containsKey("prefix")) {
            this.setPrefix(parameters.get("prefix"));
        }
        if (parameters.containsKey("length")) {
            this.setLength(Integer.parseInt(parameters.get("length")));
        }
        if (parameters.containsKey("charMap")) {
            this.setCharMap(parameters.get("charMap"));
        }
        if (parameters.containsKey("minterType")) {
            this.minterType = parameters.get("minterType");
        }
        if (parameters.containsKey("tokenType")) {
            this.setTokenType(parameters.get("tokenType"));
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

    
    
    @ExceptionHandler({SQLException.class})
    public String parameterErrorHandler(Map<String, String> incorrectParam){
        String message = "";
        final String errorMessage = "";
        return message;
    }
            
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
            minterType = properties.getProperty("minterType");

            setCharMap(properties.getProperty("charMap"));

            setTokenType(properties.getProperty("tokenType"));

            setLength(Integer.parseInt(properties.getProperty("length")));

            setPrefix(properties.getProperty("prefix"));

            setPrependedString((properties.getProperty("prependedString")));

        } catch (IOException exception) {
            System.out.println(exception.getMessage());
        }
    } // end retrieveDefaultSetting
    
    
    /**
     * Produces a 400 error
     */
    @ResponseStatus(value=HttpStatus.BAD_REQUEST, 
            reason="Given prefix and length parameters "
                    + "produce too many permutations")  
    public class TooManyPermutationsException extends RuntimeException {
        
        
        public TooManyPermutationsException(
                String prefix, int length, int numIdRemaining){
            super(String.format(
                    "There are %d remaining ids can be generated given "
                    + "current prefix (%s) and length parameters (%d)",
                    numIdRemaining, prefix, length));
            
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

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String idType) {
        this.tokenType = idType;
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

} // end class
