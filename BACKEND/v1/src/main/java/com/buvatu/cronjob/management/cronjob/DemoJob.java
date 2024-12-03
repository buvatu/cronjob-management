package com.buvatu.cronjob.management.cronjob;

import com.buvatu.cronjob.management.model.BusinessException;
import com.buvatu.cronjob.management.model.CronjobStatus;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.buvatu.cronjob.management.model.Cronjob;

import java.util.stream.IntStream;

@Component @Slf4j
public class DemoJob extends Cronjob {

    public DemoJob(CronjobManagementRepository cronjobManagementRepository, ThreadPoolTaskScheduler taskScheduler) {
        super(cronjobManagementRepository, taskScheduler);
    }

    @Override
    public String getExecutionResult() {
        try {
            insertTracingLog("Activity1", 0);
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                if (isInterrupted()) {
                    return "INTERRUPTED";
                }
                if (i == 156956) {
                    throw new RuntimeException("test exception");
                }
                System.out.println(i);
            }
            return "SUCCESS";
        } catch (Exception e) {
            setCurrentStatus(CronjobStatus.FAILED);
            return "EXCEPTION : " + e.getMessage();
        } finally {
            insertTracingLog("Activity1", 100);
        }
    }

}
