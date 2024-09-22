package com.buvatu.cronjob.management.task;

import org.springframework.stereotype.Component;

import com.buvatu.cronjob.management.model.Task;

@Component
public class FirstTask implements Task {

    @Override
    public String getTaskExecutionResult() {
        return null;
    }

}
