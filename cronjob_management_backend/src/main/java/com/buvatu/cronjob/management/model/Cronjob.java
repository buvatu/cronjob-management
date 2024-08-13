package com.buvatu.cronjob.management.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.StringUtils;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Cronjob {

    private String jobName;
    private String cronExpression;
    private ScheduledFuture<?> scheduledFuture;
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private CronjobManagementRepository cronJobManagementRepository;

    public Cronjob(String jobName, String cronExpression) {
        this.jobName = jobName;
        if (CronExpression.isValidExpression(cronExpression)) {
            this.cronExpression = cronExpression;
        }
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadGroupName(jobName);
        taskScheduler.initialize();
        schedule();
    }

    public void schedule() {
        scheduledFuture = taskScheduler.schedule(new Runnable() {

            @Override
            public void run() {
                start();
            }

        }, new CronTrigger(cronExpression));
    }

    @Autowired
    private List<Step> stepList;

    public void start() {
        String taskExecutionResult = "";
        AtomicReference<String> activityName = new AtomicReference<String>("Start Activity");
        do {
            Step nextStep = stepList.stream().filter(e -> e.getJobName().equals(jobName) && e.getActivityName().equals(activityName.get())).findFirst().orElse(null);
            if (Objects.isNull(nextStep)) {
                break;
            }
            Activity nextActivity = nextStep.getNextActivityMap().get(taskExecutionResult);
            if (Objects.isNull(nextActivity)) {
                break;
            }
            taskExecutionResult = nextActivity.getTaskExecutionResult();
            activityName.set(nextActivity.getActivityName());
        } while (StringUtils.hasText(activityName.get()) && StringUtils.hasText(taskExecutionResult) && !"Exception".equals(taskExecutionResult));
    }

    @Autowired
    private List<Activity> runningActivityList;

    public void cancel() {
        if (Objects.isNull(scheduledFuture)) {
            return;
        }
        scheduledFuture.cancel(true);
        Activity runningActivity = runningActivityList.stream().filter(e -> e.getJobName().equals(jobName)).findFirst().orElseGet(null);
        if (Objects.nonNull(runningActivity)) {
            runningActivity.stop();
        }
    }

    public void disable() {
        cancel();
        taskScheduler.destroy();
        scheduledFuture = null;
        taskScheduler = null;
    }

}
