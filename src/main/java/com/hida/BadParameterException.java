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
    private Object Parameter;
    private String ParameterType;
    
    /**
     * Creates a new instance of <code>BadParameterException</code> without
     * detail message.
     */
    public BadParameterException() {
    }

    public BadParameterException(Object parameter, String parameterType){        
        Parameter = parameter ;
        ParameterType = parameterType;
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
        return String.format("An invalid '%s' was detected: %s", ParameterType, Parameter);
    }
}
