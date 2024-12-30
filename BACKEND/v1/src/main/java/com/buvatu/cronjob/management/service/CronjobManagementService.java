package com.buvatu.cronjob.management.service;

import java.util.*;

import com.buvatu.cronjob.management.model.BusinessException;

import com.buvatu.cronjob.management.model.CronjobConstant;
import com.buvatu.cronjob.management.model.CronjobStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.buvatu.cronjob.management.model.Cronjob;

@Service
public class CronjobManagementService {

    private final List<Cronjob> cronjobList;

    public CronjobManagementService(List<Cronjob> cronjobList) {
        this.cronjobList = cronjobList;
    }

    public void updatePoolSize(String cronjobName, Integer poolSize, String executor, String description) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(poolSize) || poolSize < 1 || poolSize.equals(cronjob.getPoolSize())) throw new BusinessException(400, CronjobConstant.POOL_SIZE_IS_NOT_VALID);
        if (!CronjobStatus.UNSCHEDULED.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_MUST_BE_UNSCHEDULED, cronjobName));
        cronjob.setExecutor(executor);
        cronjob.setDescription("PREVIOUS POOL SIZE: " + cronjob.getPoolSize() + "--> UPDATED POOL SIZE: " + poolSize + ". REASON: " + description);
        cronjob.setPoolSize(poolSize);
    }

    public void updateExpression(String cronjobName, String expression, String executor, String description) {
        if (!CronExpression.isValidExpression(expression)) throw new BusinessException(400, CronjobConstant.EXPRESSION_IS_NOT_VALID);
        Cronjob cronjob = getCronjob(cronjobName);
        if (!CronjobStatus.UNSCHEDULED.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_MUST_BE_UNSCHEDULED, cronjobName));
        cronjob.setExecutor(executor);
        cronjob.setDescription("PREVIOUS CRON EXPRESSION: " + cronjob.getExpression() + "--> UPDATED CRON EXPRESSION: " + expression + ". REASON: " + description);
        cronjob.setExpression(expression);
    }

    public void schedule(String cronjobName, String executor, String description) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (!CronjobStatus.UNSCHEDULED.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_MUST_BE_UNSCHEDULED, cronjobName));
        if (!CronExpression.isValidExpression(cronjob.getExpression())) throw new BusinessException(400, CronjobConstant.EXPRESSION_IS_NOT_VALID);
        if (Objects.isNull(cronjob.getPoolSize()) || cronjob.getPoolSize() < 1) throw new BusinessException(400, CronjobConstant.POOL_SIZE_IS_NOT_VALID);
        cronjob.setExecutor(executor);
        cronjob.setDescription(description);
        cronjob.schedule();
    }

    public void cancel(String cronjobName, String executor, String description) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        if (CronjobStatus.UNSCHEDULED.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_ALREADY_UNSCHEDULED, cronjobName));
        cronjob.setExecutor(executor);
        cronjob.setDescription(description);
        cronjob.cancel();
    }

    public void forceStart(String cronjobName, String executor, String description) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        cronjob.setExecutor(executor);
        cronjob.setDescription(description);
        cronjob.forceStart();
    }

    public void forceStop(String cronjobName, String executor, String description) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (!CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_NOT_RUNNING, cronjobName));
        cronjob.setExecutor(executor);
        cronjob.setDescription(description);
        cronjob.forceStop();
    }

    public List<Map<String, Object>> getChangeHistoryList(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        return cronjob.getChangeHistoryList();
    }

    public List<Map<String, Object>> getTracingLogList(String cronjobName, String sessionId) {
        Cronjob cronjob = getCronjob(cronjobName);
        return cronjob.getTracingLogList(sessionId);
    }

    private Cronjob getCronjob(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_IS_NOT_FOUND, cronjobName));
        Cronjob cronjob = cronjobList.stream().filter(e -> e.getCronjobName().equals(cronjobName)).findFirst().orElse(null);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_IS_NOT_FOUND, cronjobName));
        return cronjob;
    }

    public List<Map<String, Object>> getAllCronjob() {
        return cronjobList.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put(CronjobConstant.CRONJOB_NAME, e.getCronjobName());
            map.put(CronjobConstant.EXPRESSION, e.getExpression());
            map.put(CronjobConstant.POOL_SIZE, e.getPoolSize());
            map.put(CronjobConstant.LAST_EXECUTION_TIME, e.getLastExecutionTime());
            return map;
        }).toList();
    }

}
