package com.viettel.files.file;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FileStorageNotFoundException extends RuntimeException {

    public FileStorageNotFoundException(String message) {
        super(message);
    }
}
