package com.buvatu.cronjob.management.cronjob;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.buvatu.cronjob.management.model.Cronjob;

import java.util.stream.IntStream;

@Component @Slf4j
public class DemoJob extends Cronjob {

    public DemoJob(CronjobManagementRepository cronjobManagementRepository) {
        super(cronjobManagementRepository);
        setTask(this::run);
    }

    private void runActivity() {
        getCronjobManagementRepository().insertTracingLog(getCronjobName(), getSessionId(), "Activity1", 0);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            if (isInterrupted()) {
                return;
            }
            System.out.println(i);
        }
        getCronjobManagementRepository().insertTracingLog(getCronjobName(), getSessionId(), "Activity1", 100);
    }

    private void run() {
        runActivity();
    }
}