package com.buvatu.cronjob.management.cronjob;

import com.buvatu.cronjob.management.model.BusinessException;
import com.buvatu.cronjob.management.model.CronjobStatus;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.buvatu.cronjob.management.model.Cronjob;

import java.util.stream.IntStream;

@Component
@Slf4j
public class DemoJob extends Cronjob {

    @Override
    public String getExecutionResult() {
        insertTracingLog("Activity1", 0);
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            System.out.println(i);
        }
        insertTracingLog("Activity1", 100);
        return "SUCCESS";
    }

}
