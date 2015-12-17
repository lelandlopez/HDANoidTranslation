package com.hida;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codehaus.jackson.map.ObjectMapper;
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
     * Creates a fair reentrant RequestLock to serialize each request
     * sequentially instead of concurrently. pids
     */
    private static final ReentrantLock RequestLock = new ReentrantLock(true);

    /**
     * fields for minter's default values, cached values
     */
    private static final String CONFIG_FILE = "minter_config.properties";

    /**
     * create a database to be used to create and count number of ids
     */
    private final DatabaseManager DatabaseManager;

    /**
     *
     */
    private final CachedSettings Settings;

    /**
     * Constructor that loads the property file and retrieves the values stored
     * at the databasePath and databaseName keys. If the the keys are empty then
     * a database called PID.db will be made at the default location specified
     * by the server.
     *
     * @throws IOException Thrown if the property file was not found.
     */
    public MinterController() throws IOException, SQLException, BadParameterException, ClassNotFoundException {

        Properties properties = new Properties();

        properties.load(Thread.currentThread().
                getContextClassLoader().getResourceAsStream(CONFIG_FILE));

        // Retrieves the path and the name of a database from the property file for this session                            
        String path = properties.getProperty("databasePath");
        Logger.info("Getting database path: " + path);
        String name = properties.getProperty("databaseName");
        Logger.info("getting Database Name: " + name);

        // Creates the database at a location specified by the properties file
        if (!name.isEmpty()) {
            if (!path.endsWith("\\")) {
                path += "\\";
                Logger.warn("Database Path not set correctly: adding \\");
            }
            DatabaseManager = new DatabaseManager(path, name);
            Logger.info("Creating DataBase Manager with Path=" + path + ", Name=" + name);
        } else {
            DatabaseManager = new DatabaseManager();
            Logger.info("Creating DatabaseManager with "
                    + "Path=" + DatabaseManager.getDatabasePath() + ", "
                    + "Name=" + DatabaseManager.getDatabaseName());
        }
        // create connection and sets up the database if need be
        DatabaseManager.createConnection();
        Logger.info("Database Connection Created to: " + DatabaseManager.getDatabaseName());

        // retrieve the default settings from the database and store it in Settings
        Settings = new CachedSettings();
        Settings.retrieveSettings();

        DatabaseManager.closeConnection();
    }

    /**
     * Redirects to the index after retrieving updated settings from the
     * administration panel.
     *
     * @param request HTTP request from the administration panel
     * @param response HTTP response that redirects to the administration panel
     * after updating the new settings.
     * @return The name of the page to redirect.
     * @throws SQLException
     * @throws BadParameterException Thrown whenever a bad parameter is
     * detected.
     * @throws ClassNotFoundException Thrown whenever a class does not exist.
     */
    @RequestMapping(value = {"/confirmation"},
            method = {org.springframework.web.bind.annotation.RequestMethod.POST})
    public String handleForm(HttpServletRequest request, HttpServletResponse response)
            throws ClassNotFoundException, SQLException, BadParameterException {
        try {
            // prevents other clients from accessing the database whenever the form is submitted
            RequestLock.lock();

            DatabaseManager.createConnection();

            Logger.info("in handleForm");
            String prepend = request.getParameter("prepend");
            String prefix = request.getParameter("idprefix");
            String isAuto = request.getParameter("mintType");
            String isRandom = request.getParameter("mintOrder");
            String sansVowels = request.getParameter("vowels");
            String digitToken;
            String lowerToken;
            String upperToken;
            String charMap;
            String rootLength = request.getParameter("idlength");

            boolean auto = isAuto.equals("auto");
            boolean random = isRandom.equals("random");
            boolean vowels = sansVowels == null;

            // assign a non-null value to prepend, prefix, and rootLength
            if (prepend == null) {
                prepend = "";
            }
            if (prefix == null) {
                prefix = "";
            }
            if ((rootLength == null || rootLength.isEmpty()) && !auto) {
                rootLength = "1";
            }

            int length = Integer.parseInt(rootLength);

            // assign values based on which minter type was selected
            if (auto) {
                digitToken = request.getParameter("digits");
                lowerToken = request.getParameter("lowercase");
                upperToken = request.getParameter("uppercase");

                TokenType tokenType;

                // gets the tokenmap value
                if (digitToken != null && lowerToken == null && upperToken == null) {
                    tokenType = TokenType.DIGIT;
                } else if (digitToken == null && lowerToken != null && upperToken == null) {
                    tokenType = TokenType.LOWERCASE;
                } else if (digitToken == null && lowerToken == null && upperToken != null) {
                    tokenType = TokenType.UPPERCASE;
                } else if (digitToken == null && lowerToken != null && upperToken != null) {
                    tokenType = TokenType.MIXEDCASE;
                } else if (digitToken != null && lowerToken != null && upperToken == null) {
                    tokenType = TokenType.LOWER_EXTENDED;
                } else if (digitToken == null && lowerToken == null && upperToken != null) {
                    tokenType = TokenType.UPPER_EXTENDED;
                } else if (digitToken != null && lowerToken != null && upperToken != null) {
                    tokenType = TokenType.MIXED_EXTENDED;
                } else {
                    throw new BadParameterException();
                }

                DatabaseManager.assignSettings(
                        prepend, prefix, tokenType, length, auto, random, vowels);
            } else {

                charMap = request.getParameter("charmapping");
                if (charMap == null || charMap.isEmpty()) {
                    throw new BadParameterException();
                }

                DatabaseManager.assignSettings(prepend, prefix, charMap, auto, random, vowels);
            }
            // close the connection and update the cache
            Settings.retrieveSettings();
            DatabaseManager.closeConnection();
        } finally {
            // unlocks RequestLock and gives access to longest waiting thread            
            RequestLock.unlock();
            Logger.warn("Request to update default settings finished, UNLOCKING MINTER");
        }
        // redirect to the administration panel
        return "redirect:";

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
            // create a connection  
            DatabaseManager.createConnection();
            Logger.info("Database Connection Created to: " + DatabaseManager.getDatabaseName());

            // override default settings
            CachedSettings tempSettings = overrideDefaults(parameters);

            // instantiate the correct minter and calculate remaining number of permutations
            long remainingPermutations;
            Minter minter;
            if (Settings.isAuto()) {
                minter = createAutoMinter(requestedAmount, tempSettings);
                remainingPermutations
                        = DatabaseManager.getPermutations(tempSettings.getPrefix(),
                                tempSettings.getTokenType(),
                                tempSettings.getRootLength(),
                                tempSettings.isSansVowels());
            } else {
                minter = createCustomMinter(requestedAmount, tempSettings);
                remainingPermutations
                        = DatabaseManager.getPermutations(tempSettings.getPrefix(),
                                tempSettings.isSansVowels(),
                                tempSettings.getCharMap(),
                                minter.getTokenType());
            }

            // throw an exception if the requested amount of ids can't be generated
            if (remainingPermutations < requestedAmount) {
                Logger.error("Not enough remaining Permutations, "
                        + "Requested Amount=" + requestedAmount + " --> "
                        + "Amount Remaining=" + remainingPermutations);
                throw new NotEnoughPermutationsException(remainingPermutations, requestedAmount);
            }
            Set<Id> idList;
            // have the minter create the ids and assign it to message
            if (tempSettings.isAuto()) {
                if (Settings.isRandom()) {

                    idList = minter.genIdAutoRandom(requestedAmount);
                    Logger.info("Generated IDs will use the Format: " + Settings);
                    Logger.info("Making autoRandom Generated IDs, Amount Requested="
                            + requestedAmount);
                } else {

                    idList = minter.genIdAutoSequential(requestedAmount);
                    Logger.info("Generated IDs will use the Format: " + Settings);
                    Logger.info("Making autoSequential Generated IDs, Amount Requested="
                            + requestedAmount);
                }
            } else {
                if (tempSettings.isRandom()) {

                    idList = minter.genIdCustomRandom(requestedAmount);
                    Logger.info("Generated IDs will use the Format: " + Settings);
                    Logger.info("Making customRandom Generated IDs, Amount Requested="
                            + requestedAmount);

                } else {

                    idList = minter.genIdCustomSequential(requestedAmount);
                    Logger.info("Generated IDs will use the Format: " + Settings);
                    Logger.info("Making customSequential Generated IDs, Amount Requested="
                            + requestedAmount);
                }
            }

            message = convertListToJson(idList, tempSettings.getPrepend());
            //Logger.info("Message from Minter: "+message);

            // print list of ids to screen
            model.addAttribute("message", message);

            // close the connection
            DatabaseManager.closeConnection();
        } finally {
            // unlocks RequestLock and gives access to longest waiting thread            
            RequestLock.unlock();
            Logger.warn("Request to Minter Finished, UNLOCKING MINTER");
        }
        // return to mint.jsp
        return "mint";
    }

    /**
     * Maps to the admin panel on the home page.
     *
     * @return name of the index page
     */
    @RequestMapping(value = {""},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public ModelAndView displayIndex() {
        ModelAndView model = new ModelAndView();

        Logger.info("index page called");
        model.addObject("prepend", Settings.getPrepend());
        model.addObject("prefix", Settings.getPrefix());
        model.addObject("charMap", Settings.getCharMap());
        model.addObject("tokenType", Settings.getTokenType());
        model.addObject("rootLength", Settings.getRootLength());
        model.addObject("isAuto", Settings.isAuto());
        model.addObject("isRandom", Settings.isRandom());
        model.addObject("sansVowel", Settings.isSansVowels());
        model.setViewName("settings");

        return model;
    }

    /**
     * Because the minter construction checks the parameters for validity, this
     * method not only instantiates an AutoMinter but also checks whether or not
     * the requested amount of ids is valid.
     *
     * @param tempSettings The parameters of the minter given by the rest end
     * point and default values.
     * @param requestedAmount The amount of ids requested.
     * @return an AutoMinter
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    private Minter createAutoMinter(long requestedAmount, CachedSettings tempSettings)
            throws BadParameterException {
        Minter minter = new Minter(DatabaseManager,
                tempSettings.getPrepend(),
                tempSettings.getTokenType(),
                tempSettings.getRootLength(),
                tempSettings.getPrefix(),
                tempSettings.isSansVowels());

        if (minter.isValidAmount(requestedAmount)) {
            return minter;
        } else {
            throw new BadParameterException(requestedAmount, "Requested Amount");
        }
    }

    /**
     * Overrides the default value of cached value with values given in the
     * parameter. If the parameters do not contain any of the valid parameters,
     * the default values are maintained.
     *
     * @param parameters List of parameters given by the client.
     * @return The settings used for the particular session it was called.
     * @throws BadParameterException
     */
    private CachedSettings overrideDefaults(Map<String, String> parameters) throws BadParameterException {
        CachedSettings tempSetting = new CachedSettings(Settings);
        if (parameters.containsKey("prepend")) {
            tempSetting.Prepend = parameters.get("prepend");
        }
        if (parameters.containsKey("prefix")) {
            tempSetting.Prefix = (parameters.get("prefix"));
        }
        if (parameters.containsKey("rootLength")) {
            tempSetting.RootLength = (Integer.parseInt(parameters.get("rootLength")));
        }
        if (parameters.containsKey("charMap")) {
            tempSetting.CharMap = (parameters.get("charMap"));
        }
        if (parameters.containsKey("tokenType")) {
            tempSetting.TokenType = getValidTokenType(parameters.get("tokenType"));
        }
        if (parameters.containsKey("auto")) {
            tempSetting.Auto = convertBoolean(parameters.get("auto"), "auto");
        }
        if (parameters.containsKey("random")) {
            tempSetting.Random = convertBoolean(parameters.get("random"),"random");
        }
        if (parameters.containsKey("sansVowels")) {
            tempSetting.SansVowels = convertBoolean(parameters.get("sansVowels"), "sansVowels");
        }

        return tempSetting;
    }

    /**
     * Because the minter construction checks the parameters for validity, this
     * method not only instantiates a CustomMinter but also checks whether or
     * not the requested amount of ids is valid.
     *
     * @param tempSettings The parameters of the minter given by the rest end
     * point and default values.
     * @param requestedAmount The amount of ids requested.
     * @return a CustomMinter
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    private Minter createCustomMinter(long requestedAmount, CachedSettings tempSettings)
            throws BadParameterException {
        Minter minter = new Minter(DatabaseManager,
                tempSettings.getPrepend(),
                tempSettings.getCharMap(),
                tempSettings.getPrefix(),
                tempSettings.isSansVowels());

        if (minter.isValidAmount(requestedAmount)) {
            return minter;
        } else {
            Logger.error("Request amount of " + requestedAmount + " IDs is unavailable");
            throw new BadParameterException(requestedAmount, "Requested Amount");
        }
    }
    
    /**
     * This method is used to check to see whether or not the given parameter is explicitly
     * equivalent to "true" or "false" and returns them respectively. The method
     * provided by the Boolean wrapper class converts all Strings that do no explictly
     * contain true to false. 
     * @param parameter the given string to convert.
     * @param parameterType the type of the parameter.
     * @throws BadParameterException Thrown whenever a malformed parameter is formed or passed
     * @return the equivalent version of true or false. 
     */
    private boolean convertBoolean(String parameter, String parameterType) 
            throws BadParameterException{
        if(parameter.equals("true")){
            return true;
        }else if(parameter.equals("false")){
            return false;
        }else{
            throw new BadParameterException(parameter, parameterType);
        }
    }

    /**
     * Returns a view that displays the error message of
     * NotEnoughPermutationsException.
     *
     * @param req The HTTP request.
     * @param exception NotEnoughPermutationsException.
     * @return The view of the error message in json format.
     */
    @ExceptionHandler(NotEnoughPermutationsException.class)
    public ModelAndView handlePermutationError(HttpServletRequest req, Exception exception) {
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 400);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());
        Logger.error("Error with permutation: " + exception.getMessage());
        mav.setViewName("error");
        return mav;

    }

    /**
     * Returns a view that displays the error message of BadParameterException.
     *
     * @param req The HTTP request.
     * @param exception BadParameterException.
     * @return The view of the error message in json format.
     */
    @ExceptionHandler(BadParameterException.class)
    public ModelAndView handleBadParameterError(HttpServletRequest req, Exception exception) {
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 400);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());
        Logger.error("Error with bad parameter: " + exception.getMessage());

        mav.setViewName("error");
        return mav;
    }

    /**
     * Throws any exception that may be caught within the program
     *
     * @param req the HTTP request
     * @param exception the caught exception
     * @return The view of the error message
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneralError(HttpServletRequest req, Exception exception) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 500);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());
        Logger.error("Error"
                + "General Error: " + exception.getMessage());

        mav.setViewName("error");
        return mav;
    }

    /**
     * Gets the current length of the queue of RequestLock
     *
     * @return length of the queue
     */
    public int getRequestLockQueueLength() {
        return this.RequestLock.getQueueLength();
    }

    /**
     * Creates a Json object based off a list of ids given in the parameter
     *
     * @param list A list of ids to display into JSON
     * @param prepend A value to attach to the beginning of every id. Typically
     * used to determine the format of the id. For example, ARK or DOI.
     * @return A reference to a String that contains Json list of ids
     * @throws IOException thrown whenever a file could not be found
     */
    public String convertListToJson(Set<Id> list, String prepend) throws IOException {
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;

        // Object used to iterate through list of ids
        Iterator<Id> iterator = list.iterator();
        for (int i = 0; iterator.hasNext(); i++) {

            // Creates desired JSON format and adds the prepended string to be displayed
            String id = String.format("{\"id\":%d,\"name\":\"%s%s\"}",
                    i, prepend, iterator.next());

            formattedJson = mapper.readValue(id, Object.class);

            // append formatted json
            jsonString += mapper.writerWithDefaultPrettyPrinter().
                    writeValueAsString(formattedJson) + "\n";
        }

        return jsonString;
    }

    /**
     * Attempts to convert a string into an equivalent enum TokenType.
     *
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @return Returns the enum type if succesful, throws BadParameterException
     * otherwise.
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    public final TokenType getValidTokenType(String tokenType) throws BadParameterException {

        switch (tokenType) {
            case "DIGIT":
                return TokenType.DIGIT;
            case "LOWERCASE":
                return TokenType.LOWERCASE;
            case "UPPERCASE":
                return TokenType.UPPERCASE;
            case "MIXEDCASE":
                return TokenType.MIXEDCASE;
            case "LOWER_EXTENDED":
                return TokenType.LOWER_EXTENDED;
            case "UPPER_EXTENDED":
                return TokenType.UPPER_EXTENDED;
            case "MIXED_EXTENDED":
                return TokenType.MIXED_EXTENDED;
            default:
                throw new BadParameterException(tokenType, "TokenType");
        }
    }

    /**
     * A class used to store the minter settings of a given session.
     */
    private class CachedSettings {

        // Fields; detailed description in Minter class
        private String Prepend;
        private String Prefix;
        private TokenType TokenType;
        private String CharMap;
        private int RootLength;
        private boolean SansVowels;
        private boolean Auto;
        private boolean Random;

        /**
         * Copy constructor
         *
         * @param s the CachedSetting to copy
         */
        public CachedSettings(CachedSettings s) {
            Prepend = s.getPrepend();
            Prefix = s.getPrefix();
            TokenType = s.getTokenType();
            CharMap = s.getCharMap();
            RootLength = s.getRootLength();
            SansVowels = s.isSansVowels();
            Auto = s.isAuto();
            Random = s.isRandom();
        }

        /**
         * Default constructor
         */
        public CachedSettings() {
        }

        /**
         * Retrieves the default settings stored in a database
         *
         * @throws SQLException thrown whenever there is an error with the
         * database.
         * @throws BadParameterException thrown whenever a malformed or invalid
         * parameter is passed
         */
        public void retrieveSettings() throws SQLException, BadParameterException {

            Prepend = (String) DatabaseManager.retrieveSetting(DatabaseManager.getPREPEND_COLUMN());
            Prefix = (String) DatabaseManager.retrieveSetting(DatabaseManager.getPREFIX_COLUMN());

            if (Prepend == null) {
                Prepend = "";
            }
            if (Prefix == null) {
                Prefix = "";
            }

            TokenType = getValidTokenType((String) DatabaseManager.retrieveSetting(
                    DatabaseManager.getTOKEN_TYPE_COLUMN()));
            CharMap = (String) DatabaseManager.retrieveSetting(DatabaseManager.getCHAR_MAP_COLUMN());
            RootLength = (int) DatabaseManager.retrieveSetting(DatabaseManager.getROOT_LENGTH_COLUMN());

            int autoFlag = (int) DatabaseManager.retrieveSetting(DatabaseManager.getAUTO_COLUMN());
            int randomFlag = (int) DatabaseManager.retrieveSetting(DatabaseManager.getRANDOM_COLUMN());
            int vowelFlag = (int) DatabaseManager.retrieveSetting(DatabaseManager.getSANS_VOWEL_COLUMN());

            SansVowels = (vowelFlag == 1);
            Auto = (autoFlag == 1);
            Random = (randomFlag == 1);

        }

        @Override
        public String toString() {
            return String.format("prepend=%s\tprefix=%s\ttokenType=%s\tlength=%d\tcharMap=%s"
                    + "\tauto=%b\trandom=%b\tsans%b",
                    Prepend, Prefix, TokenType, RootLength, CharMap, Auto, Random, SansVowels);
        }

        // Typical getters and setters
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

        public TokenType getTokenType() {
            return TokenType;
        }

        public void setTokenType(TokenType TokenType) {
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

        public boolean isSansVowels() {
            return SansVowels;
        }

        public void setSansVowels(boolean SansVowels) {
            this.SansVowels = SansVowels;
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

    }
}
