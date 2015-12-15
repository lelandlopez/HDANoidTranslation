package test.hida;

import com.hida.BadParameterException;
import com.hida.DatabaseManager;
import com.hida.Id;
import com.hida.Minter;
import com.hida.NotEnoughPermutationsException;
import com.hida.TokenType;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import java.util.Scanner;
import java.util.Set;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;


/**
 *
 * @author lruffin
 */
public class MinterTest implements Comparator<String> {

    /**
     * Name of the database
     */
    static String DbName = "testPID";

    /**
     * Creates a new DatabaseManager completely separate from the actual
     * service.
     */
    DatabaseManager DatabaseManager = new DatabaseManager("", DbName);

    public MinterTest() {
    }

    /**
     * Used to test the functionality of the AutoMinter
     *
     * @return
     */
    @DataProvider(name = "autoMinter parameters")
    public static Object[][] AutoMinterParameters() {
        Random rng = new Random();
        return new Object[][]{
            {rng.nextInt(90) + 10, TokenType.DIGIT},
            {rng.nextInt(90) + 10, TokenType.LOWERCASE},
            {rng.nextInt(90) + 10, TokenType.UPPERCASE},
            {rng.nextInt(90) + 10, TokenType.MIXEDCASE},
            {rng.nextInt(90) + 10, TokenType.LOWER_EXTENDED},
            {rng.nextInt(90) + 10, TokenType.UPPER_EXTENDED},
            {rng.nextInt(90) + 10, TokenType.MIXED_EXTENDED}
        };
    }

    /**
     * Used to test functionality of CustomMinters
     *
     * @return
     */
    @DataProvider(name = "customMinter parameters")
    public static Object[][] CustomMinterParameters() {
        Random rng = new Random();
        return new Object[][]{
            {rng.nextInt(90) + 10, "ddddd", TokenType.DIGIT},
            {rng.nextInt(90) + 10, "lllll", TokenType.LOWERCASE},
            {rng.nextInt(90) + 10, "uuuuu", TokenType.UPPERCASE},
            {rng.nextInt(90) + 10, "lmmmu", TokenType.MIXEDCASE},
            {rng.nextInt(90) + 10, "lulul", TokenType.MIXEDCASE},
            {rng.nextInt(90) + 10, "ldldl", TokenType.LOWER_EXTENDED},
            {rng.nextInt(90) + 10, "ududu", TokenType.UPPER_EXTENDED},
            {rng.nextInt(90) + 10, "uedel", TokenType.MIXED_EXTENDED},
            {rng.nextInt(90) + 10, "ldmdu", TokenType.MIXED_EXTENDED},
            {rng.nextInt(90) + 10, "ldudl", TokenType.MIXED_EXTENDED}
        };
    }

    /**
     * Used to test whether or not NotEnoughPermutationsException is properly
     * thrown
     *
     * @return
     */
    @DataProvider(name = "overlap parameters")
    public static Object[][] FormatOverlapParameters() {
        return new Object[][]{
            {100, TokenType.DIGIT, true, 2},
            {900, TokenType.LOWER_EXTENDED, true, 2},
            {900, TokenType.UPPER_EXTENDED, true, 2},
            {2500, TokenType.MIXED_EXTENDED, true, 2}
        };
    }

    /**
     * Method that is used to test whether or not the format of an id is stored
     * in a database.
     *
     * @return a list of formats to test.
     */
    @DataProvider(name = "format parameters")
    public static Object[][] FormatParameters() {
        Random rng = new Random();
        return new Object[][]{
            {rng.nextInt(90) + 10, "1", TokenType.DIGIT, true, 5},
            {rng.nextInt(90) + 10, "2", TokenType.LOWER_EXTENDED, true, 5},
            {rng.nextInt(90) + 10, "3", TokenType.UPPER_EXTENDED, true, 5},
            {rng.nextInt(90) + 10, "4", TokenType.MIXED_EXTENDED, true, 5},
            {rng.nextInt(90) + 10, "5", TokenType.LOWERCASE, true, 5},
            {rng.nextInt(90) + 10, "6", TokenType.UPPERCASE, true, 5},
            {rng.nextInt(90) + 10, "7", TokenType.MIXEDCASE, true, 5},
            {rng.nextInt(90) + 10, "1", TokenType.DIGIT, false, 5},
            {rng.nextInt(90) + 10, "2", TokenType.LOWER_EXTENDED, false, 5},
            {rng.nextInt(90) + 10, "3", TokenType.UPPER_EXTENDED, false, 5},
            {rng.nextInt(90) + 10, "4", TokenType.MIXED_EXTENDED, false, 5},
            {rng.nextInt(90) + 10, "5", TokenType.LOWERCASE, false, 5},
            {rng.nextInt(90) + 10, "6", TokenType.UPPERCASE, false, 5},
            {rng.nextInt(90) + 10, "7", TokenType.MIXEDCASE, false, 5}
        };
    }
    
