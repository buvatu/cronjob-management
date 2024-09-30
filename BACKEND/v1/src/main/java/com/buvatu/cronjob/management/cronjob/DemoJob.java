package com.buvatu.cronjob.management.cronjob;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.stereotype.Component;

import com.buvatu.cronjob.management.model.Cronjob;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class DemoJob extends Cronjob {

    public DemoJob(CronjobManagementRepository cronjobManagementRepository) {
        super(cronjobManagementRepository);
        setTask(this::run);
    }

    private void runActivity1() {
        getCronjobManagementRepository().insertTracingLog(getCronjobName(), getSessionId(), "Activity1", 0);
        Arrays.asList("Reflection", "Collection", "Stream").stream().forEach(e -> System.out.println(e));
        getCronjobManagementRepository().insertTracingLog(getCronjobName(), getSessionId(), "Activity1", 50);
    }

    private void runActivity2() {
        getCronjobManagementRepository().insertTracingLog(getCronjobName(), getSessionId(), "Activity2", 50);
        Arrays.asList("Sorting", "Mapping", "Reduction", "Stream").stream().forEach(e -> System.out.println(e));
        getCronjobManagementRepository().insertTracingLog(getCronjobName(), getSessionId(), "Activity2", 100);
    }

    private void run() {
        runActivity1();
        runActivity2();
    }
}
