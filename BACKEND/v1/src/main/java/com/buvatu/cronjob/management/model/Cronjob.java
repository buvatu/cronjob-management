package com.buvatu.cronjob.management.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;

@Slf4j
public abstract class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();
    private String sessionId;
    private String expression;
    private CronjobStatus status;
    private Integer poolSize = 5; // Default pool_size = 5
    @Setter
    private String executor = CronjobConstant.SYSTEM;
    @Setter
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
        cronjobManagementRepository.updateCronjobSessionId(cronjobName, sessionId);
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
        cronjobManagementRepository.updateCronjobExpression(cronjobName, expression, executor);
        insertCronjobChangeHistoryLog(Operation.UPDATE_CRON_EXPRESSION, description);
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
        cronjobManagementRepository.updateCronjobStatus(cronjobName, status.name());
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
        cronjobManagementRepository.updateCronjobPoolSize(cronjobName, poolSize, executor);
        insertCronjobChangeHistoryLog(Operation.UPDATE_POOL_SIZE, description);
    }

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;
    @Autowired
    private CronjobManagementRepository cronjobManagementRepository; // helper

//    protected Cronjob(CronjobManagementRepository cronjobManagementRepository, ThreadPoolTaskScheduler taskScheduler) {
//        this.cronjobManagementRepository = cronjobManagementRepository;
//        this.taskScheduler = taskScheduler;
//    }

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
        insertCronjobChangeHistoryLog(Operation.SCHEDULE_JOB, description);
    }

    public void cancel() {
        future.cancel(true);
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
        insertCronjobChangeHistoryLog(Operation.CANCEL_JOB, description);
    }

    public void forceStart() {
        taskScheduler.submit(this::executeTask);
    }

//    public void forceStop() {
//        future.cancel(true);
//    }

    private void executeTask() {
        CronjobStatus cronjobStatusBeforeRunning = getCurrentStatus();
        if (CronjobStatus.RUNNING.equals(cronjobStatusBeforeRunning)) {
            throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        }
        setCurrentStatus(CronjobStatus.RUNNING);
        setSessionId(UUID.randomUUID().toString());
        LocalDateTime startTime = LocalDateTime.now();
        Operation operation = Objects.equals(executor, CronjobConstant.SYSTEM) ? Operation.EXECUTE_JOB_ON_A_SCHEDULE : Operation.START_JOB_MANUALLY;

        try (ExecutorService executorService = Executors.newFixedThreadPool(getPoolSize())) {
            executorService.submit(this::getExecutionResult).get();
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), startTime, executor, operation, ExecutionResult.SUCCESS, null);
        } catch (InterruptedException e) { // Interrupted
            log.error("Cronjob {} has been interrupted. Caused by: {}", cronjobName, e.getMessage());
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), startTime, executor, operation, ExecutionResult.INTERRUPTED, description);
        } catch (ExecutionException | CancellationException e) {
            log.error("Failed to execute cronjob {}. Caused by: {}", cronjobName, e.getMessage());
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), startTime, executor, operation, ExecutionResult.FAILED, e.getMessage());
        } finally {
            setCurrentStatus(cronjobStatusBeforeRunning);
            setExecutor(CronjobConstant.SYSTEM);
        }
    }

    private void insertCronjobChangeHistoryLog(Operation operation, String description) {
        cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, getSessionId(), LocalDateTime.now(), executor, operation, ExecutionResult.SUCCESS, description);
    }

    protected void insertTracingLog(String activityName, Integer progressValue) {
        cronjobManagementRepository.insertTracingLog(cronjobName, getSessionId(), activityName, progressValue);
    }

    protected abstract String getExecutionResult();

    public List<Map<String, Object>> getChangeHistoryList() {
        return cronjobManagementRepository.getCronjobChangeHistoryLogList(cronjobName);
    }

    public List<Map<String, Object>> getAllRunningHistory() {
        return cronjobManagementRepository.getAllRunningHistory(cronjobName);
    }

    public List<Map<String, Object>> getTracingLogList(String sessionId) {
        return cronjobManagementRepository.getCronjobRunningLogList(sessionId);
    }

    public LocalDateTime getLastExecutionTime() {
        return cronjobManagementRepository.getLastExecutionTime(cronjobName);
    }
}
