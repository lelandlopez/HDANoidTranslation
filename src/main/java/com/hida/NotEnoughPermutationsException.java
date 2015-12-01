/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hida;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author lruffin
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Requested amount exceeds possible number "
        + "of permutations")
public class NotEnoughPermutationsException extends RuntimeException {

    private long RemainingPermutations;
    private long RequestedAmount;

    public NotEnoughPermutationsException(long remaining, long requested) {
        this.RemainingPermutations = remaining;
        this.RequestedAmount = requested;
    }

    /**
     * Creates a new instance of <code>TooManyPermutationsException</code>
     * without detail message.
     */
    public NotEnoughPermutationsException() {
    }

    /**
     * Constructs an instance of <code>TooManyPermutationsException</code> with
     * the specified detail message.
     *
     * @param msg the detail message.
     */
    public NotEnoughPermutationsException(String msg) {
        super(msg);
    }

    @Override
    public String getMessage() {
        return String.format("%d ids were requested but only %d can be created using given format",
                RequestedAmount, RemainingPermutations);
    }
}
