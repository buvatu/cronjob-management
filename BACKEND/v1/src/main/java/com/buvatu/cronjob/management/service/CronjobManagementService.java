package com.buvatu.cronjob.management.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.buvatu.cronjob.management.model.Cronjob;

@Service
public class CronjobManagementService {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CronjobManagementRepository cronjobManagementRepository;

    public void schedule(String cronjobName, String updatedExpression) {
        if (!StringUtils.hasText(cronjobName)) return;
        context.getBean(cronjobName, Cronjob.class).schedule(updatedExpression);
    }

    public void cancelSchedule(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) return;
        context.getBean(cronjobName, Cronjob.class).cancel();
    }

    public void forceStart(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) return;
        context.getBean(cronjobName, Cronjob.class).start();
    }

    public void forceStop(String cronjobName) {
        if (!StringUtils.hasText(cronjobName)) return;
        context.getBean(cronjobName, Cronjob.class).stop();
    }

    @EventListener(classes = ApplicationReadyEvent.class)
    public void onApplicationReady() { // Load all cronjob config when ready
        List<Map<String, Object>> cronjobConfigList = cronjobManagementRepository.getCronjobConfigList();
        if (Objects.isNull(cronjobConfigList)) return;
        for (Map<String, Object> cronjobConfigMap : cronjobConfigList) {
            String cronjobName = (String) cronjobConfigMap.get("cronjob_name");
            Cronjob cronjob = context.getBean(cronjobName, Cronjob.class);
            cronjob.setExpression((String) cronjobConfigMap.get("cronjob_expression"));
            cronjob.setPoolSize((Integer) cronjobConfigMap.get("cronjob_pool_size"));
        }
    }

    public List<Map<String, Object>> getLatestLogList() {
        return cronjobManagementRepository.getCronjobConfigList();
    }
}
