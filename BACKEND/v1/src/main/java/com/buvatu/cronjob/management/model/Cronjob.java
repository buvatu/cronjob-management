package com.buvatu.cronjob.management.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

@Getter @Setter
public class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();
    private String sessionId;
    private String expression;
    private String cronjobStatus;
    private ThreadPoolTaskScheduler taskScheduler;
    private Integer poolSize = 5; // Default pool_size = 5
    private Runnable cronjobExecutor;
    private Runnable task;
    private Future<?> future;

    private final CronjobManagementRepository cronjobManagementRepository; // helper

    public Cronjob(CronjobManagementRepository cronjobManagementRepository) {
        this.cronjobManagementRepository = cronjobManagementRepository;
    }

    @PostConstruct
    private void initialize() {
        initializeTaskScheduler();
        loadConfig();
        initializeCronjobExecutor();
    }

    private void initializeTaskScheduler() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("cronjob-" + cronjobName + "-");
        taskScheduler.initialize();
    }

    private void loadConfig() {
        Map<String, Object> cronjobConfigMap = cronjobManagementRepository.getCronjobConfig(getCronjobName());
        poolSize = (Integer) cronjobConfigMap.get("poolSize");
        expression = (String) cronjobConfigMap.get("expression");
        setCronjobStatus("UNSCHEDULED");
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
                future = Executors.newFixedThreadPool(getPoolSize()).submit(task);
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
        taskScheduler.schedule(cronjobExecutor, new CronTrigger(expression));
        setCronjobStatus("SCHEDULED");
    }

    public void cancel() { // How to kill my task?
        future.cancel(true);
    }

    public void forceStart() {
        future = Executors.newFixedThreadPool(getPoolSize()).submit(task);
        // taskScheduler.execute(cronjobExecutor);
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
