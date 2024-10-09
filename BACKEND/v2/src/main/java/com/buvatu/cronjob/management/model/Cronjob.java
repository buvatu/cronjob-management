package com.buvatu.cronjob.management.model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
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
public class Cronjob {

    private String cronjobName;
    private String sessionId;
    private String expression;
    private CronjobStatus currentStatus;
    private boolean interrupted = false; // Mark a flag to interrupt task when need

    private final CronjobManagementRepository cronjobManagementRepository; // helper

    public Cronjob(CronjobManagementRepository cronjobManagementRepository) {
        this.cronjobManagementRepository = cronjobManagementRepository;
    }

    @PostConstruct
    private void initialize() {
        loadConfig();
    }

    private void loadConfig() {
        Map<String, Object> cronjobConfigMap = cronjobManagementRepository.getCronjobConfig(getCronjobName());
        poolSize = (Integer) cronjobConfigMap.get("poolSize");
        expression = (String) cronjobConfigMap.get("expression");
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
    }

    public void schedule(String updatedExpression) {
        if (StringUtils.hasText(updatedExpression) && !updatedExpression.equals(expression)) setExpression(updatedExpression);
        taskScheduler.schedule(this::executeTask, new CronTrigger(expression));
        setCurrentStatus(CronjobStatus.SCHEDULED);
        cronjobManagementRepository.insertCronjobHistoryLog(getCronjobHistoryLog("SCHEDULE JOB"));
    }

    public void cancel() {
        taskScheduler.getScheduledThreadPoolExecutor().getQueue().clear();
        setCurrentStatus(CronjobStatus.UNSCHEDULED);
        cronjobManagementRepository.insertCronjobHistoryLog(getCronjobHistoryLog("CANCEL JOB"));
    }

    public void forceStart() {
        taskScheduler.execute(this::executeTask);
    }

    public void forceStop() {
        interrupted = true;
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

    public void setExpression(String expression) {
        this.expression = expression;
        cronjobManagementRepository.updateCronjobExpression(expression, cronjobName);
    }

    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
        cronjobManagementRepository.updateCronjobPoolSize(poolSize, cronjobName);
        cronjobManagementRepository.insertCronjobHistoryLog(getCronjobHistoryLog("UPDATE POOL SIZE TO " + poolSize));
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
        cronjobManagementRepository.updateCronjobSessionId(sessionId, cronjobName);
    }

    private void executeTask() {
        CronjobStatus currentCronjobStatus = getCurrentStatus();
        setInterrupted(false);
        setCurrentStatus(CronjobStatus.RUNNING);
        setSessionId(UUID.randomUUID().toString());

        // First, get initialize Activity

        // Second, call Activity to get result

        while (Objects.nonNull(1)) {
            Activity
        }

        Map<String, Object> cronjobHistoryLog = new HashMap<>();
        cronjobHistoryLog.put("cronjobName", getCronjobName());
        cronjobHistoryLog.put("sessionId", getSessionId());
        cronjobHistoryLog.put("beginTime", LocalDateTime.now());

        try (ExecutorService executorService = Executors.newFixedThreadPool(getPoolSize())){
            executorService.submit(task);
            cronjobHistoryLog.put("executeResult", isInterrupted() ? "INTERRUPTED" : "SUCCESS");
        } catch (Exception e) {
            cronjobHistoryLog.put("executeResult", "EXCEPTION: " + e.getMessage());
        }

        cronjobHistoryLog.put("endTime", LocalDateTime.now());
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        cronjobHistoryLog.put("operation", Objects.isNull(servletRequestAttributes) ? "RUN JOB ON A SCHEDULE" : "START JOB MANUALLY");
        cronjobHistoryLog.put("executedBy", getExecutor());
        cronjobManagementRepository.insertCronjobHistoryLog(cronjobHistoryLog);
        setCurrentStatus(currentCronjobStatus);
    }

    private String getExecutor() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(servletRequestAttributes)) return "SYSTEM";
        String executor = servletRequestAttributes.getRequest().getParameter("X-Username");
        return StringUtils.hasText(executor) ? executor : "SYSTEM";
    }

    private Map<String, Object> getCronjobHistoryLog(String operation) {
        Map<String, Object> cronjobHistoryLog = new HashMap<>();
        cronjobHistoryLog.put("cronjobName", cronjobName);
        cronjobHistoryLog.put("sessionId", UUID.randomUUID().toString());
        cronjobHistoryLog.put("beginTime", LocalDateTime.now());
        cronjobHistoryLog.put("endTime", LocalDateTime.now());
        cronjobHistoryLog.put("operation", operation);
        cronjobHistoryLog.put("executedBy", getExecutor());
        cronjobHistoryLog.put("executeResult", "SUCCESS");
        setSessionId(cronjobHistoryLog.get("sessionId").toString());
        return cronjobHistoryLog;
    }

}
