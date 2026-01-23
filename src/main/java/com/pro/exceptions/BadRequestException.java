package com.pro.exceptions;

public class BadRequestException extends RuntimeException {
    private final ErrorType type;

    public BadRequestException(String msg) {
        super(msg);
        this.type = ErrorType.GENERIC_ERROR;
    }

    public BadRequestException(String msg, ErrorType type) {
        super(msg);
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }
}


