package com.northstar.bank;

import org.springframework.http.HttpStatus;

final class TransferException extends RuntimeException {
    private final HttpStatus status;

    TransferException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus status() {
        return status;
    }
}
