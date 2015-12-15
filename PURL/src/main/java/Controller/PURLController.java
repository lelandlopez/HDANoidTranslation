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
	/*
	 * matches url: /PURL/retrieve
	 * retrieves corresponding purl row of provided purlid
	 * @param required purlid - purlid of PURL to retrieve
	 * @return: json string of purl_model
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
	/*
	 * matches url: /PURL/edit
	 * edit purlid row url, with provided url
	 * @param required purlid - purlid of PURL to edit
	 * @param required url - url to be changed to
	 * @return: json string of purl_model if succeed, null string if it doesn't
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
	
	/*
	 * matches url: /PURL/insert
	 * inserts purlid, url, erc, who, what, when to new row of table
	 * @param required purlid - purl id to add to row
	 * @param required url - url to add to row
	 * @param required erc - erc to add to row
	 * @param required who - who to add to row
	 * @param required what - what to add to row
	 * @param required when - when to add to row
	 * @return: json string of purl_model if succeed, null string if it doesn't
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
			ModelAndView mv = new ModelAndView("retrieve", "purl", purl);	//show edit view, attach purl object.  converted to json at view.
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
	
	/*
	 * matches url: /PURL/insert
	 * inserts purlid, url, erc, who, what, when to new row of table
	 * @param required purlid - purl id to add to row
	 * @param required url - url to add to row
	 * @param required erc - erc to add to row
	 * @param required who - who to add to row
	 * @param required what - what to add to row
	 * @param required when - when to add to row
	 * @return: json string of purl_model if succeed, null string if it doesn't
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
			model_Purl purl = dbConn.retrieveModel(purlid);	
			ModelAndView mv = new ModelAndView("deleted");	//show edit view, attach purl object.  converted to json at view.
			dbConn.closeConnection();	//close connection
			logger.info("{\"result\":\"deleted\"}");
			return mv;
		} else {
		dbConn.closeConnection();	//close connection
			ModelAndView mv = new ModelAndView("null");	//show edit view, attach purl object.  converted to json at view.
			logger.info("insert returned: " + null);
			return mv;
		}
		
	}
	
	
}