package com.buvatu.cronjob.management.model;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;

@Getter @Setter
public abstract class Activity {

    private final String activityName = this.getClass().getSimpleName();
    private Integer poolSize;
    private boolean interruptedFlag = false;
    private Callable<String> task;

    public Activity() {
        setTask(this::getTaskExecutionResult);
    }

    public abstract String getTaskExecutionResult();

}
