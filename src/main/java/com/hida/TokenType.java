package com.hida;



/**
 * An Enum created to hold define the tokens used by the Minter class. Enums 
 * are inherently serializable and, as such, do not need to implement Serializable.
 * All Enums will have the have a serialVersionUID of 0L and will therefore not
 * need to be specified. 
 * @author lruffin
 */
public enum TokenType {
    
    DIGIT,
    LOWERCASE,
    UPPERCASE,
    MIXEDCASE,
    LOWER_EXTENDED,
    UPPER_EXTENDED,
    MIXED_EXTENDED,
    ERROR;
}
