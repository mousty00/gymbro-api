package com.mousty.gymbro.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends GymBroException {

    private FileUploadException(String message, HttpStatus status) {
        super(message, status);
    }

    public static FileUploadException unsupportedContentType(String contentType) {
        return new FileUploadException(
                "Unsupported file type: " + contentType + ". Allowed types: image/png, image/jpeg, image/webp",
                HttpStatus.BAD_REQUEST);
    }

    public static FileUploadException empty() {
        return new FileUploadException("Uploaded file is empty", HttpStatus.BAD_REQUEST);
    }
}
