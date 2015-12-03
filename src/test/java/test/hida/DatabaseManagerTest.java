/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.hida;

//import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import com.hida.DatabaseManager;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author lruffin
 */
public class DatabaseManagerTest {
    DatabaseManager DatabaseManager = new DatabaseManager();
    
    public DatabaseManagerTest() {
        
    }

    @Test
    public void hello(){
        Assert.assertEquals("hi", "hi");        
    }
    
    @Test
    public void testConnection(){
        
    }
        
    
    @Test
    public void addIdsTest(){
        
    }
    
    

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }
    
    
    
}
