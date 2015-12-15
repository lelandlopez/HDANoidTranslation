<%-- 
    Document   : settings
    Created on : Nov 9, 2015, 11:53:52 AM
    Author     : Brittany Cruz
--%>

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<html>
    
<head>
    
    
    <link href="css/styles.css" rel="stylesheet" type="text/css">
    <link href="/css/styles.css" rel="stylesheet" type="text/css">
    
   

    
    <script language = "javascript">
        //When first loading the page.
        window.onload = function() {
            document.getElementById("trcharmapping").style.display = "none";
        }
    
    
        //Submitting the form.
        function submitForm() {
            var minter = document.getElementById("mintergentype").value;
            var noSubmitAutoElements = ['charmapping'];
            var noSubmitCustomElements = ['token', 'idlength'];
            //Automated ID's - remove Custom ID's fields.
            if (minter == "auto") {
                for( var i = 0, j = noSubmitAutoElements.length; i < j; i++ ) {
                    document.getElementById(noSubmitElements[i]).removeAttribute('name');
                }
            }
            //Custom ID's - remove Automated ID's fields.
            else {
                for( var i = 0, j = noSubmitCustomElements.length; i < j; i++ ) {
                    document.getElementById(noSubmitElements[i]).removeAttribute('name');
                }
            }
        }
        form1.onsubmit = submitForm;
        
        
    
        //Changing the display for when to show "Auto" or "Custom" elements.
        function onMinterSelected() {
            var minter = document.getElementById("mintergentype").value;
            //Automated ID's:
            if (minter == "auto") {
               document.getElementById("trtoken").style.display = "table-row";
                document.getElementById("tridlength").style.display = "table-row";
                document.getElementById("trcharmapping").style.display = "none";
            }
            //Custom ID's:
            else {
                document.getElementById("trtoken").style.display = "none";
                document.getElementById("tridlength").style.display = "none";
                document.getElementById("trcharmapping").style.display = "table-row";
                document.getElementById("trvowels").style.display = "table-row";
            }
        }
    
        //Changing the display for when to show the "Vowels" option.
        function onCaseSelected() {
           if(document.getElementById("lowercase").checked || document.getElementById("uppercase").checked) {
                document.getElementById("trvowels").style.display = "table-row";
           } else {
                document.getElementById("trvowels").style.display = "none";
           }     
        }
    
        //Check data when submitting form.
        function check() {
            var x;
            if (confirm("Are you sure you want to submit? Submitting will restart the service and reject all queued requests.") == true) {
                x = "Changes have been saved.";
            } else {
                function submitForm() { 
                    return false; 
                }
                x = "Canceled.";
            }
            document.getElementById("confirmed").innerHTML = x;
        }
       
        function prefixIDCHECK() {
            var isNumber =  /^[0-9a-zA-Z]*$/.test(document.getElementById("idprefix").value.toString());
            if(isNumber == true) {
            document.getElementById("idprefix").style.borderColor="#FFFFFF"
            document.getElementById("submit").disabled = false;
            } else {
            document.getElementById("idprefix").style.borderColor="#FF0000"
            document.getElementById("submit").disabled = true;
           
            }
        }
        
        
        function rootlengthCHECK() {
            var isNumber =  /^[0-9]+$/.test(document.getElementById("idlength").value.toString());
            if(isNumber == true) {
            document.getElementById("idlength").style.borderColor="#FFFFFF"
            document.getElementById("submit").disabled = false;
            } else {
            document.getElementById("idlength").style.borderColor="#FF0000"
            document.getElementById("submit").disabled = true;
           
            }
        }
        
        
        function charmappingCHECK() {
            var isNumber =  /^[dlume]+$/.test(document.getElementById("charmapping").value.toString());
            if(isNumber == true) {
            document.getElementById("charmapping").style.borderColor="#FFFFFF"
            document.getElementById("submit").disabled = false;
            } else {
            document.getElementById("charmapping").style.borderColor="#FF0000"
            document.getElementById("submit").disabled = true;
           
            }
        }
        
    </script>
</head>


