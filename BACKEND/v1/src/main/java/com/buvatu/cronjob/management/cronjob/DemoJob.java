package com.buvatu.cronjob.management.cronjob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.buvatu.cronjob.management.model.Cronjob;

@Component
@Slf4j
public class DemoJob extends Cronjob {

    @Override
    public void execute() {
        insertTracingLog("Activity1", 0, "test");
        for (int i = 0; i < Integer.MAX_VALUE/100; i++) {
            System.out.println(i);
        }
        insertTracingLog("Activity1", 100, "test");
        if (!isRunning()) {
            throw new RuntimeException("test");
        }
        insertTracingLog("Activity2", 0, "test 2");
        for (int i = 0; i < Integer.MAX_VALUE/100; i++) {
            System.out.println(i);
        }
        insertTracingLog("Activity2", 100, "test 2");
    }

}
