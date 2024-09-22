package com.buvatu.cronjob.management.cronjob;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.stereotype.Component;

import com.buvatu.cronjob.management.model.Cronjob;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class DemoJob extends Cronjob {

    public DemoJob() {
        setTask(new Runnable() {

            @Override
            public void run() {
                Map<String, Object> cronjobHistoryLog = new HashMap<String, Object>();
                String executeResult = "SUCCESS";
                try {
                    cronjobHistoryLog.put("cronjobName", getCronjobName());
                    cronjobHistoryLog.put("startAt", LocalDateTime.now());
                    insertStartLog();
                    runActivity1();
                    runActivity2();
                } catch (Exception e) {
                    
                }
                cronjobHistoryLog.put("endAt", LocalDateTime.now());
                cronjobHistoryLog.put("executeResult", executeResult);
            }

        });

    }

    private void insertStartLog() {
        getCronjobManagementRepository().insertTracingLog(this.getCronjobName(), "Start", 0);
    }

    private void insertEndLog() {
        getCronjobManagementRepository().insertTracingLog(this.getCronjobName(), "End", 100);
    }

    private void runActivity1() throws InterruptedException {
        getCronjobManagementRepository().insertTracingLog(this.getCronjobName(), "Activity1", 0);
        Thread.sleep(10000);
        getCronjobManagementRepository().insertTracingLog(this.getCronjobName(), "Activity1", 50);
    }

    private void runActivity2() {
        getCronjobManagementRepository().insertTracingLog(this.getCronjobName(), "Activity2", 50);
        getCronjobManagementRepository().insertTracingLog(this.getCronjobName(), "Activity2", 100);
    }
}
