package com.example.member.exception;

/**
 * 중복된 회원 정보로 인해 발생하는 예외
 */
public class DuplicateMemberException extends RuntimeException {

    public DuplicateMemberException(String message) {
        super(message);
    }

    public DuplicateMemberException(String message, Throwable cause) {
        super(message, cause);
    }
}


