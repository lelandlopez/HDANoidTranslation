/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hida;

/**
 *
 * @author lruffin
 */
public class BadParameterException extends Exception {
    private String Parameter;
    
    /**
     * Creates a new instance of <code>BadParameterException</code> without
     * detail message.
     */
    public BadParameterException() {
    }

    public BadParameterException(String parameter, String msg){
        super(msg);
        Parameter = parameter ;
    }
    
    /**
     * Constructs an instance of <code>BadParameterException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public BadParameterException(String msg) {
        super(msg);
    }
    
    @Override
    public String getMessage(){
        return "A bad parameter was detected: " + Parameter;
    }
}
