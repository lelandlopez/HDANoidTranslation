package com.hida;

/**
 * An exception that was created to display what parameter was incorrect and the value
 * that caused an error.
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
