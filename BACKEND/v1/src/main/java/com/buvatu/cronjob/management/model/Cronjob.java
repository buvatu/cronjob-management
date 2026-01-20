package com.buvatu.cronjob.management.model;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import com.buvatu.cronjob.management.constant.CronjobConstant;
import com.buvatu.cronjob.management.entity.BaseEntity;
import com.buvatu.cronjob.management.entity.JobConfig;
import com.buvatu.cronjob.management.entity.JobExecution;
import com.buvatu.cronjob.management.entity.JobExecutionLog;
import com.buvatu.cronjob.management.entity.JobOperation;
import com.buvatu.cronjob.management.exception.BusinessException;
import com.buvatu.cronjob.management.repository.JobConfigRepository;
import com.buvatu.cronjob.management.repository.JobExecutionLogRepository;
import com.buvatu.cronjob.management.repository.JobExecutionRepository;
import com.buvatu.cronjob.management.repository.JobOperationRepository;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;

import javax.annotation.PostConstruct;

@Slf4j
@Getter
@Setter
public abstract class Cronjob {

    private final String cronjobName = this.getClass().getSimpleName();
    private JobConfig config;
    private ScheduledFuture<?> future;
    private String executor;
    private ExecutorService executorService;
    private UUID sessionId;

    @Autowired
    private JobConfigRepository jobConfigRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private JobExecutionLogRepository jobExecutionLogRepository;

    @Autowired
    private JobOperationRepository jobOperationRepository;

    @Autowired
    private TaskScheduler taskScheduler;

    @PostConstruct
    private void initialize() {
        config = jobConfigRepository.findByName(cronjobName).orElse(null);
        if (Objects.isNull(config)) { // the first time to run
            // Try to insert the initial config if not exist
            try {
                config = new JobConfig();
                config.setName(cronjobName);
                config.setStatus(BaseEntity.Status.UNSCHEDULED);
                jobConfigRepository.save(config);
                jobConfigRepository.flush();
                insertJobOperationHistoryLog("SYSTEM", JobOperation.Operation.INITIALIZE, "Inserted initial config");
            } catch (Exception e) {
                log.error("Another instance already inserted: {}", e.getMessage());
            }
            return;
        }

        if (BaseEntity.Status.SCHEDULED.equals(config.getStatus())
                && CronExpression.isValidExpression(config.getExpression())) {
            future = taskScheduler.schedule(() -> executeTask(null), new CronTrigger(config.getExpression()));
        }
    }

