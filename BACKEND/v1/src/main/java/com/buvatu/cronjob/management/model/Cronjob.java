package com.buvatu.cronjob.management.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

@Slf4j
public abstract class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();
    private String sessionId;
    private String expression;
    private CronjobStatus status;
    private Integer poolSize = 5; // Default pool_size = 5
    private String executor = CronjobConstant.SYSTEM;
    private String description;
    private ScheduledFuture<?> future;

    public final String getCronjobName() {
        return cronjobName;
    }

    public String getSessionId() {
        String currentSessionId = cronjobManagementRepository.getCronjobCurrentSessionId(cronjobName);
        if (!Objects.equals(sessionId, currentSessionId)) {
            sessionId = currentSessionId;
        }
        return sessionId;
    }

    public void setSessionId(String newSessionId) {
        if (Objects.equals(getSessionId(), newSessionId)) {
            return;
        }
        sessionId = newSessionId;
        cronjobManagementRepository.updateCronjobSessionId(sessionId, cronjobName);
    }

    public String getExpression() {
        String currentExpression = cronjobManagementRepository.getCronjobExpression(cronjobName);
        if (!Objects.equals(expression, currentExpression)) {
            expression = currentExpression;
        }
        return expression;
    }

    public void setExpression(String updatedExpression) {
        if (Objects.equals(getExpression(), updatedExpression)) {
            return;
        }
        expression = updatedExpression;
        cronjobManagementRepository.updateCronjobExpression(expression, cronjobName);
        insertCronjobChangeHistoryLog("UPDATE CRONJOB EXPRESSION TO " + expression, null);
    }

    public CronjobStatus getCurrentStatus() {
        CronjobStatus currentStatus = cronjobManagementRepository.getCronjobStatus(cronjobName);
        if (!Objects.equals(currentStatus, status)) {
            status = currentStatus;
        }
        return status;
    }

    public void setCurrentStatus(CronjobStatus updatedStatus) {
        if (Objects.equals(getCurrentStatus(), updatedStatus)) {
            return;
        }
        status = updatedStatus;
        cronjobManagementRepository.updateCronjobStatus(status.name(), cronjobName);
    }

    public Integer getPoolSize() {
        Integer currentPoolSize = cronjobManagementRepository.getCronjobPoolSize(cronjobName);
        if (!Objects.equals(currentPoolSize, poolSize)) {
            poolSize = currentPoolSize;
        }
        return poolSize;
    }

    public void setPoolSize(Integer updatedPoolSize) {
        if (Objects.equals(getPoolSize(), updatedPoolSize)) {
            return;
        }
        poolSize = updatedPoolSize;
        cronjobManagementRepository.updateCronjobPoolSize(poolSize, cronjobName);
        insertCronjobChangeHistoryLog("UPDATE POOL SIZE TO " + poolSize, null);
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private final ThreadPoolTaskScheduler taskScheduler;
    private final CronjobManagementRepository cronjobManagementRepository; // helper

    protected Cronjob(CronjobManagementRepository cronjobManagementRepository, ThreadPoolTaskScheduler taskScheduler) {
        this.cronjobManagementRepository = cronjobManagementRepository;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    private void initialize() {
        if (!cronjobManagementRepository.isCronjobExist(cronjobName)) {
            cronjobManagementRepository.insertCronjobConfig(cronjobName);
            status = CronjobStatus.UNSCHEDULED;
        } else {
            poolSize = cronjobManagementRepository.getCronjobPoolSize(cronjobName);
            expression = cronjobManagementRepository.getCronjobExpression(cronjobName);
            setCurrentStatus(CronjobStatus.UNSCHEDULED);
        }
    }

    public void schedule() {
        future = taskScheduler.schedule(this::executeTask, new CronTrigger(expression));
        setCurrentStatus(CronjobStatus.SCHEDULED);
        insertCronjobChangeHistoryLog("SCHEDULE JOB", description);
    }

    public void cancel() {
        future.cancel(true);
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
        insertCronjobChangeHistoryLog("CANCEL JOB", description);
    }

    public void forceStart() {
        future = taskScheduler.schedule(this::executeTask, Instant.now().plusSeconds(1)); // Start after 1s
    }

    public void forceStop() {
        future.cancel(true);
    }

    private void executeTask() {
        CronjobStatus cronjobStatusBeforeRunning = getCurrentStatus();
        if (CronjobStatus.RUNNING.equals(cronjobStatusBeforeRunning)) {
            throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        }
        setCurrentStatus(CronjobStatus.RUNNING);
        setSessionId(UUID.randomUUID().toString());
        LocalDateTime startTime = LocalDateTime.now();
        String operation = Objects.equals(executor, CronjobConstant.SYSTEM) ? "RUN JOB ON A SCHEDULE" : "START JOB MANUALLY";

        // You have to define the executionResult by your own
        try (ExecutorService executorService = Executors.newFixedThreadPool(getPoolSize())) {
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), startTime, executor, operation, executorService.submit(this::getExecutionResult).get(), description);
        } catch (InterruptedException | ExecutionException | CancellationException e) { // Interrupted
            log.error("Cronjob {} has been interrupted. Caused by: {}", cronjobName, e.getMessage());
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), startTime, executor, operation, "INTERRUPTED", description);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed to execute cronjob {}. Caused by: {}", cronjobName, e.getMessage());
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), startTime, executor, operation, "EXCEPTION", e.getMessage());
        } finally {
            setCurrentStatus(cronjobStatusBeforeRunning);
            if (CronjobStatus.SCHEDULED.equals(cronjobStatusBeforeRunning)) {
                schedule();
            }
        }
    }

    private void insertCronjobChangeHistoryLog(String operation, String description) {
        cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), LocalDateTime.now(), executor, operation, "SUCCESS", description);
    }

    protected void insertTracingLog(String activityName, Integer progressValue) {
        cronjobManagementRepository.insertTracingLog(cronjobName, getSessionId(), activityName, progressValue);
    }

    protected boolean isInterrupted() {
        return Objects.equals(getCurrentStatus(), CronjobStatus.INTERRUPTED);
    }

    protected abstract String getExecutionResult();

    public List<Map<String, Object>> getChangeHistoryList() {
        return cronjobManagementRepository.getCronjobChangeHistoryLogList(cronjobName);
    }

    public List<Map<String, Object>> getTracingLogList(String sessionId) {
        return cronjobManagementRepository.getCronjobRunningLogList(sessionId);
    }

    public LocalDateTime getLastExecutionTime() {
        return cronjobManagementRepository.getLastExecutionTime(cronjobName);
    }
}
