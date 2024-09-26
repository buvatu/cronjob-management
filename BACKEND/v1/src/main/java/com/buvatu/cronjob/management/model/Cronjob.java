package com.buvatu.cronjob.management.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.context.request.RequestContextHolder;

@Getter @Setter
public class Cronjob {

    private String cronjobName;
    private String expression;
    private String cronjobStatus;
    private ScheduledFuture<?> future;
    private Integer poolSize = 5; // Default pool_size = 5
    private Runnable cronjobExecutor;
    private String taskName;
    private Runnable task;

    @Autowired
    private CronjobManagementRepository cronjobManagementRepository; // helper

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    public void initializeCronjobExecutor() {
        cronjobExecutor = new Runnable() {

            public void run() {
                String currentCronjobStatus = getCronjobStatus();
                setCronjobStatus("RUNNING");
                Map<String, Object> cronjobHistoryLog = new HashMap<String, Object>();
                String taskExecutionResult = "SUCCESS";
                try {
                    Executors.newFixedThreadPool(poolSize).execute(task);
                } catch (Exception e) {
                    taskExecutionResult = "EXCEPTION: " + e.getMessage();
                }
                setCronjobStatus(currentCronjobStatus);
                if ("SCHEDULED".equals(cronjobStatus)) {
                    schedule(expression);
                }
                cronjobManagementRepository.insertCronjobHistoryLog(cronjobHistoryLog);
            }

        };
    }

    public void schedule(String updatedExpression) {
        if (Objects.nonNull(future)) future.cancel(true);
        expression = updatedExpression;
        future = taskScheduler.schedule(cronjobExecutor, new CronTrigger(expression));
        setCronjobStatus("SCHEDULED");
    }

    public void cancel() {
        if (Objects.nonNull(future)) future.cancel(true);
    }

    public void forceStart() {
        future = taskScheduler.schedule(cronjobExecutor, Timestamp.valueOf(LocalDateTime.now().plusSeconds(1)));// delay 1s to run
    }

    public void setCronjobStatus(String cronjobStatus) {
        this.cronjobStatus = cronjobStatus;
        cronjobManagementRepository.updateCronjobStatus(cronjobStatus, cronjobName);
    }
}
