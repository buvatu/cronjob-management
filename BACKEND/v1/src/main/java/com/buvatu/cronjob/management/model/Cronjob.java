package com.buvatu.cronjob.management.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter @Setter
public class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();
    private String sessionId;
    private String expression;
    private String cronjobStatus;
    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> future;
    private Integer poolSize = 5; // Default pool_size = 5
    private Runnable cronjobExecutor;
    private Runnable task;

    private final CronjobManagementRepository cronjobManagementRepository; // helper

    public Cronjob(CronjobManagementRepository cronjobManagementRepository) {
        this.cronjobManagementRepository = cronjobManagementRepository;
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("cronjob-" + cronjobName + "-");
        taskScheduler.initialize();
        initializeCronjobExecutor();
    }

    public void initializeCronjobExecutor() {
        cronjobExecutor = () -> {
            String currentCronjobStatus = getCronjobStatus();
            setCronjobStatus("RUNNING");
            setSessionId(UUID.randomUUID().toString());
            Map<String, Object> cronjobHistoryLog = new HashMap<>();
            cronjobHistoryLog.put("cronjobName", cronjobName);
            cronjobHistoryLog.put("sessionId", getSessionId());
            cronjobHistoryLog.put("startTime", LocalDateTime.now());
            try {
                Executors.newFixedThreadPool(poolSize).execute(task);
                cronjobHistoryLog.put("executeResult", "SUCCESS");
            } catch (Exception e) {
                cronjobHistoryLog.put("executeResult", "EXCEPTION: " + e.getMessage());
            }
            cronjobHistoryLog.put("endTime", LocalDateTime.now());
            Map<String, Object> cronjobHistoryLogMap = cronjobManagementRepository.getLatestCronjobHistoryLog(cronjobName);
            if ("START JOB MANUALLY".equals(cronjobHistoryLogMap.get("operation")) && Objects.isNull(cronjobHistoryLogMap.get("executeResult"))) { // Start job manually case
                cronjobHistoryLog.put("id", cronjobHistoryLogMap.get("id"));
                cronjobManagementRepository.updateCronjobHistoryLog(cronjobHistoryLog);
            } else {
                cronjobHistoryLog.put("operation", "RUN JOB ON A SCHEDULE");
                cronjobHistoryLog.put("executedBy", "SYSTEM");
                cronjobManagementRepository.insertCronjobHistoryLog(cronjobHistoryLog);
            }
            setCronjobStatus(currentCronjobStatus);
            if ("SCHEDULED".equals(cronjobStatus)) { // Re-schedule with existed expression
                future = taskScheduler.schedule(cronjobExecutor, new CronTrigger(expression));
            }
        };
    }

    public void schedule(String updatedExpression) {
        if (StringUtils.hasText(updatedExpression) && !updatedExpression.equals(expression)) setExpression(updatedExpression);
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

    public String getCronjobStatus() {
        // Use for multiple instances
        String currentCronjobStatus = cronjobManagementRepository.getCronjobStatus(cronjobName);
        if (!cronjobStatus.equals(currentCronjobStatus)) setCronjobStatus(currentCronjobStatus);
        return cronjobStatus;
    }

    public void setExpression(String expression) {
        this.expression = expression;
        cronjobManagementRepository.updateCronjobExpression(expression, cronjobName);
    }

    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
        cronjobManagementRepository.updateCronjobPoolSize(poolSize, cronjobName);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        cronjobManagementRepository.updateCronjobSessionId(sessionId, cronjobName);
    }
}
