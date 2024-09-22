package com.buvatu.cronjob.management.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Activity {

    private String jobName;
    private String activityName;
    private Integer poolSize;
    private String taskName;
    private Future<String> future;

    public Activity(String jobName, String activityName, String taskName, Integer poolSize) {
        this.jobName = jobName;
        this.activityName = activityName;
        this.taskName = taskName;
        if (Objects.isNull(poolSize)) {
            poolSize = 5; // Default pool size
        }
        this.poolSize = poolSize;
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private List<Activity> runningActivityList;

    public String getTaskExecutionResult() {
        try {
            runningActivityList.add(this);
            Callable<String> task = context.getBean(taskName, Callable.class.asSubclass(String.class));
            future = Executors.newFixedThreadPool(poolSize, new CustomizableThreadFactory(jobName + "-" + activityName)).submit(task);
            return future.get();
        } catch (Exception e) {
            return "Exception";
        } finally {
            runningActivityList.remove(this);
        }
    }

    public void stop() {
        if (Objects.isNull(future)) {
            return;
        }
        future.cancel(true);
        runningActivityList.remove(this);
    }

}