     /**
     * Returns a set of parameters for AutoMinter that should throw a
     * BadParameterException
     *
     * @return
     */
    @DataProvider(name = "bad parameter auto")
    public static Object[][] BadParametersAutoMinter() {
        return new Object[][]{
            {-10, "prefix", TokenType.DIGIT, 10, true},
            {100, "!prefix", TokenType.DIGIT, 10, true},
            {100, String.format("%21s", ""), TokenType.DIGIT, 10, true},
            {100, "prefix", TokenType.DIGIT, -10, true},
            {100, "prefix", TokenType.DIGIT, 11, true}
        };
    }

    /**
     * Returns a set of parameters for AutoMinter that should throw a
     * BadParameterException
     *
     * @return
     */
    @DataProvider(name = "bad parameter custom")
    public static Object[][] BadParametersCustomMinter() {
        return new Object[][]{
            {-110, "prefix", "dlume", true},
            {100, "&prefix", "dlume", true},
            {100, String.format("%21s", ""), "dlume", true},
            {100, "prefix", "dlumea", true}

        };
    }


    /**
     * Tests the AutoMinter to see if its capable of producing unique ids.
     *
     *
     * @param expectedAmount The requested amount of ids
     * @param tokenType The tokenType used to format the ids
     */
    @Test(dataProvider = "autoMinter parameters")
    public void testUniqueIdRandomAutoMinter(int expectedAmount, TokenType tokenType) {
        try {
            // produce the ids using the given format
            int rootLength = 5;

            String prefix = "";
            //Set<String> set = new HashSet<>();
            Set<Id> set = new HashSet<>();

            Minter AutoMinter
                    = new Minter(DatabaseManager, "/:ark/NAAN/", tokenType, rootLength, prefix, false);

            
            DatabaseManager.getPermutations(prefix, tokenType, rootLength, false);
            set = AutoMinter.genIdAutoRandom(expectedAmount);

            /*
            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.nextLine();

                // parse the json for the name parameter. The prepend and the name identifier
                // are not filtered out because they will not affect uniqueness
                if (!id.matches("(^[{]$)|(^.*\"id\".*.*$)|(^[}]$)")) {
                    //System.out.println(id);
                    // add id to the set to prove uniqueness
                    set.add(id);
                }
            }
            */
            
            // if the amount of ids produced is not the same as the number of ids in a set, fail
            Assert.assertEquals(set.size(), expectedAmount);

            // if any exceptions are caught, fail
        } catch (SQLException | IOException | BadParameterException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }

    /**
     * Tests the minter to see if the AutoMinter can create ids in ascending
     * order in a given format.
     *
     * @param expectedAmount The amount of ids requested.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     */
    @Test(dataProvider = "autoMinter parameters")
    public void testUniqueIdAutoSequentialMinter(int expectedAmount, TokenType tokenType) {
        try {

            // produce the ids using the given format
            int rootLength = 5;

            String prefix = "";
            //Set<String> set = new LinkedHashSet<>();
            Set<Id> set = new LinkedHashSet<>();

            Minter AutoMinter
                    = new Minter(DatabaseManager, "/:ark/NAAN/", tokenType, rootLength, prefix, false);

            DatabaseManager.getPermutations(prefix, tokenType, rootLength, false);
            set = AutoMinter.genIdAutoSequential(expectedAmount);

            /*
            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.nextLine();
                if (!id.matches("(^[{]$)|(^.*\"id\".*.*$)|(^[}]$)")) {
                    //System.out.println(id);
                    // add id to the set to prove uniqueness
                    set.add(id);
                }
            }
            // if the amount of ids produced is not the same as the number of ids in a set, fail
            */
            Assert.assertEquals(set.size(), expectedAmount);
            Iterator<Id> iter = set.iterator();
            //Iterator<String> iter = set.iterator();
            String prev = iter.next().toString();
            while (iter.hasNext()) {
                String current = iter.next().toString();
                // if the previous id has an equal or greater value than the current id, 
                // fail the case
                if (compare(prev, current) > -1) {
                    Assert.fail(String.format("The ids are not sequential: prev=%s\tcurrent=%s",
                            prev, current));
                }
                prev = current;
            }
            // if any exceptions are caught, fail
        } catch (SQLException | IOException | BadParameterException | NotEnoughPermutationsException exception) {
            System.out.println("");
            Assert.fail(exception.getMessage() + "\nexpectedAmount = " + expectedAmount + "\ttokenType = " + tokenType, exception);
            
        }
    }

    /**
     * Tests the CustomRandomMinter to see if unique ids are produced
     *
     * @param expectedAmount The amount of ids requested.
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     */
    @Test(dataProvider = "customMinter parameters")
    public void testUniqueIdRandomCustomMinter(int expectedAmount, String charMap,
            TokenType tokenType) {
        try {
            // produce the ids using the given format
            String prefix = "";
            //Set<String> set = new HashSet<>();
            Set<Id> set = new HashSet<>();

            Minter CustomMinter = new Minter(DatabaseManager, "", charMap, prefix, false);
            DatabaseManager.getPermutations(prefix, false, charMap, CustomMinter.getTokenType());
            set = CustomMinter.genIdCustomRandom(expectedAmount);

            /*
            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.next().trim();
                // parse the json for the name parameter. The prepend and the name identifier
                // are not filtered out because they will not affect uniqueness
                if (!id.matches("(^[{]$)|(^.*\"id\".*$)|(^[}]$)|(\"name\")|(:)|(\\d.*,)")) {

                    Assert.assertEquals(CustomMinter.getTokenType(), tokenType);

                    //System.out.println(id);
                    // add id to the set to prove uniqueness
                    set.add(id);
                }
            }
            */
            // if the amount of ids produced is not the same as the number of ids in a set, fail
            Assert.assertEquals(set.size(), expectedAmount);

            // if any exceptions are caught, fail
        } catch (SQLException | IOException | BadParameterException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }

    /**
     * Tests whether or not unique values are produced in ascending order by the
     * Custom Sequential Minter.
     *
     * @param expectedAmount The amount of ids requested.
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     */
    @Test(dataProvider = "customMinter parameters")
    public void testUniqueIdSequentialCustomMinter(int expectedAmount, String charMap,
            TokenType tokenType) {
        try {
            // produce the ids using the given format
            String prefix = "";
            //Set<String> set = new LinkedHashSet<>();
            Set<Id> set = new LinkedHashSet<>();
            
            Minter CustomMinter = new Minter(DatabaseManager, "", charMap, prefix, false);
            DatabaseManager.getPermutations(prefix, false, charMap, CustomMinter.getTokenType());
            set = CustomMinter.genIdCustomSequential(expectedAmount);

            /*
            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.next().trim();
                // parse the json for the name parameter. The prepend and the name identifier
                // are not filtered out because they will not affect uniqueness
                if (!id.matches("(^[{]$)|(^.*\"id\".*$)|(^[}]$)|(\"name\")|(:)|(\\d.*,)")) {

                    System.out.println("expected: " + tokenType);
                    System.out.println("getToken: " + CustomMinter.getTokenType());
                    Assert.assertEquals(CustomMinter.getTokenType(), tokenType);


                    // add id to the set to prove uniqueness
                    set.add(id);
                }
            }
            */
            // if the amount of ids produced is not the same as the number of ids in a set, fail
            Assert.assertEquals(set.size(), expectedAmount);
            Iterator<Id> iter = set.iterator();
            String prev = iter.next().toString();
            while (iter.hasNext()) {
                String current = iter.next().toString();
                // if the previous id has a higher value than the current id, fail the case
                if (compare(prev, current) > -1) {
                    Assert.fail(
                            "The ids are not sequential: prev=" + prev + "\tcurrent=" + current);
                }
                prev = current;
            }

            // if any exceptions are caught, fail
        } catch (SQLException | IOException | BadParameterException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }

    /**
     * Populates the database with a digit format so that
     * NotEnoughPermutationsException can be tested.
     */
    @Test
    public void populateAutoDigitFormat() {
        try {
            Minter AutoMinter;
            String prefix = "";

            DatabaseManager.getPermutations(prefix, TokenType.DIGIT, 2, true);
            AutoMinter
                    = new Minter(DatabaseManager, prefix, TokenType.DIGIT, 2, prefix, true);

            AutoMinter.genIdAutoRandom(100);
        } catch (Exception exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }

    /**
     * This tests to see if NotEnoughPermutationsException is properly thrown
     * whenever appropriate.
     *
     * <pre>
     * The exception is thrown whenever:
     * - the amount requested exceeds the number of possible permutations.
     * - the amount of unique ids produced does not meet the amount requested .
     * </pre>
     *
     * If another exception is thrown the test fails.
     *
     * @param expectedAmount The amount of ids requested.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     */
    @Test(dataProvider = "overlap parameters",
            expectedExceptions = NotEnoughPermutationsException.class,
            dependsOnMethods = "populateAutoDigitFormat")
    public void formatOverlap(int expectedAmount, TokenType tokenType, boolean sansVowel,
            int rootLength) {
        System.out.println("inside formatOverlap");
        try {

            String prefix = "";

            DatabaseManager.getPermutations(prefix, tokenType, rootLength, sansVowel);
            Minter AutoMinter
                    = new Minter(DatabaseManager, prefix, tokenType, rootLength, prefix, sansVowel);

            AutoMinter.genIdAutoRandom(expectedAmount);

            DatabaseManager.printFormat();

        } catch (SQLException | BadParameterException | IOException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }

    /**
     * Tests the service to see if a format corresponding to a mint request is
     * stored in the format table.
     *
     * @param expectedAmount The amount of ids requested.
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @param rootLength Designates the length of the id's root.
     */
    @Test(dataProvider = "format parameters")
    public void testFormat(int expectedAmount, String prefix, TokenType tokenType,
            boolean sansVowel, int rootLength) {
        try {
            DatabaseManager.getPermutations(prefix, tokenType, rootLength, sansVowel);
            Minter AutoMinter
                    = new Minter(DatabaseManager, "", tokenType, rootLength, prefix, sansVowel);

            AutoMinter.genIdAutoRandom(expectedAmount);

            if (!DatabaseManager.formatExists(prefix, tokenType, sansVowel, rootLength)) {
                Assert.fail("format does not exist");
            }

        } catch (Exception exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }

   
    /**
     *
     * @param amount The amount of ids requested.
     * @param prefix The string that will be at the front of every id.
     * @param tokenType Designates what characters are contained in the id's
     * root.
     * @param rootLength Designates the length of the id's root.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     * @throws Exception
     */
    @Test(dataProvider = "bad parameter auto", expectedExceptions = BadParameterException.class)
    public void testBadParameterExceptionAutoMinter(long amount, String prefix, 
            TokenType tokenType, int rootLength, boolean sansVowel) throws Exception {
        Minter AutoMinter
                = new Minter(DatabaseManager, "", tokenType, rootLength, prefix, sansVowel);
        if (!AutoMinter.isValidAmount(amount)) {
            throw new BadParameterException(amount, "Requested Amount");
        }
    }

    /**
     *
     * @param amount The amount of ids requested.
     * @param prefix The string that will be at the front of every id.
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits.
     * @param sansVowel Designates whether or not the id's root contains vowels.
     */
    @Test(dataProvider = "bad parameter custom", expectedExceptions = BadParameterException.class)
    public void testBadParameterExceptionCustomMinter(long amount, String prefix, String charMap,
            boolean sansVowel) throws Exception {
        Minter CustomMinter
                = new Minter(DatabaseManager, "", charMap, prefix, sansVowel);        
        if (!CustomMinter.isValidAmount(amount)) {
            throw new BadParameterException(amount, "Requested Amount");
        }
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        File db = new File(DbName);
        System.out.println(db.getAbsolutePath());
        if (db.exists()) {
            System.out.print("set up classfound test database; deleting..."+db.delete());
            //db.delete();
            System.out.print("done\n");
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File db = new File(DbName);
        if (db.exists()) {
            System.out.print("tear down class found test database; deleting..."+db.delete());
            //db.delete();
            System.out.print("done\n");
        }
    }

    @BeforeTest
    public void setUpTest() throws Exception {
        DatabaseManager.createConnection();
        File db = new File(DbName);
        System.out.println(db.getAbsolutePath());
        if (db.exists()) {
            System.out.print("set up testfound test database; deleting..."+db.delete());
            //db.delete();
            System.out.print("done\n");
        }

    }
    
    
    @AfterTest
    public void tearDownTest() throws Exception {
        DatabaseManager.closeConnection();
        File db = new File(DbName);
        System.out.println(db.getAbsolutePath());
        if (db.exists()) {
            System.out.print("tear down test found test database; deleting..."+db.delete());
            //db.delete();
            
            System.out.print("done\n");
        }
    }

    /**
     * Used to compare to ids. If the first id has a smaller value than the
     * second id, -1 is returned. If they are equal, 0 is returned. Otherwise 1
     * is returned. In terms of value, each character has a unique value
     * associated with them. Numbers are valued less than lowercase letters,
     * which are valued less than upper case letters.
     *
     * The least and greatest valued number is 0 and 9 respectively. The least
     * and greatest valued lowercase letter is a and z respectively. The least
     * and greatest valued uppercase letter is A and Z respectively.
     *
     * @param id1 the first id
     * @param id2 the second id
     * @return result of the comparison.
     */
    @Override
    public int compare(String id1, String id2) {
        if (id1.length() < id2.length()) {
            return -1;
        } else if (id1.length() > id2.length()) {
            return 1;
        } else {
            for (int i = 0; i < id1.length(); i++) {
                char c1 = id1.charAt(i);
                char c2 = id2.charAt(i);
                if (Character.isDigit(c1) && Character.isLetter(c2)
                        || Character.isLowerCase(c1) && Character.isUpperCase(c2)
                        || c1 < c2) {
                    return -1;
                } else if ((Character.isLetter(c1) && Character.isDigit(c2))
                        || Character.isUpperCase(c1) && Character.isLowerCase(c2)
                        || c1 > c2) {
                    return 1;
                }
            }
            return 0;
        }
    }
}
