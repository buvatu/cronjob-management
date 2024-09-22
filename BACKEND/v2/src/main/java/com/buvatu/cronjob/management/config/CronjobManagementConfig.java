package com.buvatu.cronjob.management.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buvatu.cronjob.management.model.Activity;
import com.buvatu.cronjob.management.model.Cronjob;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;

@Configuration
public class CronjobManagementConfig {

    @Autowired
    CronjobManagementRepository cronJobManagementRepository;

    @Bean
    List<Cronjob> jobList() {
        return cronJobManagementRepository.getAllCronjob();
    }

    @Bean
    List<Activity> activityList() {
        return cronJobManagementRepository.getAllSep();
    }

    @Bean
    List<Activity> runningActivityList() {
        return cronJobManagementRepository.getAllSep();
    }

}
