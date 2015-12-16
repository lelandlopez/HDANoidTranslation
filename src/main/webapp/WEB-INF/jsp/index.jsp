<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Index</title>
    </head>
    <body>
        <h1>Instructions</h1>
        <pre>
        <h3>paths:</h3>
        */settings
        */mint/amount?*

        for example:
        http://localhost:8080/MinterService/mint/10
        http://localhost:8080/MinterService/mint/10?auto=false&charMap=ddlumedd
        http://localhost:8080/MinterService/mint/10?auto=true&length=5&tokenType=LOWERCASE

        <h3>parameters for mint path:</h3>
        boolean auto
        boolean random
        boolean sansVowels
        int length
        String prefix
        String prepend
        String charMap 
        String tokenType

        <h3>char map key:</h3>
        d = digital 
        l = lowercase
        u = uppercase 
        m = mixed-case 
        e = extended         
        

        Any other suggestions for this page?        
        - Settings management here?
        - explanation of service?
        </pre>
        
    </body>
</html>
