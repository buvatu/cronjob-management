package com.buvatu.cronjob.management.service;

import java.util.*;

import com.buvatu.cronjob.management.model.BusinessException;

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

    public void schedule(String cronjobName, String updatedExpression) {
        if (StringUtils.hasText(updatedExpression) && !CronExpression.isValidExpression(updatedExpression)) throw new BusinessException(400, "Expression is not valid");
        Cronjob cronjob = getCronjob(cronjobName);
        if (!StringUtils.hasText(cronjob.getExpression()) && !StringUtils.hasText(updatedExpression)) throw new BusinessException(400, "Expression is not valid");
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        cronjob.schedule(updatedExpression);
    }

    public void cancel(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        if (CronjobStatus.UNSCHEDULED.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format("Cronjob %s is already unscheduled", cronjobName));
        cronjob.cancel();
    }

    public void forceStart(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        cronjob.forceStart();
    }

    public void forceStop(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (!CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format("Cronjob %s is not running", cronjobName));
        cronjob.forceStop();
    }

    public void updatePoolSize(String cronjobName, Integer poolSize) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(poolSize) || poolSize < 1 || poolSize.equals(cronjob.getPoolSize())) throw new BusinessException(400, "Pool size is not valid");
        if (!CronjobStatus.UNSCHEDULED.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format("Cronjob %s must be unscheduled", cronjobName));
        cronjob.setPoolSize(poolSize);
    }

    public List<Map<String, Object>> getChangeHistoryList(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        return cronjob.getChangeHistoryList();
    }

    public List<Map<String, Object>> getTracingLogList(String cronjobName, String sessionId) {
        Cronjob cronjob = getCronjob(cronjobName);
        return cronjob.getTracingLogList(sessionId);
    }

    public List<Cronjob> getCronjobList() {
        return cronjobList;
    }

    private Cronjob getCronjob(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        Cronjob cronjob = cronjobList.stream().filter(e -> e.getCronjobName().equals(cronjobName)).findFirst().orElse(null);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        return cronjob;
    }

}
