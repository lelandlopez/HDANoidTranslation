package com.hida;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.io.PrintWriter;
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

    /**
     * Creates a fair reentrant RequestLock to serialize each request
     * sequentially instead of concurrently. pids
     */
    private static final ReentrantLock RequestLock = new ReentrantLock(true);

    /**
     * Logger; logfile to be stored in resource folder
     */
    private static final Logger Logger = LoggerFactory.getLogger(MinterController.class);

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
        String name = properties.getProperty("databaseName");

        // Creates the database at a location specified by the properties file
        if (!name.isEmpty()) {
            if (!path.endsWith("\\")) {
                path += "\\";
            }
            DatabaseManager = new DatabaseManager(path, name);
        } else {
            DatabaseManager = new DatabaseManager();
        }
        // create connection and sets up the database if need be
        DatabaseManager.createConnection();
        
        // retrieve the default settings from the database and store it in Settings
        Settings = new CachedSettings();
        Settings.retrieveSettings();
        
        DatabaseManager.closeConnection();
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
        Logger.info("hello1");

        // message variable to be sent to mint.jsp
        String message;
        try {
            System.out.print("retrieving data...");
            // retrieve default setting
            //MinterParameter minterParameter = new MinterParameter(parameters);

            // create connection and sets up the database if need be
            DatabaseManager.createConnection();

            // instantiate the correct minter and calculate remaining number of permutations
            long remainingPermutations;
            Minter minter;
            if (Settings.isAuto()) {
            //if (minterParameter.isAuto()) {
                //minter = createAutoMinter(minterParameter, requestedAmount);
                minter = createAutoMinter(Settings, requestedAmount);

                remainingPermutations
                        /*= DatabaseManager.getPermutations(minterParameter.getPrefix(),
                                minterParameter.getTokenType(),
                                minterParameter.getRootLength(),
                                minterParameter.isSansVowels());*/
                        = DatabaseManager.getPermutations(Settings.getPrefix(),
                                Settings.getTokenType(),
                                Settings.getRootLength(),
                                Settings.isSansVowels());
            } else {
                //minter = createCustomMinter(minterParameter, requestedAmount);
                minter = createCustomMinter(Settings, requestedAmount);

                remainingPermutations
                        /*= DatabaseManager.getPermutations(minterParameter.getPrefix(),
                                minterParameter.isSansVowels(),
                                minterParameter.getCharMap(),
                                minterParameter.getTokenType());*/
                        = DatabaseManager.getPermutations(Settings.getPrefix(),
                                Settings.isSansVowels(),
                                Settings.getCharMap(),
                                Settings.getTokenType());
            }

            // throw an exception if the requested amount of ids can't be generated
            if (remainingPermutations < requestedAmount) {
                throw new NotEnoughPermutationsException(remainingPermutations, requestedAmount);
            }
            Set<Id> idList;
            // have the minter create the ids and assign it to message
            if (Settings.isAuto()) {
            //if (minterParameter.isAuto()) {
                if (Settings.isRandom()) {
                //if (minterParameter.isRandom()) {
                    System.out.println("making autoRandom");
                    idList = minter.genIdAutoRandom(requestedAmount);
                } else {
                    System.out.println("making autoSequential");
                    idList = minter.genIdAutoSequential(requestedAmount);
                }
            } else {
                if (Settings.isRandom()) {
                //if (minterParameter.isRandom()) {
                    System.out.println("making customRandom");
                    idList = minter.genIdCustomRandom(requestedAmount);
                } else {
                    System.out.println("making customSequential");
                    idList = minter.genIdCustomSequential(requestedAmount);
                }
            }

            message = convertListToJson(idList, Settings.getPrepend());
            //message = convertListToJson(idList, minterParameter.getPrepend());

            // print list of ids to screen
            model.addAttribute("message", message);

            // close the connection
            minter.getDatabaseManager().closeConnection();
        } finally {
            // unlocks RequestLock and gives access to longest waiting thread            
            RequestLock.unlock();
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
        return "settings";
    }

    /**
     *
     * @param request
     * @param response
     * @return
     */
    //@RequestMapping(value = {"/confirmation"},
    //      method = {org.springframework.web.bind.annotation.RequestMethod.GET})    
    public String handleForm(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String prepend = request.getParameter("prepend");

        // get response writer
        PrintWriter writer = response.getWriter();
        return "confirmation";
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
            throw new BadParameterException(requestedAmount, "Requested Amount");
        }
    }
    
    /**
     * Because the minter construction checks the parameters for validity, this
     * method not only instantiates an AutoMinter but also checks whether or not
     * the requested amount of ids is valid.
     *
     * @param settings The parameters of the minter given by the rest end
     * point and default values.
     * @param requestedAmount The amount of ids requested.
     * @return an AutoMinter
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    private Minter createAutoMinter(CachedSettings settings, long requestedAmount)
            throws BadParameterException {
        Minter minter = new Minter(DatabaseManager,
                settings.getPrepend(),
                settings.getTokenType(),
                settings.getRootLength(),
                settings.getPrefix(),
                settings.isSansVowels());

        if (minter.isValidAmount(requestedAmount)) {
            return minter;
        } else {
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
            throw new BadParameterException(requestedAmount, "Requested Amount");
        }
    }
    
    /**
     * Because the minter construction checks the parameters for validity, this
     * method not only instantiates a CustomMinter but also checks whether or
     * not the requested amount of ids is valid.
     *
     * @param settings The parameters of the minter given by the rest end
     * point and default values.
     * @param requestedAmount The amount of ids requested.
     * @return a CustomMinter
     * @throws BadParameterException thrown whenever a malformed or invalid
     * parameter is passed
     */
    private Minter createCustomMinter(CachedSettings settings, long requestedAmount)
            throws BadParameterException {
        Minter minter = new Minter(DatabaseManager,
                settings.getPrepend(),
                settings.getCharMap(),
                settings.getPrefix(),
                settings.isSansVowels());

        if (minter.isValidAmount(requestedAmount)) {
            return minter;
        } else {
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
        //logger.error("Request: " + req.getRequestURL() + " raised " + exception);

        ModelAndView mav = new ModelAndView();
        mav.addObject("status", 500);
        mav.addObject("exception", exception.getClass().getSimpleName());
        mav.addObject("message", exception.getMessage());

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
     * Creates a Json object based off a list of ids given in the parameter
     *
     * @param list A list of ids to display into JSON
     * @param prepend A value to attach to the beginning of every id. Typically
     * used to determine the format of the id. For example, ARK or DOI.
     * @return A reference to a String that contains Json list of ids
     * @throws IOException
     */
    public String convertListToJson(Set<Id> list, String prepend) throws IOException {
        // Jackson objects to create formatted Json string
        String jsonString = "";
        ObjectMapper mapper = new ObjectMapper();
        Object formattedJson;

        // Object used to iterate through list of ids
        Iterator<Id> iterator = list.iterator();
        for (int i = 0; iterator.hasNext(); i++) {

            // Creates desired JSON format. Also adds prepend to the string to be displayed
            // to the client
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
     *
     * @param tokenType
     * @return
     * @throws BadParameterException
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

    private class CachedSettings {

        // Fields; detailed description in Minter class

        private String Prepend;
        private String Prefix;
        private TokenType TokenType;
        private String CharMap;
        private int RootLength;
        private boolean SansVowels;
        // determines whether or not an auto minter or custom minter is created 
        private boolean Auto;

        // determines whether or not ids are randomly or sequentially created
        private boolean Random;

        /**
         * 
         * @throws SQLException
         * @throws BadParameterException 
         */
        public void retrieveSettings() throws SQLException, BadParameterException {
            System.out.println("in cached settings retrieve settings");
            Prepend = (String) DatabaseManager.retrieveSetting(DatabaseManager.getPREPEND_COLUMN());
            Prefix = (String) DatabaseManager.retrieveSetting(DatabaseManager.getPREFIX_COLUMN());
            
            if(Prepend == null){
                Prepend = "";
            }if(Prefix == null){
                Prefix = "";
            }
            
            TokenType = getValidTokenType((String) DatabaseManager.retrieveSetting(DatabaseManager.getTOKEN_TYPE_COLUMN()));
            CharMap = (String) DatabaseManager.retrieveSetting(DatabaseManager.getCHAR_MAP_COLUMN());
            RootLength = (int) DatabaseManager.retrieveSetting(DatabaseManager.getROOT_LENGTH_COLUMN());

            int autoFlag = (int)DatabaseManager.retrieveSetting(DatabaseManager.getAUTO_COLUMN());
            int randomFlag = (int)DatabaseManager.retrieveSetting(DatabaseManager.getRANDOM_COLUMN());
            int vowelFlag = (int)DatabaseManager.retrieveSetting(DatabaseManager.getSANS_VOWEL_COLUMN());
            
            SansVowels = (vowelFlag == 1);
            Auto = (autoFlag == 1);
            Random = (randomFlag == 1);
            
            System.out.println("prepend = " + Prepend);
            System.out.println("prefix = " + Prefix);
            System.out.println("TokenType = " + TokenType);
            System.out.println("charmap = " + CharMap);
            System.out.println("RootLength = " + RootLength);
            System.out.println("SansVowels = " + SansVowels);
            System.out.println("auto = " + Auto);
            System.out.println("random = " + Random);
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

    /**
     * A class used to store the parameters given via REST end-point /mint.
     */
    private class MinterParameter {

        // Fields; detailed description in Minter class
        private String Prepend;
        private String Prefix;
        private TokenType TokenType;
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
        private MinterParameter(Map<String, String> parameters) throws IOException,
                BadParameterException {
            retrieveDefaultSetting();
            setDefaultSettings(parameters);
        }

        /**
         * Retrieve default settings for minter from property file.
         */
        private void retrieveDefaultSetting() throws IOException, BadParameterException {
            // load property file
            Properties properties = new Properties();

            properties.load(Thread.currentThread().
                    getContextClassLoader().getResourceAsStream(CONFIG_FILE));

            // retrieve values found in minter_config.properties file                                
            this.Prepend = properties.getProperty("prepend");
            this.Prefix = properties.getProperty("prefix");
            this.CharMap = properties.getProperty("charMap");
            this.RootLength = Integer.parseInt(properties.getProperty("rootLength"));
            this.Auto = Boolean.parseBoolean(properties.getProperty("auto"));
            this.Random = Boolean.parseBoolean(properties.getProperty("random"));
            this.SansVowels = Boolean.parseBoolean(properties.getProperty("sansVowels"));
            this.TokenType = getValidTokenType(properties.getProperty("tokenType"));

            System.out.println("prop file path = " + Thread.currentThread().
                    getContextClassLoader().getResources(CONFIG_FILE));

        }

        /**
         * Method that sets the fields to any given parameters. Defaults to
         * values found in minter_config.properties file located in resources
         * folder.
         *
         * @param parameters list of given parameters.
         *
         */
        private void setDefaultSettings(Map<String, String> parameters)
                throws BadParameterException {

            if (parameters.containsKey("prepend")) {
                this.Prepend = parameters.get("prepend");
            }
            if (parameters.containsKey("prefix")) {
                this.Prefix = (parameters.get("prefix"));
            }
            if (parameters.containsKey("rootLength")) {
                this.RootLength = (Integer.parseInt(parameters.get("rootLength")));
            }
            if (parameters.containsKey("charMap")) {
                this.CharMap = (parameters.get("charMap"));
            }
            if (parameters.containsKey("tokenType")) {
                this.TokenType = getValidTokenType(parameters.get("tokenType"));
            }
            if (parameters.containsKey("auto")) {
                this.Auto = (Boolean.parseBoolean(parameters.get("auto")));
            }
            if (parameters.containsKey("random")) {
                this.Random = (Boolean.parseBoolean(parameters.get("random")));
            }
            if (parameters.containsKey("sansVowels")) {
                this.SansVowels = (Boolean.parseBoolean(parameters.get("sansVowels")));
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
