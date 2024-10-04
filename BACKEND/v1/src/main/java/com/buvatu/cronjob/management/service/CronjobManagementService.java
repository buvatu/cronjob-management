package com.buvatu.cronjob.management.service;

import java.time.LocalDateTime;
import java.util.*;

import com.buvatu.cronjob.management.model.BusinessException;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.buvatu.cronjob.management.model.Cronjob;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CronjobManagementService {

    private final List<Cronjob> cronjobList;

    private final CronjobManagementRepository cronjobManagementRepository;

    public CronjobManagementService(List<Cronjob> cronjobList, CronjobManagementRepository cronjobManagementRepository) {
        this.cronjobList = cronjobList;
        this.cronjobManagementRepository = cronjobManagementRepository;
    }

    public void schedule(String cronjobName, @Nullable String updatedExpression) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        if (StringUtils.hasText(updatedExpression) && !CronExpression.isValidExpression(updatedExpression)) throw new BusinessException(400, "Expression is not valid");
        if (!StringUtils.hasText(cronjob.getExpression()) && !StringUtils.hasText(updatedExpression)) throw new BusinessException(400, "Expression is not valid");
        if ("RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        cronjob.schedule(updatedExpression);
        cronjobManagementRepository.insertCronjobHistoryLog(getCronjobHistoryLog(cronjobName, "SCHEDULE JOB"));
    }

    public void postpone(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        if ("RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, "Cronjob is running");
        cronjob.setScheduledFuture(null);
        cronjob.setCronjobStatus("UNSCHEDULED");
        cronjobManagementRepository.insertCronjobHistoryLog(getCronjobHistoryLog(cronjobName, "POSTPONE JOB"));
    }

    public void cancel(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        if (!"RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, String.format("Cronjob %s is not running", cronjobName));
        cronjob.cancel();
        cronjobManagementRepository.insertCronjobHistoryLog(getCronjobHistoryLog(cronjobName, "CANCEL JOB"));
    }

    public void forceStart(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        if ("RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        Map<String, Object> cronjobHistoryLog = new HashMap<>();
        cronjobHistoryLog.put("cronjobName", cronjobName);
        cronjobHistoryLog.put("beginTime", LocalDateTime.now());
        cronjobHistoryLog.put("operation", "START JOB MANUALLY");
        cronjobHistoryLog.put("executedBy", getExecutor());
        cronjobManagementRepository.insertPartOfCronjobHistoryLog(cronjobHistoryLog);
        cronjob.forceStart();
    }

    public void updatePoolSize(String cronjobName, Integer poolSize) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        if (Objects.isNull(poolSize) || poolSize < 1 || poolSize.equals(cronjob.getPoolSize())) throw new BusinessException(400, "Pool size is not valid");
        if ("RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        cronjob.setPoolSize(poolSize);
        cronjob.initializeCronjobExecutor();
        cronjobManagementRepository.insertCronjobHistoryLog(getCronjobHistoryLog(cronjobName, "UPDATE POOL SIZE TO " + poolSize));
    }

    private Cronjob getCronjob(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        return cronjobList.stream().filter(e -> e.getCronjobName().equals(cronjobName)).findFirst().orElse(null);
    }

    private Map<String, Object> getCronjobHistoryLog(String cronjobName, String operation) {
        Map<String, Object> cronjobHistoryLog = new HashMap<>();
        cronjobHistoryLog.put("cronjobName", cronjobName);
        cronjobHistoryLog.put("sessionId", UUID.randomUUID().toString());
        cronjobHistoryLog.put("beginTime", LocalDateTime.now());
        cronjobHistoryLog.put("endTime", LocalDateTime.now());
        cronjobHistoryLog.put("operation", operation);
        cronjobHistoryLog.put("executedBy", getExecutor());
        cronjobHistoryLog.put("executeResult", "SUCCESS");
        return cronjobHistoryLog;
    }

    private String getExecutor() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(servletRequestAttributes)) return "SYSTEM";
        String executor = servletRequestAttributes.getRequest().getParameter("X-Username");
        return StringUtils.hasText(executor) ? executor : "SYSTEM";
    }

    public List<Map<String, Object>> getCronjobList() {
        return cronjobManagementRepository.getCronjobConfigList();
    }

    public List<Map<String, Object>> getActiveLogs() {
        return cronjobManagementRepository.getActiveLogs();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        cronjobList.stream().forEach(e -> {
            Map<String, Object> cronjobConfigMap = cronjobManagementRepository.getCronjobConfig(e.getCronjobName());
            e.setPoolSize((Integer) cronjobConfigMap.get("poolSize"));
            e.setExpression((String) cronjobConfigMap.get("expression"));
            e.setCronjobStatus("UNSCHEDULED");
        });
    }
}
