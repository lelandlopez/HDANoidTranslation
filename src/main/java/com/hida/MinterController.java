package com.hida;

import javax.json.JsonObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * A controller class that paths the user to all jsp files
 *
 * @author lruffin
 */
@Controller
public class MinterController {

    /**
     * Creates a path to mint ids
     *
     * @param input - requested number of ids to mint
     * @param model - serves as a holder for the model so that attributes can be
     * added
     * @return - paths user to mint.jsp
     */
    @RequestMapping(value = {"/mint/{input}"}, method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String printPids(@PathVariable String input, ModelMap model) {
        try {
            int amount = Integer.parseInt(input);

            createArkMinter(amount, model);
        }  // detects number fomatting errors in input
        catch (NumberFormatException exception) {
            String message = String.format(
                    "input error %s", new Object[]{exception.getMessage()});
            model.addAttribute("message", message);
        }  // detects any unimplemented methods
        catch (UnsupportedOperationException exception) {
            String message = String.format(
                    "%s", new Object[]{exception.getMessage()});
            model.addAttribute("message", message);
        }
        return "mint";
    }

    /**
     * Used to create PseudoMinter. Will be implemented after deciding how data
     * should be received from user. JavaBeans?
     *
     * @param amount - requested number of ids to mint
     * @param model - serves as a holder for the model so that attributes can be
     * added
     */
    private void createPseudoMinter(int amount, ModelMap model) {
        PseudoMinter pminter = new PseudoMinter(amount);

        pminter.retrieveSettings();

        JsonObject idList = pminter.genIdAuto("EXTENDED");

        model.addAttribute("message", idList);
    }

    /**
     * Used to create ArkMinter.
     *
     * @param amount - requested number of ids to mint
     * @param model - serves as a holder for the model so that attributes can be
     * added
     */
    private void createArkMinter(int amount, ModelMap model) {
        ArkMinter arkminter = new ArkMinter(amount);

        arkminter.retrieveSettings();

        JsonObject idList = arkminter.genIdAuto("EXTENDED");

        model.addAttribute("message", idList);
    }

    /**
     * Maps to home page
     *
     * @return
     */
    @RequestMapping(value = {""},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String printIndex() {
        return "index";
    }

    /**
     * maps to settings.jsp so that the user may input data in a form
     *
     * @param model
     * @return
     */
    @RequestMapping(value = {"/settings"},
            method = {org.springframework.web.bind.annotation.RequestMethod.GET})
    public String handleForm(ModelMap model) {
        return "settings";
    }
}
