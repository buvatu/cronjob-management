package com.buvatu.cronjob.management.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class BusinessException extends RuntimeException {

    private int code;
    private String message;

}
