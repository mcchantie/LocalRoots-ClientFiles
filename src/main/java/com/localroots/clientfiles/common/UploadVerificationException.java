package com.localroots.clientfiles.common;

import org.springframework.http.HttpStatus;

public class UploadVerificationException extends ApiException {

    public UploadVerificationException(String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Upload verification failed", detail);
    }
}
