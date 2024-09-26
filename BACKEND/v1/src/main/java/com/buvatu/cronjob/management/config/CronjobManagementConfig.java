package com.buvatu.cronjob.management.config;

import com.buvatu.cronjob.management.model.Cronjob;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Configuration
public class CronjobManagementConfig {

    @Autowired
    private CronjobManagementRepository cronjobManagementRepository;

    @Autowired
    private ApplicationContext context;

    @Bean
    List<Cronjob> cronjobList() {
        List<Cronjob> cronjobList = new ArrayList<>();
        List<Map<String, Object>> cronjobConfigList = cronjobManagementRepository.getCronjobConfigList();
        if (Objects.isNull(cronjobConfigList)) return cronjobList;
        for (Map<String, Object> cronjobConfigMap : cronjobConfigList) {
            String cronjobName = (String) cronjobConfigMap.get("cronjobName");
            if (!StringUtils.hasText(cronjobName)) continue;
            String expression = (String) cronjobConfigMap.get("cronjob_expression");
            if (!CronExpression.isValidExpression(expression)) continue;
            Integer poolSize = (Integer) cronjobConfigMap.get("pool_size");
            if (Objects.isNull(poolSize) || poolSize < 1 || poolSize > 128) continue;
            String taskName = (String) cronjobConfigMap.get("task_name");
            if (!StringUtils.hasText(taskName)) continue;
            Runnable task = context.getBean(taskName, Runnable.class);
            Cronjob cronjob = new Cronjob();
            cronjob.setCronjobName(cronjobName);
            cronjob.setExpression(expression);
            cronjob.setPoolSize(poolSize);
            cronjob.setTask(task);
            cronjob.initializeCronjobExecutor();
            cronjobList.add(cronjob);
        }
        return cronjobList;
    }

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        return taskScheduler;
    }

}
