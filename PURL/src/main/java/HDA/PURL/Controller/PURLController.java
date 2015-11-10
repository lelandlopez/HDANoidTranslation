package HDA.PURL.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import HDA.PURL.DBConn.DBConn;
import HDA.PURL.Model.model_Purl;

@Controller
public class PURLController {

	/*
	 * matches url: /PURL/retrieve
	 * required input purlid
	 * output: json of purl object
	*/
	@RequestMapping("/retrieve")
    public ModelAndView retrieve(@RequestParam(value="purlid", required = true) String purlid) {
		DBConn dbConn = new DBConn("jdbc:postgresql://localhost/HDA", "postgres", "waiakea2009");  //connect to db
		dbConn.openConnection();	//open connection
		model_Purl purl = dbConn.retrieveModel(purlid);	//retrieve purl object
		dbConn.closeConnection();	//close connection
		ModelAndView mv = new ModelAndView("retrieve", "purl", purl); //show retrieve view, attach purl object.  converted to json at view.
		return mv;
    }
	/*
	 * matches url: /PURL/retrieve
	 * required input purlid, url
	 * rows where purlid = purlid, url will be replaced
	 * output: json of purl edited purl object
	*/
	@RequestMapping("/edit")
	public ModelAndView edit(@RequestParam(value="purlid", required = true) String purlid, 
			@RequestParam(value="url", required = true) String url) {
		DBConn dbConn = new DBConn("jdbc:postgresql://localhost/HDA", "postgres", "waiakea2009");  //connect to db
		dbConn.openConnection();	//connect to db
		dbConn.editURL(purlid, url);	//edit url
		model_Purl purl = dbConn.retrieveModel(purlid);		//retrieve edited purl object
		dbConn.closeConnection();	//close connection
		ModelAndView mv = new ModelAndView("edit", "purl", purl);	//show edit view, attach purl object.  converted to json at view.
		return mv;
	}
	
	
	

}
