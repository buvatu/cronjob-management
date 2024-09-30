package com.buvatu.cronjob.management.config;

import com.buvatu.cronjob.management.model.Cronjob;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CronjobManagementConfig {

//    @Bean
//    List<Cronjob> cronjobList() {
//        List<Cronjob> cronjobList =
//        List<Map<String, Object>> cronjobConfigList = cronjobManagementRepository.getCronjobConfigList();
//        if (Objects.isNull(cronjobConfigList)) return cronjobList;
//        for (Map<String, Object> cronjobConfigMap : cronjobConfigList) {
//            String cronjobName = (String) cronjobConfigMap.get("cronjobName");
//            if (!StringUtils.hasText(cronjobName)) continue;
//            String expression = (String) cronjobConfigMap.get("cronjob_expression");
//            if (!CronExpression.isValidExpression(expression)) continue;
//            Integer poolSize = (Integer) cronjobConfigMap.get("pool_size");
//            if (Objects.isNull(poolSize) || poolSize < 1 || poolSize > 128) continue;
//            String taskName = (String) cronjobConfigMap.get("task_name");
//            if (!StringUtils.hasText(taskName)) continue;
//            Runnable task = context.getBean(taskName, Runnable.class);
//            Cronjob cronjob = new Cronjob(cronjobManagementRepository);
//            cronjob.setCronjobName(cronjobName);
//            cronjob.setExpression(expression);
//            cronjob.setPoolSize(poolSize);
//            cronjob.setTask(task);
//            cronjobList.add(cronjob);
//        }
//        return new ArrayList<>();
//    }

}
