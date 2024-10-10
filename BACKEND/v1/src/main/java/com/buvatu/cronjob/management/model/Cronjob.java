package com.buvatu.cronjob.management.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;

@Getter @Setter @Slf4j
public abstract class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();
    private String sessionId;
    private String expression;
    private CronjobStatus currentStatus;
    private Integer poolSize = 5; // Default pool_size = 5
    @JsonIgnore
    private boolean interrupted = false; // Mark a flag to interrupt task when need
    @JsonIgnore
    private Runnable task;
    @JsonIgnore
    private ScheduledFuture<?> future;

    private final ThreadPoolTaskScheduler taskScheduler;
    private final CronjobManagementRepository cronjobManagementRepository; // helper

    protected Cronjob(CronjobManagementRepository cronjobManagementRepository, ThreadPoolTaskScheduler taskScheduler) {
        this.cronjobManagementRepository = cronjobManagementRepository;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    private void initialize() {
        loadConfig();
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
        setTask(this::execute);
    }

    private void loadConfig() {
        Map<String, Object> cronjobConfigMap = cronjobManagementRepository.getCronjobConfig(getCronjobName());
        poolSize = (Integer) cronjobConfigMap.get("poolSize");
        expression = (String) cronjobConfigMap.get("expression");
    }

    public void schedule(String updatedExpression) {
        if (StringUtils.hasText(updatedExpression) && !updatedExpression.equals(expression)) setExpression(updatedExpression);
        future = taskScheduler.schedule(this::executeTask, new CronTrigger(expression));
        setCurrentStatus(CronjobStatus.SCHEDULED);
        insertCronjobHistoryLog("SCHEDULE JOB");
    }

    public void cancel() {
        future.cancel(true);
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
        insertCronjobHistoryLog("CANCEL JOB");
    }

    public void forceStart() {
        taskScheduler.execute(this::executeTask);
    }

    public void forceStop() {
        setInterrupted(true);
    }

    public void setCurrentStatus(CronjobStatus status) {
        this.currentStatus = status;
        cronjobManagementRepository.updateCronjobStatus(status.name(), getCronjobName());
    }

    public CronjobStatus getCurrentStatus() {
        // Use for multiple instances
        String currentCronjobStatus = cronjobManagementRepository.getCronjobStatus(cronjobName);
        if (!currentCronjobStatus.equals(currentStatus.name())) setCurrentStatus(CronjobStatus.valueOf(currentCronjobStatus));
        return currentStatus;
    }

    public boolean isInterrupted() {
        boolean interrupted = cronjobManagementRepository.isCronjobInteruppted(cronjobName);
        if (interrupted != this.interrupted) setInterrupted(interrupted);
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
        cronjobManagementRepository.updateCronjobInterupptedStatus(interrupted, getCronjobName());
    }

    public void setExpression(String expression) {
        this.expression = expression;
        cronjobManagementRepository.updateCronjobExpression(expression, cronjobName);
    }

    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
        cronjobManagementRepository.updateCronjobPoolSize(poolSize, cronjobName);
        insertCronjobHistoryLog("UPDATE POOL SIZE TO " + poolSize);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        cronjobManagementRepository.updateCronjobSessionId(sessionId, cronjobName);
    }

    private void executeTask() {
        CronjobStatus currentCronjobStatus = getCurrentStatus();
        if (CronjobStatus.RUNNING.equals(currentCronjobStatus)) {
            return;
        }
        setInterrupted(false);
        setCurrentStatus(CronjobStatus.RUNNING);
        setSessionId(UUID.randomUUID().toString());
        LocalDateTime startTime = LocalDateTime.now();
        String operation = Objects.isNull(RequestContextHolder.getRequestAttributes()) ? "RUN JOB ON A SCHEDULE" : "START JOB MANUALLY";
        String executor = "RUN JOB ON A SCHEDULE".equals(operation) ? "SYSTEM" : getExecutor();
        // default is success
        String executeResult = "SUCCESS";
        try (ExecutorService executorService = Executors.newFixedThreadPool(getPoolSize())){
            executorService.submit(task);
        } catch (Exception e) {
            log.error("Failed to execute job {}. Caused by: {}", cronjobName, e.getMessage());
            executeResult = "EXCEPTION: " + e.getMessage();
        }
        if (interrupted) executeResult = "INTERRUPTED";
        cronjobManagementRepository.insertCronjobHistoryLog(cronjobName, sessionId, startTime, operation, executor, executeResult);
        setCurrentStatus(currentCronjobStatus);
    }

    private String getExecutor() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(servletRequestAttributes)) return "SYSTEM";
        String executor = servletRequestAttributes.getRequest().getParameter("X-Username");
        return StringUtils.hasText(executor) ? executor : "SYSTEM";
    }

    private void insertCronjobHistoryLog(String operation) {
        cronjobManagementRepository.insertCronjobHistoryLog(cronjobName, sessionId, LocalDateTime.now(), operation, getExecutor(), "SUCCESS");
    }

    protected void insertTracingLog(String activityName, Integer progressValue) {
        cronjobManagementRepository.insertTracingLog(cronjobName, sessionId, activityName, progressValue);
    }

    protected abstract void execute();

    public List<Map<String, Object>> getChangeHistoryList() {
        return cronjobManagementRepository.getChangeHistoryLogList(cronjobName);
    }

    public List<Map<String, Object>> getTracingLogList(String sessionId) {
        return cronjobManagementRepository.getWorkflowLogList(sessionId);
    }

}
