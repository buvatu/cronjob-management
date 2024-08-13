package com.buvatu.cronjob.management.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.buvatu.cronjob.management.model.Activity;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;

@Service
public class CronjobManagementService {

    private final CronjobManagementRepository cronJobManagementRepository;

    public CronjobManagementService(CronjobManagementRepository cronJobManagementRepository) {
        this.cronJobManagementRepository = cronJobManagementRepository;
    }

    public void executeStepsInChain(String jobName, String stepName) {
        String stepExecutionResult = "";
        do {
            Activity nextStep = cronJobManagementRepository.getNextStep(jobName, stepName, stepExecutionResult);
            if (Objects.isNull(nextStep)) {
                break;
            }
            stepExecutionResult = nextStep.getTaskExecutionResult();
        } while (StringUtils.hasText(stepExecutionResult) && !"Exception".equals(stepExecutionResult));
    }

}
