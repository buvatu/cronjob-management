package com.buvatu.cronjob.management.model;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Getter @Setter
public class Activity {

    private Callable<String> task;
    private Integer poolSize;
    private String activityName;
    private ThreadPoolExecutor taskExecutor;

    public String getTaskExecutionResult() throws ExecutionException, InterruptedException {
        taskExecutor.setCorePoolSize(poolSize);
        taskExecutor.setMaximumPoolSize(poolSize * 2);
        taskExecutor.submit(task);
        return Executors.newFixedThreadPool(poolSize).submit(task).get();
    }
}
