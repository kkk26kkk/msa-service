package com.example.order.exception;

/**
 * 잘못된 주문 정보로 인해 발생하는 예외
 */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}


