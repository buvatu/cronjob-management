package com.buvatu.cronjob.management.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
    private ScheduledFuture<?> scheduledFuture;
    private Integer poolSize = 5; // Default pool_size = 5
    private Runnable cronjobExecutor;
    private Runnable task;
    private ExecutorService executor;

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
            cronjobHistoryLog.put("cronjobName", getCronjobName());
            cronjobHistoryLog.put("sessionId", getSessionId());
            cronjobHistoryLog.put("beginTime", LocalDateTime.now());
            try {
                Executors.newFixedThreadPool(getPoolSize()).submit(getTask()).get();
                cronjobHistoryLog.put("executeResult", "SUCCESS");
            } catch (Exception e) {
                cronjobHistoryLog.put("executeResult", "EXCEPTION: " + e.getMessage());
                setCronjobStatus(currentCronjobStatus);
            }
            cronjobHistoryLog.put("endTime", LocalDateTime.now());
            Map<String, Object> cronjobHistoryLogMap = cronjobManagementRepository.getLatestCronjobHistoryLog(getCronjobName());
            if ("START JOB MANUALLY".equals(cronjobHistoryLogMap.get("operation")) && Objects.isNull(cronjobHistoryLogMap.get("executeResult"))) { // Start job manually case
                cronjobHistoryLog.put("id", cronjobHistoryLogMap.get("id"));
                cronjobManagementRepository.updateCronjobHistoryLog(cronjobHistoryLog);
            } else {
                cronjobHistoryLog.put("operation", "RUN JOB ON A SCHEDULE");
                cronjobHistoryLog.put("executedBy", "SYSTEM");
                cronjobManagementRepository.insertCronjobHistoryLog(cronjobHistoryLog);
            }
            setCronjobStatus(currentCronjobStatus);
        };
    }

    public void schedule(String updatedExpression) {
        if (StringUtils.hasText(updatedExpression) && !updatedExpression.equals(expression)) setExpression(updatedExpression);
        scheduledFuture = taskScheduler.schedule(cronjobExecutor, new CronTrigger(expression));
        setCronjobStatus("SCHEDULED");
    }

    public void cancel() { // How to kill my task?
    }

    public void forceStart() {
        taskScheduler.execute(cronjobExecutor);
    }

    public void setCronjobStatus(String cronjobStatus) {
        this.cronjobStatus = cronjobStatus;
        cronjobManagementRepository.updateCronjobStatus(cronjobStatus, getCronjobName());
    }

    public String getCronjobStatus() {
        // Use for multiple instances
        String currentCronjobStatus = cronjobManagementRepository.getCronjobStatus(cronjobName);
        if (!currentCronjobStatus.equals(cronjobStatus)) setCronjobStatus(currentCronjobStatus);
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
