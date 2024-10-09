package com.buvatu.cronjob.management.model;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public interface Task {

    public abstract String getTaskExecutionResult();

}
