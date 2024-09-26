package com.buvatu.cronjob.management.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.buvatu.cronjob.management.model.BusinessException;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.buvatu.cronjob.management.model.Cronjob;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CronjobManagementService {

    @Autowired
    private List<Cronjob> cronjobList;

    @Autowired
    private CronjobManagementRepository cronjobManagementRepository;

    public void schedule(String cronjobName, String updatedExpression) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        if (!StringUtils.hasText(updatedExpression) || !CronExpression.isValidExpression(updatedExpression)) throw new BusinessException(400, "Expression is not valid");
        if ("RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        cronjob.schedule(updatedExpression);
    }

    public void cancel(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, String.format("Cronjob %s is not found", cronjobName));
        if (!"RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, String.format("Cronjob %s is not running", cronjobName));
        cronjob.cancel();
    }

    public void forceStart(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, "Cronjob not found");
        if ("RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, String.format("Cronjob %s is running", cronjobName));
        cronjob.forceStart();
    }

    public void postpone(String cronjobName) {
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.isNull(cronjob)) throw new BusinessException(404, "Cronjob not found");
        if ("RUNNING".equals(cronjob.getCronjobStatus())) throw new BusinessException(400, "Cronjob is running");
        cronjob.setFuture(null);
        cronjob.setCronjobStatus("UNSCHEDULED");
    }

    public void addNewCronjob(Map<String, Object> cronjobConfigMap) {
        String cronjobName = (String) cronjobConfigMap.get("cronjobName");
        Cronjob cronjob = getCronjob(cronjobName);
        if (Objects.nonNull(cronjob)) throw new BusinessException(409, "Cronjob already existed");
    }

    private Cronjob getCronjob(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) throw new BusinessException(404, "Cronjob not found");
        return cronjobList.stream().filter(e -> e.getCronjobName().equals(cronjobName)).findFirst().orElse(null);
    }

    private String getExecutor() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader("X-Username");
    }

    public List<Map<String, Object>> getCronjobList() {
        return cronjobManagementRepository.getCronjobConfigList();
    }

    public List<Map<String, Object>> getActiveLogs() {
        return cronjobManagementRepository.getActiveLogs();
    }
}
