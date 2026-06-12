package com.viettel.demo.exception;

public class MasterDataCodeConflictException extends RuntimeException {

    public MasterDataCodeConflictException(String code) {
        super("MasterData code already exists in this tenant: " + code);
    }
}