<body>
    <!--HDA MINTER FORM: User form to select and customize Persistent ID Minter options.-->
    <form id="form1" runat="server" onsubmit="submitForm()" method="post" action="confirmation" novalidate>
        
        
        <!--FORM HEADER: A simple header for the form.-->
        <h2 align="center"><img src="images/hda.png" width=60 height=60 align="middle"> Settings</h2>
        
        
        
        <!--FORM TABLE: Includes all options on generating the Persistent ID's.-->
        <table id="table1"; cellspacing="5px" cellpadding="5%"; align="center" >
            <col width="10000px" />
            <col width="10000px" />
            
            
            
            <!--PREPEND: A textbox for user to input a prepend such as the NAAN, DOI, etc.-->
            <tr id="trprepend">
                <td align="right">
                    <a href="#" class="tooltip left" data-tool="This is a text box where the user can place a prepend variable. It isn't a required option to do so. (Example: '/:ark/12345/' would be a NAAN prepend. Etc.)"><img src="images/help.png" width=12 height=12>
                    </a>
                    Prepend:
                </td>
                <td>
                    <input type="text" name="prepend"/>
                </td>
            </tr>
            
            
            
            <!--ID PREFIX: A text field for the desired prefix before each generated ID.-->
            <tr id="tridprefix">
                <td align="right">
                    <a href="#" class="tooltip left" data-tool="This is a text box where the user can place a set ID prefix that will come before the root of each generated ID within the session. It isn’t a required option to do so. (Example: prefix001, prefix002, prefix003. Etc.)"><img src="images/help.png" width=12 height=12>
                    </a>
                    ID Prefix:
                </td>
                <td>
                    <input type="text" id="idprefix" name="idprefix" required pattern="[a-zA-Z0-9]*" onkeyup="prefixIDCHECK()"/>
                </td>
            </tr>
            
            
            
            <!--MINTER TYPE: A pull-down menu allows for a choice among different Minters.-->
            <tr id="trminter">
                <td align="right">
                    <a href="#" class="tooltip left" data-tool="Automated: Set pool of tokens for computer to choose requested characters, and set an ID length for each generated ID.    Custom: Use the Char Mapping feature by typing in specifically which char tokens in desired pattern.">
                        <img src="images/help.png" width=12 height=12>
                    </a>
                    Minter Type:
                </td>
                <td>
                    <select name="mintType" id="mintergentype" onchange="onMinterSelected()">
                        <option value="auto">Automated</option>
                        <option value="custom">Custom</option>
                    </select>
                    <select name="mintOrder">
                        <option value="random">Random</option>
                        <option value="sequential">Sequential</option>
                    </select>
                </td>
                
            </tr>
            
            
            
            <!--TOKEN TYPE: Allows users to check which set of chars to place into pool of characters to generate from.-->
            <tr id="trtoken">
                <td align="right">
                    <a href="#" class="tooltip left" data-tool="This consists of three checkboxes: Digits, Lowercase, and Uppercase. 
                        These are the only options of character selection for the generated ID’s.                        
                        Digits consists of '0123456789'.
                        Lowercase consists of 'abcdefghijklmnopqrstuvwxyz'.
                        Uppercase consists of 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'." 
                        ><img src="images/help.png" width=12 height=12>
                    </a>
                    Tokens:
                </td>
                <td>
                    <input type="checkbox" name="digits" value="digits" id="digits" onchange=""/>Digits
                    <input type="checkbox" name="lowercase" value="lowercase" id="lowercase" onclick="onCaseSelected()"/>Lowercase
                    <input type="checkbox" name="uppercase" value="uppercase" id="uppercase" onclick="onCaseSelected()"/>Uppercase
                </td>
            </tr>
            
            
            
            <!--CHAR MAPPING: A text field to indicate desired Char Mapping.-->
            <tr id="trcharmapping">
                <td align="right">
                    <a href="#" class="tooltip left" data-tool="A text box where user can specifically choose where each token can be arranged for the whole session to be generated. Char Mapping only takes the letters, “dlume”.
                        “d” is for Digits.
                        “l” is for Lowercase.
                        “u” is for Uppercase.
                        “m” is for Mixedcase.
                        “e” is for Extended.
                        "><img src="images/help.png" width=12 height=12></a>
                    Char Mapping:
                </td>
                <td>
                    <input type="text" id="charmapping" name="charmapping" required pattern="[dlume]+" onkeyup="charmappingCHECK()"/>
                </td>
            </tr>
            
            
            <!--VOWELS: A checkbox to include vowels within ID generation.-->
            <tr id="trvowels" style="display:none;">
                <td align="right">
                    <a href="#" class="tooltip left" data-tool="A checkbox where user can select if they want ID’s to be opaque or not. 
                       Vowels are already pre-checked, and only shows up if the user selects either or both, Lowercase and Uppercase.
                        Vowels consists of “aeiuoyAEIOUY”."><img src="images/help.png" width=12 height=12></a>
                    Vowels:
                </td>
                <td>
                    <input type="checkbox" name="vowels" value="vowels"/>Include Vowels
                </td>
            </tr>
            
            
            
            <!--ID LENGTH: A text field for the desired length generated for each ID.-->
            <tr id="tridlength">
                <td align="right">
                    <a href="#" class="tooltip left" data-tool="A text box where user can choose the length of the ID’s to be generated. 
                        It determines how many characters will be used. It has the parameter of 1-10.">
                        <img src="images/help.png" width=12 height=12></a>
                    Root Length:
                </td>
                <td>
                    <input type="number" id="idlength" name="idlength" min="1" max="10" onkeyup="rootlengthCHECK()"/>
                </td>
            </tr>
            
            

            <!--SUBMIT: Submit Button. End of form.-->
            <tr id="trsubmit">
                <td>
                    <td><br><input type="submit" name="submit" value="Submit" onclick="check()"/></td>
                </td>
            </tr>
            
            
            
            <!--CONFIRMED: Confirmation message if changes are saved or canceled.-->
            <tr id="trconfirmed">
                   <p id="confirmed" align="center"></p>
            </tr>
            
        </table> 
    </form>
    
    
</body>
    
 </html>
