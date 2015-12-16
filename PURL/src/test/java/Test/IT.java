package Test;

import java.io.IOException;
import java.util.Random;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

import Controller.PURLController;
import DBConn.DBConn;
import org.junit.Assert;
import Model.model_Purl;
import org.junit.runners.MethodSorters;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IT {
	
	private static String test_purlid;
	
	public static StringBuilder randomString() {
		Random rn = new Random();
		int z;
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < 20; i++) {
			z = rn.nextInt(27) + 48;
			sb.append((char)(z));
			
		}
		System.out.println(sb.toString());
		return sb;
		
	}
	
	@BeforeClass
	public static void setup() {
		test_purlid=randomString().toString();
	}
	
	@Test
	public void test1() throws IOException {
		DBConn dbconn = new DBConn();
		Assert.assertEquals(true, dbconn.openConnection());
	}
	
	@Test
	public void test2() throws IOException {
		String test_string ="{\"PURL\":\""+ test_purlid +"\",\"URL\":\"first_URL\",\"ERC\":\"first_ERC\",\"Who\":\"first_WHO\",\"What\":\"first_What\",\"When\":\"first_When\"}";
		PURLController PC = new PURLController();
		Assert.assertEquals(
				((model_Purl) PC.insert(test_purlid, "first_URL", "first_ERC", "first_WHO", "first_What", "first_When").getModel().get("purl")).toJSON(),
				test_string);
	}
	
	@Test
	public void test3() throws IOException {
		String test_string ="{\"PURL\":\""+test_purlid +"\",\"URL\":\"first_URL\",\"ERC\":\"first_ERC\",\"Who\":\"first_WHO\",\"What\":\"first_What\",\"When\":\"first_When\"}";
		PURLController PC = new PURLController();
		Assert.assertEquals(
				((model_Purl) PC.retrieve(test_purlid).getModel().get("purl")).toJSON(),
				test_string);
	}
	
	@Test
	public void test4() throws IOException {
		PURLController PC = new PURLController();
		Assert.assertEquals(( PC.insert(test_purlid, "first_URL", "first_ERC", "first_WHO", "first_What", "first_When").getViewName()), "null");
	}
	
	@Test
	public void test5() throws IOException {
		String test_string ="{\"PURL\":\""+ test_purlid +"\",\"URL\":\"first_URL_Edit\",\"ERC\":\"first_ERC\",\"Who\":\"first_WHO\",\"What\":\"first_What\",\"When\":\"first_When\"}";
		PURLController PC = new PURLController();
		Assert.assertEquals(
				((model_Purl) PC.edit(test_purlid, "first_URL_Edit").getModel().get("purl")).toJSON(),
				test_string);
	}
	
	@Test
	public void test6() throws IOException {
		PURLController PC = new PURLController();
		Assert.assertEquals(( PC.edit("doesnt_exist", "first_URL").getViewName()), "null");
	}
	
	@Test
	public void test7() throws IOException {
		PURLController PC = new PURLController();
		Assert.assertEquals(( PC.delete(test_purlid).getViewName()), "deleted");
	}
	
	

}
