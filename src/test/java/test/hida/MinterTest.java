/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.hida;

import com.hida.BadParameterException;
import com.hida.DatabaseManager;
import com.hida.Minter;
import com.hida.NotEnoughPermutationsException;
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
            {rng.nextInt(90) + 10, "DIGIT"},
            {rng.nextInt(90) + 10, "LOWERCASE"},
            {rng.nextInt(90) + 10, "UPPERCASE"},
            {rng.nextInt(90) + 10, "MIXEDCASE"},
            {rng.nextInt(90) + 10, "LOWER_EXTENDED"},
            {rng.nextInt(90) + 10, "UPPER_EXTENDED"},
            {rng.nextInt(90) + 10, "MIXED_EXTENDED"}
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
            {rng.nextInt(90) + 10, "ddddd", "DIGIT"},
            {rng.nextInt(90) + 10, "lllll", "LOWERCASE"},
            {rng.nextInt(90) + 10, "uuuuu", "UPPERCASE"},
            {rng.nextInt(90) + 10, "lmmmu", "MIXEDCASE"},
            {rng.nextInt(90) + 10, "lulul", "MIXEDCASE"},
            {rng.nextInt(90) + 10, "ldldl", "LOWER_EXTENDED"},
            {rng.nextInt(90) + 10, "ududu", "UPPER_EXTENDED"},
            {rng.nextInt(90) + 10, "uedel", "MIXED_EXTENDED"},
            {rng.nextInt(90) + 10, "ldmdu", "MIXED_EXTENDED"},
            {rng.nextInt(90) + 10, "ldudl", "MIXED_EXTENDED"}
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
            {100, "DIGIT", true, 2},
            {900, "LOWER_EXTENDED", true, 2},
            {900, "UPPER_EXTENDED", true, 2},
            {2500, "MIXED_EXTENDED", true, 2}
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
            {rng.nextInt(90) + 10, "1", "DIGIT", true, 5},
            {rng.nextInt(90) + 10, "2", "LOWER_EXTENDED", true, 5},
            {rng.nextInt(90) + 10, "3", "UPPER_EXTENDED", true, 5},
            {rng.nextInt(90) + 10, "4", "MIXED_EXTENDED", true, 5},
            {rng.nextInt(90) + 10, "5", "LOWERCASE", true, 5},
            {rng.nextInt(90) + 10, "6", "UPPERCASE", true, 5},
            {rng.nextInt(90) + 10, "7", "MIXEDCASE", true, 5},
            {rng.nextInt(90) + 10, "1", "DIGIT", false, 5},
            {rng.nextInt(90) + 10, "2", "LOWER_EXTENDED", false, 5},
            {rng.nextInt(90) + 10, "3", "UPPER_EXTENDED", false, 5},
            {rng.nextInt(90) + 10, "4", "MIXED_EXTENDED", false, 5},
            {rng.nextInt(90) + 10, "5", "LOWERCASE", false, 5},
            {rng.nextInt(90) + 10, "6", "UPPERCASE", false, 5},
            {rng.nextInt(90) + 10, "7", "MIXEDCASE", false, 5}
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
            {-10, "prefix", "tokenType", 10, true},
            {100, "!prefix", "tokenType", 10, true},
            {100, String.format("%21s", ""), "tokenType", 10, true},
            {100, "prefix", "tokenType", -10, true},
            {100, "prefix", "tokenType", 11, true}
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
     * @param token The token used to format the ids
     */
    @Test(dataProvider = "autoMinter parameters")
    public void testUniqueIdRandomAutoMinter(int expectedAmount, String token) {
        try {
            // produce the ids using the given format
            int rootLength = 5;

            String prefix = "";
            Set<String> set = new HashSet<String>();

            Minter AutoMinter
                    = new Minter(DatabaseManager, "/:ark/NAAN/", token, rootLength, prefix, false);

            DatabaseManager.getPermutations(prefix, token, rootLength, false);
            String JSONIds = AutoMinter.genIdAutoRandom(expectedAmount);

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
    public void testUniqueIdAutoSequentialMinter(int expectedAmount, String tokenType) {
        try {

            // produce the ids using the given format
            int rootLength = 5;

            String prefix = "";
            Set<String> set = new LinkedHashSet<String>();

            Minter AutoMinter
                    = new Minter(DatabaseManager, "/:ark/NAAN/", tokenType, rootLength, prefix, false);

            DatabaseManager.getPermutations(prefix, tokenType, rootLength, false);
            String JSONIds = AutoMinter.genIdAutoSequential(expectedAmount);

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
            Assert.assertEquals(set.size(), expectedAmount);
            Iterator<String> iter = set.iterator();
            String prev = iter.next();
            while (iter.hasNext()) {
                String current = iter.next();
                // if the previous id has an equal or greater value than the current id, 
                // fail the case
                if (compare(prev, current) > -1) {
                    Assert.fail(String.format("The ids are not sequential: prev=%s\tcurrent=%s",
                            prev, current));
                }
                prev = current;
            }
            // if any exceptions are caught, fail
        } catch (SQLException | IOException | BadParameterException exception) {
            Assert.fail(exception.getMessage(), exception);
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
            String tokenType) {
        try {
            // produce the ids using the given format
            String prefix = "";
            Set<String> set = new HashSet<String>();

            Minter CustomMinter = new Minter(DatabaseManager, "", charMap, prefix, false);
            DatabaseManager.getPermutations(prefix, false, charMap, CustomMinter.getTokenType());
            String JSONIds = CustomMinter.genIdCustomRandom(expectedAmount);

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
            String tokenType) {
        try {
            // produce the ids using the given format
            String prefix = "";
            Set<String> set = new LinkedHashSet<String>();
            Minter CustomMinter = new Minter(DatabaseManager, "", charMap, prefix, false);
            DatabaseManager.getPermutations(prefix, false, charMap, CustomMinter.getTokenType());
            String JSONIds = CustomMinter.genIdCustomSequential(expectedAmount);

            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.next().trim();
                // parse the json for the name parameter. The prepend and the name identifier
                // are not filtered out because they will not affect uniqueness
                if (!id.matches("(^[{]$)|(^.*\"id\".*$)|(^[}]$)|(\"name\")|(:)|(\\d.*,)")) {

                    System.out.println("expected: " + tokenType);
                    System.out.println("getToken: " + CustomMinter.getTokenType());
                    Assert.assertEquals(CustomMinter.getTokenType(), tokenType);

                    //System.out.println(id);
                    // add id to the set to prove uniqueness
                    set.add(id);
                }
            }
            // if the amount of ids produced is not the same as the number of ids in a set, fail
            Assert.assertEquals(set.size(), expectedAmount);
            Iterator<String> iter = set.iterator();
            String prev = iter.next();
            while (iter.hasNext()) {
                String current = iter.next();
                // if the previous id has a higher value than the current id, fail the case
                if (compare(prev, current) > -1) {
                    Assert.fail("The ids are not sequential: prev=" + prev + "\tcurrent=" + current);
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

            DatabaseManager.getPermutations(prefix, "DIGIT", 2, true);
            AutoMinter
                    = new Minter(DatabaseManager, prefix, "DIGIT", 2, prefix, true);

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
    public void formatOverlap(int expectedAmount, String tokenType, boolean sansVowel,
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
    public void testFormat(int expectedAmount, String prefix, String tokenType,
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
    public void testBadParameterExceptionAutoMinter(long amount, String prefix, String tokenType,
            int rootLength, boolean sansVowel) throws Exception {
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

    @BeforeTest
    public static void setUpClass() throws Exception {
        File db = new File(DbName);
        System.out.println(db.getAbsolutePath());
        if (db.exists()) {
            System.out.print("found test database; deleting...");
            db.delete();
            System.out.print("done\n");
        }
    }

    @AfterTest
    public static void tearDownClass() throws Exception {
        File db = new File(DbName);
        if (db.exists()) {
            System.out.print("deleting test database...");
            db.delete();
            System.out.print("done\n");
        }
    }

    @BeforeTest
    public void setUpTest() throws Exception {
        DatabaseManager.createConnection();

    }

    @AfterTest
    public void tearDownTest() throws Exception {
        DatabaseManager.closeConnection();
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
