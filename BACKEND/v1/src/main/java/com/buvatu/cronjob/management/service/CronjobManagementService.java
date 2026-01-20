package com.buvatu.cronjob.management.service;

import java.util.*;

import com.buvatu.cronjob.management.entity.JobExecution;
import com.buvatu.cronjob.management.entity.JobExecutionLog;
import com.buvatu.cronjob.management.entity.JobOperation;
import com.buvatu.cronjob.management.exception.BusinessException;

import com.buvatu.cronjob.management.constant.CronjobConstant;
import com.buvatu.cronjob.management.model.Cronjob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CronjobManagementService {

    private final List<Cronjob> cronjobList;

    public CronjobManagementService(List<Cronjob> cronjobList) {
        this.cronjobList = cronjobList;
    }

    public void updatePoolSize(String cronjobName, Integer poolSize, String executor, String description,
            Long version) {
        getCronjob(cronjobName).updatePoolSize(executor, description, poolSize, version);
    }

    public void updateExpression(String cronjobName, String expression, String executor, String description,
            Long version) {
        getCronjob(cronjobName).updateExpression(executor, description, expression, version);
    }

    public void schedule(String cronjobName, String executor, String description) {
        getCronjob(cronjobName).schedule(executor, description);
    }

    public void cancel(String cronjobName, String executor, String description) {
        getCronjob(cronjobName).cancel(executor, description);
    }

    public void forceStart(String cronjobName, String executor, String description) {
        getCronjob(cronjobName).forceStart(executor, description);
    }

    public void forceStop(String cronjobName, String executor, String description) {
        getCronjob(cronjobName).forceStop(executor, description);
    }

    public List<JobOperation> getChangeHistoryList(String cronjobName) {
        return getCronjob(cronjobName).getChangeHistoryList();
    }

    public Page<JobOperation> getPagedChangeHistory(String cronjobName, Pageable pageable) {
        return getCronjob(cronjobName).getPagedChangeHistoryList(pageable);
    }

    public List<JobExecutionLog> getTracingLogList(String cronjobName, UUID sessionId) {
        return getCronjob(cronjobName).getTracingLogList(sessionId);
    }

    public List<JobExecution> getAllRunningHistory(String cronjobName) {
        return getCronjob(cronjobName).getAllRunningHistory();
    }

    public Page<JobExecution> getPagedRunningHistory(String cronjobName, Pageable pageable) {
        return getCronjob(cronjobName).getPagedRunningHistory(pageable);
    }

    private Cronjob getCronjob(String cronjobName) {
        if (!StringUtils.hasText(cronjobName))
            throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName));
        Cronjob cronjob = cronjobList.stream().filter(e -> e.getCronjobName().equals(cronjobName)).findFirst()
                .orElse(null);
        if (Objects.isNull(cronjob))
            throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName));
        return cronjob;
    }

    public List<String> getAllCronjob() {
        return cronjobList.stream().map(Cronjob::getCronjobName).toList();
    }

    public Map<String, Object> getJobDetail(String cronjobName) {
        Map<String, Object> map = new HashMap<>();
        Cronjob e = getCronjob(cronjobName);
        map.put(CronjobConstant.CRONJOB_NAME, cronjobName);
        map.put(CronjobConstant.EXPRESSION, e.getConfig().getExpression());
        map.put(CronjobConstant.POOL_SIZE, e.getConfig().getPoolSize());
        map.put(CronjobConstant.VERSION, e.getConfig().getVersion());
        map.put(CronjobConstant.LAST_EXECUTION_TIME, e.getLastExecutionTime());
        return map;
    }
}
