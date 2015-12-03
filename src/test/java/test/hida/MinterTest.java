/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.hida;

//import static org.testng.Assert.*;
import com.hida.DatabaseManager;
import com.hida.Minter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import org.testng.Assert;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
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

    static String DbName = "testPID";
    DatabaseManager DatabaseManager = new DatabaseManager("", DbName);

    

    public MinterTest() {
    }

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
    
    @DataProvider(name = "customMinter parameters")
    public static Object[][] CustomMinterParameters(){
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
     * Tests that unique values are being added to the database and displayed to
     * the client using the Set collection. The tests succeeds if the number of
     * ids requested is equal to the size of the set.
     *
     * Quotation marks and the "name" identifier were not parsed as they will
     * have no effect on whether or not the actual ids are unique.
     *
     * @param expectedAmount The requested amount of ids
     * @param token The token used to format the ids
     */
    @Test(dataProvider = "autoMinter parameters")
    public void testRandomAutoMinter(int expectedAmount, String token) {
        // must be greater than 2
        int rootLength = 5;

        String prefix = "";
        Set<String> set = new HashSet<String>();

        Minter AutoMinter = new Minter(DatabaseManager, "/:ark/NAAN/", rootLength, prefix, false);

        try {

            DatabaseManager.getPermutations(prefix, token, rootLength, true);
            String JSONIds = AutoMinter.genIdAutoRandom(expectedAmount, token);

            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.nextLine();
                if (!id.matches("(^[{]$)|(^.*\"id\".*.*$)|(^[}]$)")) {
                    System.out.println(id);
                    // add id to the set
                    set.add(id);
                }
            }
            Assert.assertEquals(set.size(), expectedAmount);
        } catch (SQLException | IOException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }

    /**
     *
     * @param expectedAmount
     * @param token
     */
    @Test(dataProvider = "autoMinter parameters")
    public void testSequentialAutoMinter(int expectedAmount, String token) {
        // must be greater than 2
        int rootLength = 5;

        String prefix = "";
        Set<String> set = new LinkedHashSet<String>();

        Minter AutoMinter = new Minter(DatabaseManager, "/:ark/NAAN/", rootLength, prefix, false);

        try {
            DatabaseManager.getPermutations(prefix, token, rootLength, true);
            String JSONIds = AutoMinter.genIdAutoSequential(expectedAmount, token);

            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.nextLine();
                if (!id.matches("(^[{]$)|(^.*\"id\".*.*$)|(^[}]$)")) {
                    System.out.println(id);
                    // add id to the set
                    set.add(id);
                }
            }
            Assert.assertEquals(set.size(), expectedAmount);
            Iterator<String> iter = set.iterator();
            String prev = iter.next();
            while (iter.hasNext()) {
                String current = iter.next();
                if(compare(prev,current) > -1){
                    Assert.fail("The ids are not sequential: prev="+prev+"\tcurrent="+current);
                }
                prev = current;
            }            
        } catch (SQLException | IOException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }
    
    /**
     *
     * @param expectedAmount
     * @param charMap
     * @param expectedToken
     */
    @Test(dataProvider = "customMinter parameters")
    public void testRandomCustomMinter(int expectedAmount, String charMap, String expectedToken) {       
        String prefix = "";
        Set<String> set = new HashSet<String>();

        Minter CustomMinter = new Minter(DatabaseManager, "", charMap, prefix, false);

        try {            
            DatabaseManager.getPermutations(prefix, false, charMap);
            String JSONIds = CustomMinter.genIdCustomRandom(expectedAmount);

            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.next().trim();
                if (!id.matches("(^[{]$)|(^.*\"id\".*$)|(^[}]$)|(\"name\")|(:)|(\\d.*,)")) {
                    System.out.println("(l="+id.length()+")parsedid= " + id);
                    String parsedId = id.substring(1, id.length()-1);
                    
                    System.out.println("expected: " + expectedToken);
                    System.out.println("getToken: " + getToken(parsedId));
                    Assert.assertEquals(getToken(parsedId), expectedToken);
                    
                    System.out.println(id);
                    // add id to the set
                    set.add(id);
                }
            }
            Assert.assertEquals(set.size(), expectedAmount);
            
            
                      
        } catch (SQLException | IOException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }
    
    
    /**
     *
     * @param expectedAmount
     * @param charMap
     * @param expectedToken
     */
    @Test(dataProvider = "customMinter parameters")
    public void testSequentialCustomMinter(int expectedAmount, String charMap, String expectedToken) {       
        String prefix = "";
        Set<String> set = new LinkedHashSet<String>();

        Minter CustomMinter = new Minter(DatabaseManager, "", charMap, prefix, false);

        try {            
            DatabaseManager.getPermutations(prefix, false, charMap);
            String JSONIds = CustomMinter.genIdCustomSequential(expectedAmount);

            Scanner JSONParser = new Scanner(JSONIds);
            while (JSONParser.hasNext()) {
                String id = JSONParser.next().trim();
                if (!id.matches("(^[{]$)|(^.*\"id\".*$)|(^[}]$)|(\"name\")|(:)|(\\d.*,)")) {
                    System.out.println("(l="+id.length()+")parsedid= " + id);
                    String parsedId = id.substring(1, id.length()-1);
                    
                    System.out.println("expected: " + expectedToken);
                    System.out.println("getToken: " + getToken(parsedId));
                    Assert.assertEquals(getToken(parsedId), expectedToken);
                    
                    System.out.println(id);
                    // add id to the set
                    set.add(id);
                }
            }
            Assert.assertEquals(set.size(), expectedAmount);            
            Iterator<String> iter = set.iterator();
            String prev = iter.next();
            while (iter.hasNext()) {
                String current = iter.next();
                if(compare(prev,current) > -1){
                    Assert.fail("The ids are not sequential: prev="+prev+"\tcurrent="+current);
                }
                prev = current;
            }      
            
                      
        } catch (SQLException | IOException exception) {
            Assert.fail(exception.getMessage(), exception);
        }
    }
    
    
    /**
     * This method returns an equivalent token for any given charMap
     *
     * @param charMap The mapping used to describe range of possible characters
     * at each of the id's root's digits
     * @return the token equivalent to the charMap
     */
    private String getToken(String token) {

        String regex;
        if (token.matches("([\\d]+)")){                
            return "DIGIT";
        } else if (token.matches("([a-z]+)")) {
            return "LOWERCASE";
        } else if (token.matches("([A-Z]+)")) {
            return "UPPERCASE";
        } else if (token.matches("([a-zA-Z]+)")) {
            return "MIXEDCASE";
        } else if (token.matches("^(?=[\\da-z]*\\d)(?=[\\da-z]*[a-z])[\\da-z]*$")) {
            return "LOWER_EXTENDED";
        } else if (token.matches("^(?=[\\dA-Z]*\\d)(?=[\\dA-Z]*[A-Z])[\\dA-Z]*$")) {
            return "UPPER_EXTENDED";
        } else if (token.matches("^((?=[\\da-zA-Z]*[a-z])"
                + "(?=[\\da-zA-Z]*[\\d])"
                + "(?=[\\da-zA-Z]*[A-Z])[\\da-zA-Z]*)$")) {
            return "MIXED_EXTENDED";
        } else {
            // throw an error here
            regex = token;
            System.out.println("theres an error with regex");
        }
        return regex;
    }

    
    @BeforeTest
    public static void setUpClass() throws Exception {
        File db = new File(DbName + ".db");
        System.out.println(db.getAbsolutePath());
        if (db.exists()) {
            System.out.println("found test database; deleting...");
            db.delete();
            System.out.print("done");
        }
    }

    @AfterTest
    public static void tearDownClass() throws Exception {
        File db = new File(DbName + ".db");
        if (db.exists()) {
            System.out.println("deleting test database...");
            db.delete();
            System.out.print("done");
        }
    }

    @BeforeTest
    public void setUpTest() throws Exception {
        DatabaseManager.createConnection();

    }

    @AfterTest
    public void tearDownTest() throws Exception {
        String sqlQuery = "DROP TABLE " + MinterTest.DbName + "" + 
                DatabaseManager.getTABLE_NAME()+ ";";
        
        
        DatabaseManager.closeConnection();
        
        
    }
    
    

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
