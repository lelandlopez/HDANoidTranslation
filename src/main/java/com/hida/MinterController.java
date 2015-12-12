package com.hida;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * A controller class that paths the user to all jsp files in WEB_INF/jsp.
 *
 * @author lruffin
 */
@Controller
public class MinterController {

    // Logger; logfile to be stored in resource folder
    private static final Logger Logger = LoggerFactory.getLogger(MinterController.class);
    /**
     * creates a fair reentrant RequestLock to bottle-neck access to printing
     * pids
     */
    private final ReentrantLock RequestLock = new ReentrantLock(true);

    /**
     * create a database to be used to create and count number of ids
     */
    private final DatabaseManager DatabaseManager;// = new DatabaseManager();


    /**
     * fields for minter's default values, cached values
     */
    private final String CONFIG_FILE = "minter_config.properties";

    /**
     * Constructor that loads the property file and retrieves the values stored
     * at the databasePath and databaseName keys. If the the keys are empty then
     * a database called PID.db will be made at the default location specified by
     * the server. 
     *
     * @throws IOException Thrown if the property file was not found.
     */
    public MinterController() throws IOException {
        Properties properties = new Properties();

        properties.load(Thread.currentThread().
                getContextClassLoader().getResourceAsStream(CONFIG_FILE));

        // retrieve values found in minter_config.properties file                                
        String path = properties.getProperty("databasePath");
        Logger.info("Getting database path: "+path);
        String name = properties.getProperty("databaseName");
        Logger.info("getting Database Name: "+name);

        if (!name.isEmpty()) {
            if (!path.endsWith("\\")) {
                path += "\\";
                Logger.warn("Database Path not set correctly: adding \\");
            }
            DatabaseManager = new DatabaseManager(path, name);
            Logger.info("Creating DataBase Manager with Path="+path+", Name="+name);
        } else {
            DatabaseManager = new DatabaseManager();
            Logger.info("Creating DatabaseManager with "
                    +"Path="+DatabaseManager.getDatabasePath()+", "
                    +"Name="+DatabaseManager.getDatabaseName());
        }

    }

