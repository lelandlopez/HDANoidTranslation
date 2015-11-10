<%-- 
    Document   : settings
    Created on : Nov 9, 2015, 11:53:52 AM
    Author     : Brittney
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<html>

    <head>
        <title>
            Minter Request Form
        </title>

        <script language="JavaScript" type='text/javascript'>
            function checkData() {
                var i = 0;

                //CHAR MAPPING ERROR:
                if (document.getElementById("charmap").value.match(/^[dlumea]+$/) === null) {
                    document.getElementById("charmapErr").style.display = "block";
                    i++;
                    document.getElementById("charmap").focus();
                } else {
                    document.getElementById("charmap").style.display = "none";
                }

                //ID PREFIX ERROR:

                //ID LENGTH ERROR:

                if (i === 0) {
                    document.getElementById("formErr").style.display = "none";
                    return true;
                } else {
                    document.getElementById("formErr").style.display = "block";
                    return false;
                }

            } //End of checkData()
        </script>
    </head>

    <!-- ========== END OF HEAD, START OF BODY ================ -->
    <body bgcolor="#ffffff">

        <!-- === CONTENT AREA ===  -->
        <div style="position: absolute; left:30px; right:30px; ">
            <div align="left">

                <h2>Minter</h2>

                <p>Choose your desired minter, and input the char mapping as well. Then there's an option to input a prefix if wanted, the legnth of the ID, and lastly choose if you want it Automated or Custom'.<br><br></p>
            </div>

            <center>
                <hr width="80%">
            </center>

            <form onsubmit='return checkData()' >
                <table border="3" bgcolor="#FFFFF0" cellspacing="3" cellpadding="7">
                    <tr>
                        <!-- old style:
                         <td align="CENTER" bgcolor="#000080"><b><i><font color="#ffd700" size="+2"
                         >Shipping Information</font></i></b></td>
                        -->
                        <td style="background-color:#000080; font-size:large; font-style:italic;
                            font-weight:bold; color:#ffd700; text-align:center">
                            Minter Request Form</td>
                    </tr>
                    <tr>

                        <td><table border="0" cellspacing="2" cellpadding="4">

                                <!--MINTER:  A pull-down menu allows for a choice among different Minters.-->
                                <tr>
                                    <td align="RIGHT">Minter Type:</td>
                                    <td><select name="MinterType" id="minter">
                                            <option value="pseudo">Pseudo Minter</option>
                                            <option value="ark">Ark Minter</option></td>
                                </tr>


                                <!--FORMAT:  A pull-down menu allows for a choice among ID Format.-->
                                <tr>
                                    <td align="RIGHT">ID Format:</td>
                                    <td><select name="MinterIDformat" id="minter">
                                            <option value="auto">Automated</option>
                                            <option value="custom">Customize</option>
                                    </td>
                                </tr>


                                <!--CHAR MAPPING:  A text field to indicate desired Char Mapping.-->
                                <tr>
                                    <td align="right" valign="top">Char Mapping:</td>
                                    <td><input type="text" name="MinterCharMapping" size="25" maxlength="10" id="charmap">
                                        <div id="charmapErr" style="display:none; color:red">
                                            Must be lowercase "d", "l", "u", "m", "e", "a" values only.</div>
                                    </td>
                                </tr>


                                <!--ID PREFIX:  A text field for the desired prefix before each generated ID.-->
                                <tr>
                                    <td align="right" valign="top">ID Prefix:</td>
                                    <td><input type="text" name="MinterIDprefix" size="25" maxlength="10" id="IDprefix">
                                    </td>
                                </tr>


                                <!--ID LENGTH:  A  text field for the desired length generated for each ID.-->
                                <tr>
                                    <td align="right" valign="top">ID Length:</td>
                                    <td><input type="text" name="MinterIDlength" size="25" maxlength="2" id="IDlength"></td>
                                </tr>


                            </table>
                            <!--  end of overall table -->

                            <p>
                                <input type="reset" value="Clear Order" name="reset">
                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                                <input type="SUBMIT" onClick="checkData()" value="Begin Form" name="submit">
                            </p>
                            <div id="formErr" style="display:none; color:red">
                                Please fix the errors noted above before proceeding.
                            </div>

                            </form>


                            </div>

                            </body>
                            </html>