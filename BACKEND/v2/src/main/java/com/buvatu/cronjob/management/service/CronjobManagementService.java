package com.buvatu.cronjob.management.service;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.buvatu.cronjob.management.model.Activity;
import com.buvatu.cronjob.management.model.Cronjob;
import com.buvatu.cronjob.management.repository.CronjobManagementRepository;

@Service
public class CronjobManagementService {

    private final CronjobManagementRepository cronJobManagementRepository;

    public CronjobManagementService(CronjobManagementRepository cronJobManagementRepository) {
        this.cronJobManagementRepository = cronJobManagementRepository;
    }

    public void schedule(Cronjob cronjob) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadGroupName(cronjob.getJobName());
        taskScheduler.initialize();
        taskScheduler.schedule(new Runnable() {

            @Override
            public void run() {
                executeJob(cronjob);
            }

        }, new CronTrigger(cronjob.getCronExpression()));
    }

    public void executeJob(Cronjob cronjob) {
        
    }

    public String getActivityExecutionResult(Activity activity) throws InterruptedException, ExecutionException {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(activity.getCorePoolSize());
        taskExecutor.setMaxPoolSize(activity.getMaxPoolSize());
        taskExecutor.setThreadGroupName(activity.getJobName());
        taskExecutor.setThreadNamePrefix(activity.getJobName() + "-" + activity.getActivityName());
        taskExecutor.initialize();
        return taskExecutor.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                return activity.getTask().getTaskExecutionResult();
            }

        }).get();
    }

    public void stopRunningActivity(Activity activity) {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread runningThread : threadSet) {
            if (runningThread.getName().startsWith(activity.getJobName() + "-" + activity.getActivityName())) {
                runningThread.interrupt();
            }
        }
    }

    
}