    /**
     * Creates a path to mint ids. If parameters aren't given then printPids
     * will resort to using the default values found in minter_config.properties
     *
     * @param requestedAmount requested number of ids to mint
     * @param model serves as a holder for the model so that attributes can be
     * added.
     * @param parameters parameters given by user to instill variety in ids
     * @return paths user to mint.jsp
     * @throws Exception catches all sorts of exceptions that may be thrown by
     * any methods
     */
    @RequestMapping(value = {"/mint/{requestedAmount}"},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String printPids(@PathVariable long requestedAmount, ModelMap model,
            @RequestParam Map<String, String> parameters)
            throws Exception {

        // ensure that only one thread access the minter at any given time
        RequestLock.lock();
        Logger.warn("Request to Minter made, LOCKING MINTER");
        
        // message variable to be sent to mint.jsp
        String message;
        try {
            System.out.print("retrieving data...");
            Logger.info("Retrieving data for Parameter");
            
            // retrieve default setting
            MinterParameter minterParameter = new MinterParameter(parameters);

            // create connection and sets up the database if need be
            DatabaseManager.createConnection();
            Logger.info("Database Connection Created to: "+DatabaseManager.getDatabaseName());

            // instantiate the correct minter and calculate remaining number of permutations
            long remainingPermutations;
            Minter minter;
            if (minterParameter.isAuto()) {
                minter = createAutoMinter(minterParameter, requestedAmount);

                remainingPermutations
                        = DatabaseManager.getPermutations(minterParameter.getPrefix(),
                                minterParameter.getTokenType(),
                                minterParameter.getRootLength(),
                                minterParameter.isSansVowels());
            } else {
                minter = createCustomMinter(minterParameter, requestedAmount);

                remainingPermutations
                        = DatabaseManager.getPermutations(minterParameter.getPrefix(),
                                minterParameter.isSansVowels(),
                                minterParameter.getCharMap(),
                                minterParameter.getTokenType());
            }

            // throw an exception if the requested amount of ids can't be generated
            if (remainingPermutations < requestedAmount) {
                Logger.error("Not enough remaining Permutations, "
                        + "Requested Amount="+requestedAmount+" --> "
                        + "Amount Remaining="+remainingPermutations);
                throw new NotEnoughPermutationsException(remainingPermutations, requestedAmount);
            }
            // have the minter create the ids and assign it to message
            if (minterParameter.isAuto()) {
                if (minterParameter.isRandom()) {
                    System.out.println("making autoRandom");
                    Logger.info("Generated IDs will use the Format: "+minterParameter);
                    Logger.info("Making autoRandom Generated IDs, Amount Requested="+requestedAmount);
                    message = minter.genIdAutoRandom(requestedAmount);
                    //Logger.info("Message from Minter: "+message);
                } else {
                    System.out.println("making autoSequential");
                    Logger.info("Generated IDs will use the Format: "+minterParameter);
                    Logger.info("Making autoSequential Generated IDs, Amount Requested="+requestedAmount);
                    
                    message = minter.genIdAutoSequential(requestedAmount);
                    //Logger.info("Message from Minter: "+message);
                }
            } else {
                if (minterParameter.isRandom()) {
                    System.out.println("making customRandom");
                    Logger.info("Generated IDs will use the Format: "+minterParameter);
                    Logger.info("Making customRandom Generated IDs, Amount Requested="+requestedAmount);
                    message = minter.genIdCustomRandom(requestedAmount);
                    //Logger.info("Message from Minter: "+message);
                } else {
                    System.out.println("making customSequential");
                    Logger.info("Generated IDs will use the Format: "+minterParameter);
                    Logger.info("Making customSequential Generated IDs, Amount Requested="+requestedAmount);
                    message = minter.genIdCustomSequential(requestedAmount);
                    //Logger.info("Message from Minter: "+message);
                }
            }
            // print list of ids to screen
            model.addAttribute("message", message);
                        

            // close the connection
            minter.getDatabaseManager().closeConnection();
        } finally {
            // unlocks RequestLock and gives access to longest waiting thread            
            RequestLock.unlock();
            Logger.warn("Request to Minter Finished, UNLOCKING MINTER");
        }
        // return to mint.jsp
        return "mint";
    }

    /**
     * Maps to home page.
     *
     * @return name of the index page
     */
    @RequestMapping(value = {""},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String displayIndex() {
        Logger.info("index page called");
        return "index";
    }

    /**
     * maps to settings.jsp so that the user may input data in a form.
     *
     * @param model
     * @return name of the settings page
     */
    @RequestMapping(value = {"/settings"},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String handleForm(ModelMap model) {
        Logger.info("settings page Called");
        return "settings";
    }

    /**
     * Because the minter construction checks the parameters for validity, this
     * method not only instantiates an AutoMinter but also checks whether or not
     * the requested amount of ids is valid.
     *
     * @param minterParameter The parameters of the minter given by the rest end
     * point and default values.
     * @param requestedAmount The amount of ids requested.
     * @return an AutoMinter
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    private Minter createAutoMinter(MinterParameter minterParameter, long requestedAmount)
            throws BadParameterException {
        Minter minter = new Minter(DatabaseManager,
                minterParameter.getPrepend(),
                minterParameter.getTokenType(),
                minterParameter.getRootLength(),
                minterParameter.getPrefix(),
                minterParameter.isSansVowels());

        if (minter.isValidAmount(requestedAmount)) {
            return minter;
        } else {
            Logger.error("Request amount of "+requestedAmount+" IDs is unavailable");
            throw new BadParameterException(requestedAmount, "Requested Amount");
        }
    }

    /**
     * Because the minter construction checks the parameters for validity, this
     * method not only instantiates a CustomMinter but also checks whether or
     * not the requested amount of ids is valid.
     *
     * @param minterParameter The parameters of the minter given by the rest end
     * point and default values.
     * @param requestedAmount The amount of ids requested.
     * @return a CustomMinter
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    private Minter createCustomMinter(MinterParameter minterParameter, long requestedAmount)
            throws BadParameterException {
        Minter minter = new Minter(DatabaseManager,
                minterParameter.getPrepend(),
                minterParameter.getCharMap(),
                minterParameter.getPrefix(),
                minterParameter.isSansVowels());

        if (minter.isValidAmount(requestedAmount)) {
            return minter;
        } else {
            Logger.error("Request amount of "+requestedAmount+" IDs is unavailable");
            throw new BadParameterException(requestedAmount, "Requested Amount");
        }
    }

    /**
     *
     * @param req
     * @param exception
     * @return
     */
    @ExceptionHandler(NotEnoughPermutationsException.class)
    public ModelAndView handlePermutationError(HttpServletRequest req, Exception exception) {
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 400);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());
        Logger.error("Error with permutation: "+exception.getMessage());
        mav.setViewName("error");
        return mav;

    }

    /**
     *
     * @param req
     * @param exception
     * @return
     */
    @ExceptionHandler(BadParameterException.class)
    public ModelAndView handleBadParameterError(HttpServletRequest req, Exception exception) {
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 400);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());
        Logger.error("Error with bad parameter: "+ exception.getMessage());
        
