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

        if (BaseEntity.Status.SCHEDULED.equals(config.getStatus()) && CronExpression.isValidExpression(config.getExpression())) {
            future = taskScheduler.schedule(this::executeTask, new CronTrigger(config.getExpression())); // schedule in case the instance is restarted
        }
    }

    public void schedule(String executor, String description) {
        if (isRunning()) throw new BusinessException(409, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        reloadConfig();
        if (config.getStatus().equals(BaseEntity.Status.SCHEDULED)) throw new BusinessException(409, CronjobConstant.CRONJOB_MUST_BE_UNSCHEDULED);
        if (!CronExpression.isValidExpression(config.getExpression())) throw new BusinessException(400, CronjobConstant.EXPRESSION_IS_NOT_VALID);
        if (Objects.isNull(config.getPoolSize()) || config.getPoolSize() < 1) throw new BusinessException(400, CronjobConstant.POOL_SIZE_IS_NOT_VALID);
        future = taskScheduler.schedule(this::executeTask, new CronTrigger(config.getExpression()));
        config.setStatus(BaseEntity.Status.SCHEDULED);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.SCHEDULE_JOB, description);
    }

    public void updateExpression(String executor, String description,@NonNull String expression) {
        if (isRunning()) throw new BusinessException(409, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        reloadConfig();
        if (Objects.isNull(config)) throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName));
        if (!CronExpression.isValidExpression(expression)) throw new BusinessException(400, CronjobConstant.EXPRESSION_IS_NOT_VALID);
        if (Objects.equals(config.getExpression(), expression)) throw new BusinessException(409, CronjobConstant.EXPRESSION_IS_NOT_CHANGED);
        String expressionDescription = "UPDATED EXPRESSION: " + config.getExpression() + " --> " + expression + ". REASON: " + description;
        config.setExpression(expression);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.UPDATE_CRON_EXPRESSION, expressionDescription);
        unschedule();
        if (!config.getStatus().equals(BaseEntity.Status.SCHEDULED)) return;
        future = taskScheduler.schedule(this::executeTask, new CronTrigger(expression));
    }

    public void updatePoolSize(String executor, String description, Integer poolSize) {
        if (isRunning()) throw new BusinessException(409, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        reloadConfig();
        if (Objects.isNull(config)) throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName));
        if (Objects.isNull(poolSize) || poolSize < 1) throw new BusinessException(400, CronjobConstant.POOL_SIZE_IS_NOT_VALID);
        if (Objects.equals(config.getPoolSize(), poolSize)) throw new BusinessException(409, CronjobConstant.POOL_SIZE_IS_NOT_CHANGED);
        String poolSizeDescription = "UPDATED POOL SIZE: " + config.getPoolSize() + "--> " + poolSize + ". REASON: " + description;
        config.setPoolSize(poolSize);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.UPDATE_POOL_SIZE, poolSizeDescription);
    }
    public void cancel(String executor, String description) {
        if (isRunning()) throw new BusinessException(409, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        reloadConfig();
        if (Objects.isNull(config)) throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_NOT_FOUND, cronjobName));
        if (config.getStatus().equals(BaseEntity.Status.UNSCHEDULED)) throw new BusinessException(409, String.format(CronjobConstant.CRONJOB_IS_ALREADY_UNSCHEDULED, cronjobName));
        unschedule();
        shutdownExecutorService();
        config.setStatus(BaseEntity.Status.UNSCHEDULED);
        jobConfigRepository.save(config);
        jobConfigRepository.flush();
        insertJobOperationHistoryLog(executor, JobOperation.Operation.CANCEL_JOB, "CANCEL JOB. REASON: " + description);
    }

    public void forceStart(String executor, String description) {
        if (isRunning()) throw new BusinessException(409, String.format(CronjobConstant.CRONJOB_IS_RUNNING, cronjobName));
        insertJobOperationHistoryLog(executor, JobOperation.Operation.START_JOB_MANUALLY, description);
        this.executor = executor;
        executeTask();
        this.executor = null;
    }

    public void forceStop(String executor, String description) {
        if (!isRunning()) throw new BusinessException(404, String.format(CronjobConstant.CRONJOB_IS_NOT_RUNNING, cronjobName));
        insertJobOperationHistoryLog(executor, JobOperation.Operation.STOP_JOB_MANUALLY, description);
        jobExecutionRepository.updateStatusOfRunningJob(cronjobName, BaseEntity.Status.ABORTED);
        jobExecutionRepository.flush();
        shutdownExecutorService();
    }

    private void executeTask() {
        // ScheduleLock
        JobExecution jobExecution;
        try {
            jobExecution = getJobExecution();
            if (Objects.isNull(jobExecution)) return;
            jobExecutionRepository.save(jobExecution);
            jobExecutionRepository.flush();
        } catch (Exception e) {
            log.error("Another instance already executed job: {}", e.getMessage());
            return;
        }
        if (Objects.isNull(executor)) insertJobOperationHistoryLog("SYSTEM", JobOperation.Operation.EXECUTE_JOB_ON_A_SCHEDULE, "Executed on a schedule");
        sessionId = jobExecution.getId();
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
        }
        sessionId = null;
    }

    private void finalizeJobExecution(JobExecution jobExecution, JobExecution.ExitCode exitCode, String output, Instant startTime) {
        jobExecution.setExitCode(exitCode);
        jobExecution.setOutput(output);
        jobExecution.setDuration(Duration.between(startTime, Instant.now()));
        jobExecution.setStatus(BaseEntity.Status.COMPLETED); // Assuming status should change from RUNNING
        jobExecutionRepository.save(jobExecution);
        jobExecutionRepository.flush();
    }
    private JobExecution getJobExecution() {
        try {
            JobExecution jobExecution = new JobExecution();
            jobExecution.setJobName(cronjobName);
            jobExecution.setInstanceId(InetAddress.getLocalHost().getHostName());
            if (Objects.isNull(executor)) {
                jobExecution.setTriggerType(JobExecution.TriggerType.AUTO);
                jobExecution.setExecutor("SYSTEM");
            } else {
                jobExecution.setExecutor(executor);
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
        jobExecutionLogRepository.save(new JobExecutionLog(sessionId, activityName, progressValue, description, Instant.now()));
        jobExecutionLogRepository.flush();
    }

    private void insertJobOperationHistoryLog(String executor, JobOperation.Operation operation, String description) {
        jobOperationRepository.save(new JobOperation(cronjobName, operation, executor, description));
    }

    public List<JobOperation> getChangeHistoryList() {
        return jobOperationRepository.findByJobName(cronjobName);
    }

    public List<JobExecution> getAllRunningHistory() {
        return jobExecutionRepository.findByJobName(cronjobName);
    }

    public List<JobExecutionLog> getTracingLogList(UUID sessionId) {
        return jobExecutionLogRepository.findBySessionIdOrderById(sessionId);
    }

    public Instant getLastExecutionTime() {
        return Objects.requireNonNull(jobExecutionRepository.findFirstByJobNameOrderByCreatedAtDesc(cronjobName).orElse(null)).getCreatedAt();
    }

    protected abstract void execute();

    protected boolean isRunning() {
        return jobExecutionRepository.findByJobNameAndStatus(cronjobName, BaseEntity.Status.RUNNING).isPresent();
    }

    @Scheduled(fixedRateString = "PT30S", initialDelayString = "#{new java.util.Random().nextInt(30000)}") // Reload every 30 seconds - random from 0-30s initial delay
    private void reload() {
        if (!isRunning()) shutdownExecutorService();
        reloadConfig();
        unschedule();
        if (BaseEntity.Status.UNSCHEDULED.equals(config.getStatus()) || !CronExpression.isValidExpression(config.getExpression())) return;
        future = taskScheduler.schedule(this::executeTask, new CronTrigger(config.getExpression()));
    }

    private void reloadConfig() {
        JobConfig lastestConfig = jobConfigRepository.findByNameAndUpdatedAtAfter(cronjobName, config.getUpdatedAt()).orElse(null);
        if (Objects.isNull(lastestConfig)) return;
        config = lastestConfig;
    }

    private void unschedule() {
        if (Objects.isNull(future)) return;
        future.cancel(true);
        future = null;
    }

    private void shutdownExecutorService() {
        if (Objects.isNull(executorService)) return;
        executorService.shutdownNow();
        executorService = null;
    }
}
