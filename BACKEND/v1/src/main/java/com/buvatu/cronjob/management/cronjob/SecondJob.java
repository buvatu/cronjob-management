package com.buvatu.cronjob.management.cronjob;

import com.buvatu.cronjob.management.model.Cronjob;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class SecondJob extends Cronjob {

    @Override
    public String getExecutionResult() {
        try {
            insertTracingLog("Activity2", 0);
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                if (i == 156956) {
                    throw new RuntimeException("test exception");
                }
                System.out.println(i);
            }
            return "SUCCESS";
        } catch (Exception e) {
            return "EXCEPTION: " + e.getMessage();
        } finally {
            insertTracingLog("Activity2", 100);
        }
    }
}