        mav.setViewName("error");
        return mav;
    }

    /**
     *
     * @param req
     * @param exception
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneralError(HttpServletRequest req, Exception exception) {
        
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 500);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());
        Logger.error("Error General Error: "+ exception.getMessage());
        
        mav.setViewName("error");
        return mav;
    }

    /**
     *
     * @return
     */
    public int getRequestLockQueueLength() {
        return this.RequestLock.getQueueLength();
    }

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
        private boolean SansVowels;
        // determines whether or not an auto minter or custom minter is created 
        private boolean Auto;

        // determines whether or not ids are randomly or sequentially created
        private boolean Random;

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
                    getContextClassLoader().getResourceAsStream(CONFIG_FILE));

            // retrieve values found in minter_config.properties file                                
            this.setPrepend((properties.getProperty("prepend")));
            this.setPrefix((properties.getProperty("prefix")));
            this.setTokenType((properties.getProperty("tokenType")));
            this.setCharMap((properties.getProperty("charMap")));
            this.setRootLength(Integer.parseInt(properties.getProperty("rootLength")));
            this.setAuto(Boolean.parseBoolean(properties.getProperty("auto")));
            this.setRandom(Boolean.parseBoolean(properties.getProperty("random")));
            this.setSansVowels(Boolean.parseBoolean(properties.getProperty("sansVowels")));

        }

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
         * Used for testing
         *
         * @return
         */
        @Override
        public String toString() {
            return String.format("prepend=%s\tprefix=%s\ttokenType=%s\tlength=%d\tcharMap=%s"
                    + "\tauto=%b\trandom=%b\tsans%b",
                    Prepend, Prefix, TokenType, RootLength, CharMap, Auto, Random, SansVowels);
        }
    }

    private class DatabaseParameter {

        private String Path = "";
        private String Name = "";

        public void loadParameters() throws IOException {
            Properties properties = new Properties();

            properties.load(Thread.currentThread().
                    getContextClassLoader().getResourceAsStream(CONFIG_FILE));

            // retrieve values found in minter_config.properties file                                
            Path = properties.getProperty("databasePath");
            Name = properties.getProperty("databaseName");
            Logger.info("Loading Database Path: "+Path+" Loading Database Name: "+Name);
            

        }

        public String getPath() {
            return Path;
        }

        public void setPath(String Path) {
            this.Path = Path;
        }

        public String getName() {
            return Name;
        }

        public void setName(String Name) {
            this.Name = Name;
        }

    }
}
