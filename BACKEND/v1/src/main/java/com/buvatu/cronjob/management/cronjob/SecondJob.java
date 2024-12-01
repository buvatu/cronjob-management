package com.buvatu.cronjob.management.cronjob;

import com.buvatu.cronjob.management.model.Cronjob;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class SecondJob extends Cronjob {

    public SecondJob(CronjobManagementRepository cronjobManagementRepository, ThreadPoolTaskScheduler taskScheduler) {
        super(cronjobManagementRepository, taskScheduler);
    }

    @Override
    public void execute() {
        runActivity();
    }

    private void runActivity() {
        insertTracingLog("Activity2", 0);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            if (isInterrupted()) {
                return;
            }
            System.out.println(i);
        }
        insertTracingLog("Activity2", 100);
    }
}
