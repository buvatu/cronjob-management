package com.buvatu.cronjob.management.service;

import java.util.*;

import com.buvatu.cronjob.management.model.BusinessException;

import com.buvatu.cronjob.management.model.CronjobConstant;
import com.buvatu.cronjob.management.model.CronjobStatus;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.buvatu.cronjob.management.model.Cronjob;

@Service
public class CronjobManagementService {

    private final List<Cronjob> cronjobList;
    private final CronjobManagementRepository cronjobManagementRepository;

    public CronjobManagementService(List<Cronjob> cronjobList, CronjobManagementRepository cronjobManagementRepository) {
        this.cronjobList = cronjobList;
        this.cronjobManagementRepository = cronjobManagementRepository;
    }

    public void schedule(String cronjobName, String updatedExpression) {
        if (StringUtils.hasText(updatedExpression) && !CronExpression.isValidExpression(updatedExpression)) throw new BusinessException(400, "Expression is not valid");
        Cronjob cronjob = getCronjob(cronjobName);
        if (!StringUtils.hasText(cronjob.getExpression()) && !StringUtils.hasText(updatedExpression)) throw new BusinessException(400, "Expression is not valid");
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        cronjob.schedule(updatedExpression);
    }

    public void cancel(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        if (CronjobStatus.UNSCHEDULED.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format("Cronjob %s is already unscheduled", cronjobName));
        cronjob.cancel();
    }

    public void forceStart(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (CronjobStatus.RUNNING.equals(cronjob.getCurrentStatus())) throw new BusinessException(400, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
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

    private Cronjob getCronjob(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        Cronjob cronjob = cronjobList.stream().filter(e -> e.getCronjobName().equals(cronjobName)).findFirst().orElse(null);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        return cronjob;
    }

    public List<Map<String, Object>> getAllCronjob() {

        return null;
    }

    @EventListener(ApplicationReadyEvent.class)
    private void onReady() {
        for (Cronjob cronjob : cronjobList) {
            if (cronjobManagementRepository.isCronjobExist(cronjob.getCronjobName())) {
                continue;
            }
            cronjobManagementRepository.insertCronjobConfig(cronjob.getCronjobName());
        }
    }

}