    public void schedule(String executor, String description) {
        if (isRunning())
            handleFailOperation(JobOperation.Operation.SCHEDULE_JOB, executor, description,
                    String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName), 409);
        reloadConfig();
        if (config.getStatus().equals(BaseEntity.Status.SCHEDULED))
            handleFailOperation(JobOperation.Operation.SCHEDULE_JOB, executor, description,
                    CronjobConstant.CRONJOB_MUST_BE_UNSCHEDULED, 409);
        if (!CronExpression.isValidExpression(config.getExpression()))
            handleFailOperation(JobOperation.Operation.SCHEDULE_JOB, executor, description,
                    CronjobConstant.EXPRESSION_IS_NOT_VALID, 400);
        if (Objects.isNull(config.getPoolSize()) || config.getPoolSize() < 1)
            handleFailOperation(JobOperation.Operation.SCHEDULE_JOB, executor, description,
                    CronjobConstant.POOL_SIZE_IS_NOT_VALID, 400);
        future = taskScheduler.schedule(() -> executeTask(null), new CronTrigger(config.getExpression()));
        config.setStatus(BaseEntity.Status.SCHEDULED);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.SCHEDULE_JOB, description);
    }

    public void updateExpression(String executor, String description, @NonNull String expression, Long version) {
        if (isRunning())
            handleFailOperation(JobOperation.Operation.UPDATE_CRON_EXPRESSION, executor, description,
                    String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName), 409);
        reloadConfig();
        if (Objects.isNull(config))
            handleFailOperation(JobOperation.Operation.UPDATE_CRON_EXPRESSION, executor, description,
                    String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName), 404);
        if (!Objects.equals(config.getVersion(), version))
            handleFailOperation(JobOperation.Operation.UPDATE_CRON_EXPRESSION, executor, description,
                    CronjobConstant.VERSION_MISMATCH, 409);
        if (!CronExpression.isValidExpression(expression))
            handleFailOperation(JobOperation.Operation.UPDATE_CRON_EXPRESSION, executor, description,
                    CronjobConstant.EXPRESSION_IS_NOT_VALID, 400);
        if (Objects.equals(config.getExpression(), expression))
            handleFailOperation(JobOperation.Operation.UPDATE_CRON_EXPRESSION, executor, description,
                    CronjobConstant.EXPRESSION_IS_NOT_CHANGED, 409);
        String expressionDescription = "UPDATED EXPRESSION: " + config.getExpression() + " --> " + expression
                + ". REASON: " + description;
        config.setExpression(expression);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.UPDATE_CRON_EXPRESSION, expressionDescription);
        unschedule();
        if (!config.getStatus().equals(BaseEntity.Status.SCHEDULED))
            return;
        future = taskScheduler.schedule(() -> executeTask(null), new CronTrigger(expression));
    }

    public void updatePoolSize(String executor, String description, Integer poolSize, Long version) {
        if (isRunning())
            handleFailOperation(JobOperation.Operation.UPDATE_POOL_SIZE, executor, description,
                    String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName), 409);
        reloadConfig();
        if (Objects.isNull(config))
            handleFailOperation(JobOperation.Operation.UPDATE_POOL_SIZE, executor, description,
                    String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName), 404);
        if (!Objects.equals(config.getVersion(), version))
            handleFailOperation(JobOperation.Operation.UPDATE_POOL_SIZE, executor, description,
                    CronjobConstant.VERSION_MISMATCH, 409);
        if (Objects.isNull(poolSize) || poolSize < 1)
            handleFailOperation(JobOperation.Operation.UPDATE_POOL_SIZE, executor, description,
                    CronjobConstant.POOL_SIZE_IS_NOT_VALID, 400);
        if (Objects.equals(config.getPoolSize(), poolSize))
            handleFailOperation(JobOperation.Operation.UPDATE_POOL_SIZE, executor, description,
                    CronjobConstant.POOL_SIZE_IS_NOT_CHANGED, 409);
        String poolSizeDescription = "UPDATED POOL SIZE: " + config.getPoolSize() + "--> " + poolSize + ". REASON: "
                + description;
        config.setPoolSize(poolSize);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.UPDATE_POOL_SIZE, poolSizeDescription);
    }

    public void cancel(String executor, String description) {
        if (isRunning())
            handleFailOperation(JobOperation.Operation.CANCEL_JOB, executor, description,
                    String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName), 409);
        reloadConfig();
        if (Objects.isNull(config))
            handleFailOperation(JobOperation.Operation.CANCEL_JOB, executor, description,
                    String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName), 404);
        if (config.getStatus().equals(BaseEntity.Status.UNSCHEDULED))
            handleFailOperation(JobOperation.Operation.CANCEL_JOB, executor, description,
                    String.format(CronjobConstant.CRONJOB_IS_ALREADY_UNSCHEDULED, cronjobName), 409);
        unschedule();
        shutdownExecutorService();
        config.setStatus(BaseEntity.Status.UNSCHEDULED);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.CANCEL_JOB, "CANCEL JOB. REASON: " + description);
    }

    public void forceStart(String executor, String description) {
        if (isRunning())
            handleFailOperation(JobOperation.Operation.START_JOB_MANUALLY, executor, description,
                    String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName), 409);
        insertJobOperationHistoryLog(executor, JobOperation.Operation.START_JOB_MANUALLY, description);
        CompletableFuture.runAsync(() -> executeTask(executor));
    }

    public void forceStop(String executor, String description) {
        if (!isRunning())
            handleFailOperation(JobOperation.Operation.STOP_JOB_MANUALLY, executor, description,
                    String.format(CronjobConstant.CRONJOB_IS_NOT_RUNNING, cronjobName), 404);
        insertJobOperationHistoryLog(executor, JobOperation.Operation.STOP_JOB_MANUALLY, description);
        jobExecutionRepository.updateStatusOfRunningJob(cronjobName, BaseEntity.Status.ABORTED);
        jobExecutionRepository.flush();
        shutdownExecutorService();
    }

    private void executeTask(String manualExecutor) {
        // ScheduleLock
        JobExecution jobExecution;
        try {
            jobExecution = getJobExecution(manualExecutor);
            if (Objects.isNull(jobExecution))
                return;
            jobExecutionRepository.save(jobExecution);
            jobExecutionRepository.flush();
        } catch (Exception e) {
            log.error("Another instance already executed job: {}", e.getMessage());
            return;
        }
        runTask(jobExecution);
    }

    private void runTask(JobExecution jobExecution) {
        if (Objects.isNull(jobExecution.getTriggerType())
                || JobExecution.TriggerType.AUTO.equals(jobExecution.getTriggerType())) {
            insertJobOperationHistoryLog("SYSTEM", JobOperation.Operation.EXECUTE_JOB_ON_A_SCHEDULE,
                    "Executed on a schedule");
        }
        sessionId = jobExecution.getId();
        this.executor = jobExecution.getExecutor();
        insertTracingLog("START", 0, "Started");
        Instant startTime = Instant.now();
        try {
            executorService = Executors.newFixedThreadPool(config.getPoolSize());
            executorService.submit(this::execute).get();
            insertTracingLog("END", 100, "Ended");
            finalizeJobExecution(jobExecution, JobExecution.ExitCode.SUCCESS, null, startTime);
        } catch (Exception e) {
            log.error("failed to execute task: {}", e.getMessage());
            finalizeJobExecution(jobExecution, JobExecution.ExitCode.FAILURE, e.getMessage(), startTime);
        } finally {
            shutdownExecutorService();
            this.executor = null;
            sessionId = null;
        }
    }

    private void finalizeJobExecution(JobExecution jobExecution, JobExecution.ExitCode exitCode, String output,
            Instant startTime) {
        jobExecution.setExitCode(exitCode);
        jobExecution.setOutput(output);
        jobExecution.setDuration(Duration.between(startTime, Instant.now()));
        jobExecution.setStatus(BaseEntity.Status.COMPLETED); // Assuming status should change from RUNNING
        jobExecutionRepository.save(jobExecution);
        jobExecutionRepository.flush();
    }

    private JobExecution getJobExecution(String manualExecutor) {
        try {
            JobExecution jobExecution = new JobExecution();
            jobExecution.setJobName(cronjobName);
            jobExecution.setInstanceId(InetAddress.getLocalHost().getHostName());
            if (Objects.isNull(manualExecutor)) {
                jobExecution.setTriggerType(JobExecution.TriggerType.AUTO);
                jobExecution.setExecutor("SYSTEM");
            } else {
                jobExecution.setExecutor(manualExecutor);
                jobExecution.setTriggerType(JobExecution.TriggerType.MANUAL);
            }
            jobExecution.setStatus(BaseEntity.Status.RUNNING);
            return jobExecution;
        } catch (Exception e) {
            log.error("Cannot get hostname. Caused by: {}", e.getMessage());
            return null;
        }
    }

    public void insertTracingLog(String activityName, Integer progressValue, String description) {
        jobExecutionLogRepository
                .save(new JobExecutionLog(sessionId, activityName, progressValue, description, Instant.now()));
        jobExecutionLogRepository.flush();
    }

    private void insertJobOperationHistoryLog(String executor, JobOperation.Operation operation, String description) {
        jobOperationRepository.save(new JobOperation(cronjobName, operation, executor, description));
    }

    private void insertFailJobOperationHistoryLog(String executor, JobOperation.Operation operation, String description,
            String errorMessage) {
        jobOperationRepository.save(new JobOperation(cronjobName, operation, executor, description, errorMessage));
    }

    private void handleFailOperation(JobOperation.Operation operation, String executor, String description,
            String errorMessage, int code) {
        insertFailJobOperationHistoryLog(executor, operation, description, errorMessage);
        throw new BusinessException(code, errorMessage);
    }

    public List<JobOperation> getChangeHistoryList() {
        return jobOperationRepository.findByJobName(cronjobName);
    }

    public List<JobExecution> getAllRunningHistory() {
        return jobExecutionRepository.findByJobName(cronjobName);
    }

    public Page<JobExecution> getPagedRunningHistory(Pageable pageable) {
        return jobExecutionRepository.findByJobName(cronjobName, pageable);
    }

    public Page<JobOperation> getPagedChangeHistoryList(Pageable pageable) {
        return jobOperationRepository.findByJobName(cronjobName, pageable);
    }

    public List<JobExecutionLog> getTracingLogList(UUID sessionId) {
        return jobExecutionLogRepository.findBySessionIdOrderByCreatedAt(sessionId);
    }

    public Instant getLastExecutionTime() {
        JobExecution jobExecution = jobExecutionRepository.findFirstByJobNameOrderByCreatedAtDesc(cronjobName)
                .orElse(null);
        return Objects.isNull(jobExecution) ? null : jobExecution.getCreatedAt();
    }

    protected abstract void execute();

    protected boolean isRunning() {
        return jobExecutionRepository.findByJobNameAndStatus(cronjobName, BaseEntity.Status.RUNNING).isPresent();
    }

    // Reload every 30 seconds - random from 0-30s initial delay
    @Scheduled(fixedRateString = "PT30S", initialDelayString = "#{new java.util.Random().nextInt(30000)}")
    private void reload() {
        if (!isRunning())
            shutdownExecutorService();
        reloadConfig();
        unschedule();
        if (BaseEntity.Status.UNSCHEDULED.equals(config.getStatus())
                || !CronExpression.isValidExpression(config.getExpression()))
            return;
        future = taskScheduler.schedule(() -> executeTask(null), new CronTrigger(config.getExpression()));
    }

    private void reloadConfig() {
        JobConfig lastestConfig = jobConfigRepository.findByNameAndUpdatedAtAfter(cronjobName, config.getUpdatedAt())
                .orElse(null);
        if (Objects.isNull(lastestConfig))
            return;
        config = lastestConfig;
    }

    private void unschedule() {
        if (Objects.isNull(future))
            return;
        future.cancel(true);
        future = null;
    }

    private void shutdownExecutorService() {
        if (Objects.isNull(executorService))
            return;
        executorService.shutdownNow();
        executorService = null;
    }
}
