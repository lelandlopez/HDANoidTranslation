package Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import DBConn.DBConn;
import Model.model_Purl;

import java.io.IOException;

import org.apache.log4j.Logger;

@Controller
public class PURLController {

	private static final Logger logger = Logger.getLogger(PURLController.class);
	/**
	 * matches url: /PURL/retrieve
	 * retrieves corresponding purl row of provided purlid
	 * returns model - purl and view : retrieve if successful
	 * returns model - null if not 
	 * @param purlid purlid of desired retrieved row
	 * @return ModelAndView
	 * @throws IOException throws if connection could not be made
	 */
	@RequestMapping("/retrieve")
    public ModelAndView retrieve(@RequestParam(value="purlid", required = true) String purlid) throws IOException {
		if(logger.isInfoEnabled()){
			logger.info("Retrieve was Called");
		}
		DBConn dbConn = new DBConn();  //connect to db
		dbConn.openConnection();	//open connection
		model_Purl purl = dbConn.retrieveModel(purlid);	//retrieve purl object
		dbConn.closeConnection();	//close connection
		if(purl != null) {
			ModelAndView mv = new ModelAndView("retrieve", "purl", purl); //show retrieve view, attach purl object.  converted to json at view.
			logger.info("Retrieve returned: " + purl.toJSON());
			return mv;  
		} else {
			System.out.println("got here");
			ModelAndView mv = new ModelAndView("null");
			logger.info("insert returned: " + null);
			return mv;
		}
    }

	
	/**
	 * matches url: /PURL/edit
	 * edit purlid row url, with provided url
	 * returns model : purl and view : edit if successful
	 * returns model : null if not 
	 * @param purlid purlid of desired edited row
	 * @param url url that desired row url will be changed to
	 * @return ModelAndView
	 * @throws IOException throws if connection could not be made
	 */
	@RequestMapping("/edit")
	public ModelAndView edit(@RequestParam(value="purlid", required = true) String purlid, 
			@RequestParam(value="url", required = true) String url) throws IOException {
		if(logger.isInfoEnabled()){
			logger.info("Edit was Called");
		}
		DBConn dbConn = new DBConn();  //connect to db
		dbConn.openConnection();	//connect to db
		dbConn.editURL(purlid, url);	//edit url
		model_Purl purl = dbConn.retrieveModel(purlid);		//retrieve edited purl object
		dbConn.closeConnection();	//close connection
		if(purl != null) {
			ModelAndView mv = new ModelAndView("edit", "purl", purl);	//show edit view, attach purl object.  converted to json at view.
			logger.info("Edit returned: " + purl.toJSON());
			return mv; 
		} else {
			ModelAndView mv = new ModelAndView("null");
			logger.info("Edit returned: " + null);
			return mv;
		}
	}
	

	/**
	 * matches url: /PURL/insert
	 * inserts purlid, url, erc, who, what, when to new row of table
	 * returns model : purl and view : insert if successful
	 * returns model : null if not 
	 * @param purlid purlid to be inserted
	 * @param url url to be inserted
	 * @param erc erc to be inserted
	 * @param who who to be inserted
	 * @param what what to be inserted
	 * @param when when to be insertd
	 * @return ModelAndView
	 * @throws IOException throws if db conn not successful
	 */
	@RequestMapping("/insert")
	public ModelAndView insert(@RequestParam(value="purlid", required = true) String purlid, 
			@RequestParam(value="url", required = true) String url,
			@RequestParam(value="erc", required = true) String erc,
			@RequestParam(value="who", required = true) String who,
			@RequestParam(value="what", required = true) String what,
			@RequestParam(value="when", required = true) String when
			) throws IOException {
		if(logger.isInfoEnabled()){
			logger.info("Insert was Called");
		}
		DBConn dbConn = new DBConn();  //connect to db
		dbConn.openConnection();	//connect to db
		if(dbConn.insertPURL(purlid, url, erc, who, what, when)) {
			model_Purl purl = dbConn.retrieveModel(purlid);	
			ModelAndView mv = new ModelAndView("insert", "purl", purl);	//show edit view, attach purl object.  converted to json at view.
			dbConn.closeConnection();	//close connection
			logger.info("insert returned: " + purl.toJSON());
			return mv;
		} else {
		dbConn.closeConnection();	//close connection
			ModelAndView mv = new ModelAndView("null");	//show edit view, attach purl object.  converted to json at view.
			logger.info("insert returned: " + null);
			return mv;
		}
		
	}
	

	/**
	 * matches url: /PURL/delete
	 * deletes row of table with corresponding purlid
	 * returns view : deleted if successful
	 * returns model : null if not 
	 * @param purlid purlid of desired deleted row
	 * @return ModelAndView
	 * @throws IOException throws if dbConn is not successful
	 */
	@RequestMapping("/delete")
	public ModelAndView delete(@RequestParam(value="purlid", required = true) String purlid
			) throws IOException {
		if(logger.isInfoEnabled()){
			logger.info("Insert was Called");
		}
		DBConn dbConn = new DBConn();  //connect to db
		dbConn.openConnection();	//connect to db
		if(dbConn.deletePURL(purlid)) {
			ModelAndView mv = new ModelAndView("deleted");	//show edit view, attach purl object.  converted to json at view.
			dbConn.closeConnection();	//close connection
			logger.info("{\"result\":\"success\"}");
			return mv;
		} else {
		dbConn.closeConnection();	//close connection
			ModelAndView mv = new ModelAndView("null");	//show edit view, attach purl object.  converted to json at view.
			logger.info("insert returned: " + null);
			return mv;
		}
		
	}
	
	
}