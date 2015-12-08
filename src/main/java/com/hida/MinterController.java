package com.hida;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.sql.SQLException;

import java.util.Map;
import java.util.Properties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.servlet.ModelAndView;

/**
 * A controller class that paths the user to all jsp files in WEB_INF/jsp.
 *
 * @author lruffin
 */
@Controller
public class MinterController {

    // creates a fair reentrant Lock to bottle-neck access to printing pids
    private final ReentrantLock Lock = new ReentrantLock(true);

    // create a database to be used to create and count number of ids
    private final DatabaseManager DatabaseManager = new DatabaseManager();

    // Logger; logfile to be stored in resource folder
    private static final Logger Logger = LoggerFactory.getLogger(MinterController.class);

    // fields for minter's default values, cached values
    private final String CONFIG_FILE = "minter_config.properties";

    /**
     * Creates a path to mint ids. If parameters aren't given then printPids
     * will resort to using the default values found in minter_config.properties
     *
     * @param input requested number of ids to mint
     * @param model serves as a holder for the model so that attributes can be
     * added.
     * @param parameters parameters given by user to instill variety in ids
     * @return paths user to mint.jsp
     */
    @RequestMapping(value = {"/mint/{input}"},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String printPids(@PathVariable String input, ModelMap model,
            @RequestParam Map<String, String> parameters)
            throws Exception {

        // ensure that only one thread access the minter at any given time
        Lock.lock();

        // message variable to be sent to mint.jsp
        String message;
        try {

            int amount = Integer.parseInt(input);

            //LOG.info("retrieving data...");
            System.out.print("retrieving data...");
            // retrieve default setting
            MinterParameter minterParameter = new MinterParameter(parameters);

            // create connection
            DatabaseManager.createConnection();

            // instantiate the correct minter and calculate remaining number of permutations
            long remainingPermutations;
            Minter minter;
            if (minterParameter.isAuto()) {
                minter = new Minter(DatabaseManager,
                        minterParameter.getPrepend(),
                        minterParameter.getTokenType(),
                        minterParameter.getRootLength(),
                        minterParameter.getPrefix(),
                        minterParameter.isSansVowels());
                remainingPermutations
                        = DatabaseManager.getPermutations(minterParameter.getPrefix(),
                                minterParameter.getTokenType(),
                                minterParameter.getRootLength(),
                                minterParameter.isSansVowels());
            } else {
                minter = new Minter(DatabaseManager,
                        minterParameter.getPrepend(),
                        minterParameter.getCharMap(),
                        minterParameter.getPrefix(),
                        minterParameter.isSansVowels());

                remainingPermutations
                        = DatabaseManager.getPermutations(minterParameter.getPrefix(),
                                minterParameter.isSansVowels(),
                                minterParameter.getCharMap(),
                                minterParameter.getTokenType());
            }

            // throw an exception if the requested amount of ids can't be generated
            if (remainingPermutations < amount) {
                throw new NotEnoughPermutationsException(remainingPermutations, amount);
            }
            // have the minter create the ids and assign it to message
            if (minterParameter.isAuto()) {
                if (minterParameter.isRandom()) {
                    System.out.println("making autoRandom");
                    message = minter.genIdAutoRandom(amount);
                } else {
                    System.out.println("making autoSequential");
                    message = minter.genIdAutoSequential(amount);
                }
            } else {
                if (minterParameter.isRandom()) {
                    System.out.println("making customRandom");
                    message = minter.genIdCustomRandom(amount);
                } else {
                    System.out.println("making customSequential");
                    message = minter.genIdCustomSequential(amount);
                }
            }
            // print list of ids to screen
            model.addAttribute("message", message);

            // close the connection
            minter.getDatabaseManager().closeConnection();
            

            // log error messages in catch statements, call error handlers here
        } finally {
            // grants unlocks method and gives access to longest waiting thread            
            Lock.unlock();
        }
        // return to mint.jsp
        return "mint";
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

    ///*
    @ExceptionHandler(NotEnoughPermutationsException.class)
    public ModelAndView handlePermutationError(HttpServletRequest req, Exception exception) {
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 400);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());

        mav.setViewName("error");
        return mav;
        
    }
    //*/
    @ExceptionHandler(BadParameterException.class)
    public ModelAndView handleBadParameterError(HttpServletRequest req, Exception exception) {
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 400);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());

        mav.setViewName("error");
        return mav;
    }

    ///*
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneralError(HttpServletRequest req, Exception exception) {
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 500);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());

        mav.setViewName("error");
        return mav;
    }
    //*/

    /**
     * A class used to store the parameters given via REST end-point /mint.
     */
    private class MinterParameter {

        // Fields; detailed description in Minter class
        private String Prepend;
        private String Prefix;
        private String TokenType;
        private String CharMap;
        private int RootLength;
        private boolean Auto;
        private boolean Random;
        private boolean SansVowels;

        /**
         * Aside from receiving parameters, the constructor will load the
         * properties file and store default values into fields. The parameters,
         * if any, will be replace the default values in the fields.
         *
         * @param parameters list of parameters given via REST end-point /mint
         * @throws IOException thrown whenever the configuration file cannot be
         * found or opened
         */
        private MinterParameter(Map<String, String> parameters) throws IOException {
            retrieveDefaultSetting();
            setDefaultSettings(parameters);
        }

        /**
         * Retrieve default settings for minter from property file.
         */
        private void retrieveDefaultSetting() throws IOException {
            // load property file
            Properties properties = new Properties();

            properties.load(Thread.currentThread().
                    getContextClassLoader().getResourceAsStream(
                            String.format("%s", CONFIG_FILE)));

            // retrieve values found in minter_config.properties file                                
            this.setPrepend((properties.getProperty("prepend")));
            this.setPrefix((properties.getProperty("prefix")));
            this.setTokenType((properties.getProperty("tokenType")));
            this.setCharMap((properties.getProperty("charMap")));
            this.setRootLength(Integer.parseInt(properties.getProperty("rootLength")));
            this.setAuto(Boolean.parseBoolean(properties.getProperty("auto")));
            this.setRandom(Boolean.parseBoolean(properties.getProperty("random")));
            this.setSansVowels(Boolean.parseBoolean(properties.getProperty("sansVowels")));

        } // end retrieveDefaultSetting     

        /**
         * Method that sets the fields to any given parameters. Defaults to
         * values found in minter_config.properties file located in resources
         * folder.
         *
         * @param parameters list of given parameters.
         */
        private void setDefaultSettings(Map<String, String> parameters) {

            if (parameters.containsKey("prepend")) {
                this.setPrepend(parameters.get("prepend"));
            }
            if (parameters.containsKey("prefix")) {
                this.setPrefix(parameters.get("prefix"));
            }
            if (parameters.containsKey("rootLength")) {
                this.setRootLength(Integer.parseInt(parameters.get("rootLength")));
            }
            if (parameters.containsKey("charMap")) {
                this.setCharMap(parameters.get("charMap"));
            }
            if (parameters.containsKey("tokenType")) {
                this.setTokenType(parameters.get("tokenType"));
            }
            if (parameters.containsKey("auto")) {
                this.setAuto(Boolean.parseBoolean(parameters.get("auto")));
            }
            if (parameters.containsKey("random")) {
                this.setRandom(Boolean.parseBoolean(parameters.get("random")));
            }
            if (parameters.containsKey("sansVowels")) {
                this.setSansVowels(Boolean.parseBoolean(parameters.get("sansVowels")));
            }
        }

        /* getters and setters */
        public String getPrepend() {
            return Prepend;
        }

        public void setPrepend(String Prepend) {
            this.Prepend = Prepend;
        }

        public String getPrefix() {
            return Prefix;
        }

        public void setPrefix(String Prefix) {
            this.Prefix = Prefix;
        }

        public String getTokenType() {
            return TokenType;
        }

        public void setTokenType(String TokenType) {
            this.TokenType = TokenType;
        }

        public String getCharMap() {
            return CharMap;
        }

        public void setCharMap(String CharMap) {
            this.CharMap = CharMap;
        }

        public int getRootLength() {
            return RootLength;
        }

        public void setRootLength(int RootLength) {
            this.RootLength = RootLength;
        }

        public boolean isAuto() {
            return Auto;
        }

        public void setAuto(boolean Auto) {
            this.Auto = Auto;
        }

        public boolean isRandom() {
            return Random;
        }

        public void setRandom(boolean Random) {
            this.Random = Random;
        }

        public boolean isSansVowels() {
            return SansVowels;
        }

        public void setSansVowels(boolean SansVowels) {
            this.SansVowels = SansVowels;
        }

        /**
         * Used for testing; subject to deletion.
         *
         * @return
         */
        @Override
        public String toString() {
            return String.format("prepend=%s\nprefix=%s\ntokenType=%s\nlength=%d\ncharMap=%s"
                    + "\nauto=%b\nrandom=%b\nsans%b",
                    Prepend, Prefix, TokenType, RootLength, CharMap, Auto, Random, SansVowels);
        }

    }

} // end class
