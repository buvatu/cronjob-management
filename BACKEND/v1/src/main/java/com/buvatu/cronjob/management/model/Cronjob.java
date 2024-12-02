package com.buvatu.cronjob.management.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;

@Slf4j
public abstract class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();
    private String sessionId;
    private String expression;
    private CronjobStatus status;
    private Integer poolSize = 5; // Default pool_size = 5
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
        insertCronjobChangeHistoryLog("UPDATE CRONJOB EXPRESSION TO " + expression);
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
        insertCronjobChangeHistoryLog("UPDATE POOL SIZE TO " + poolSize);
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
            return;
        }
        poolSize = cronjobManagementRepository.getCronjobPoolSize(cronjobName);
        expression = cronjobManagementRepository.getCronjobExpression(cronjobName);
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
    }

    public void schedule(String updatedExpression) {
        if (StringUtils.hasText(updatedExpression) && !updatedExpression.equals(expression)) setExpression(updatedExpression);
        future = taskScheduler.schedule(this::executeTask, new CronTrigger(expression));
        setCurrentStatus(CronjobStatus.SCHEDULED);
        insertCronjobChangeHistoryLog("SCHEDULE JOB");
    }

    public void cancel() {
        future.cancel(true);
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
        insertCronjobChangeHistoryLog("CANCEL JOB");
    }

    public void forceStart() {
        taskScheduler.execute(this::executeTask);
    }

    public void forceStop() {
        setCurrentStatus(CronjobStatus.INTERRUPTED);
    }

    private void executeTask() {
        CronjobStatus cronjobStatusBeforeRunning = getCurrentStatus();
        if (CronjobStatus.RUNNING.equals(cronjobStatusBeforeRunning)) {
            throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        }
        setCurrentStatus(CronjobStatus.RUNNING);
        setSessionId(UUID.randomUUID().toString());
        LocalDateTime startTime = LocalDateTime.now();
        String operation = Objects.isNull(RequestContextHolder.getRequestAttributes()) ? "RUN JOB ON A SCHEDULE" : "START JOB MANUALLY";
        String executor = "RUN JOB ON A SCHEDULE".equals(operation) ? CronjobConstant.SYSTEM : getExecutor();

        try (ExecutorService executorService = Executors.newFixedThreadPool(getPoolSize())){
            executorService.submit(this::execute);
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, sessionId, startTime, operation, executor, CronjobStatus.INTERRUPTED.equals(getCurrentStatus()) ? "INTERRUPTED" : "SUCCESS");
        } catch (Exception e) {
            cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, sessionId, startTime, operation, executor, "EXCEPTION: " + e.getMessage());
            log.error("Failed to execute job {}. Caused by: {}", cronjobName, e.getMessage());
        }

        setCurrentStatus(cronjobStatusBeforeRunning);
    }

    private String getExecutor() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(servletRequestAttributes)) return CronjobConstant.SYSTEM;
        String executor = servletRequestAttributes.getRequest().getParameter("X-Username");
        return StringUtils.hasText(executor) ? executor : CronjobConstant.SYSTEM;
    }

    private void insertCronjobChangeHistoryLog(String operation) {
        cronjobManagementRepository.insertCronjobChangeHistoryLog(cronjobName, sessionId, LocalDateTime.now(), operation, getExecutor(), "SUCCESS");
    }

    protected void insertTracingLog(String activityName, Integer progressValue) {
        cronjobManagementRepository.insertTracingLog(cronjobName, sessionId, activityName, progressValue);
    }

    protected boolean isInterrupted() {
        return Objects.equals(getCurrentStatus(), CronjobStatus.INTERRUPTED);
    }

    protected abstract void execute();

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
