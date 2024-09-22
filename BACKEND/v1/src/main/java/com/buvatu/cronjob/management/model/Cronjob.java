package com.buvatu.cronjob.management.model;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

import com.buvatu.cronjob.management.repository.CronjobManagementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.context.request.RequestContextHolder;

@Getter @Setter
public class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();

    private String expression;
    private Integer poolSize = 5; // Default poolSize = 5
    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledFuture;
    private Runnable task;

    @Autowired
    private CronjobManagementRepository cronjobManagementRepository; // helper

    public void initializeTaskScheduler() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(poolSize);
        taskScheduler.setThreadNamePrefix(cronjobName);
        taskScheduler.initialize();
    }

    public void schedule(String updatedExpression) {
        if (!StringUtils.hasText(updatedExpression) || !CronExpression.isValidExpression(updatedExpression) || updatedExpression.equalsIgnoreCase(expression)) {
            return;
        }
        if (Objects.isNull(taskScheduler)) initializeTaskScheduler();
        if (Objects.nonNull(scheduledFuture)) scheduledFuture.cancel(true);
        expression = updatedExpression;
        scheduledFuture = taskScheduler.schedule(task, new CronTrigger(expression));
        cronjobManagementRepository.updateCronjobStatus("SCHEDULED", cronjobName);
    }

    public void cancel() {
        if (Objects.nonNull(scheduledFuture)) scheduledFuture.cancel(true);
        taskScheduler.destroy();
        scheduledFuture = null;
        taskScheduler = null;
        cronjobManagementRepository.updateCronjobStatus("CANCELLED", cronjobName);
    }

    public void start() {
        if (Objects.isNull(taskScheduler)) initializeTaskScheduler();
        taskScheduler.execute(task);
    }

    public void stop() {
        destroyCronjob();
        cronjobManagementRepository.updateCronjobStatus("STOPPED", cronjobName);
    }

    public void loadConfig() {
        Map<String, Object> cronjobConfigMap = cronjobManagementRepository.getCronjobConfig(cronjobName);
        setExpression((String) cronjobConfigMap.get("cronjob_expression"));
        setPoolSize((Integer) cronjobConfigMap.get("pool_size"));
    }

    private String getCurrentLoggedInUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Objects.nonNull(auth) ? auth.getName() : "SYSTEM";
    }

    private void destroyCronjob() {
        if (Objects.nonNull(scheduledFuture)) scheduledFuture.cancel(true);
        taskScheduler.destroy();
        scheduledFuture = null;
        taskScheduler = null;
    }
}
