package com.buvatu.cronjob.management.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class BusinessException extends RuntimeException {

    private int code;
    private String message;

}
