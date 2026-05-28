package com.viettel.demo.storage;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
 * 404 tenant-safe cho file không tồn tại trong tenant hiện tại.
 *
 * Với cross-tenant access, backend cũng trả 404 để không tiết lộ fileId đó
 * có tồn tại ở tenant khác hay không.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class FileStorageNotFoundException extends RuntimeException {

    public FileStorageNotFoundException(String message) {
        super(message);
    }
}
