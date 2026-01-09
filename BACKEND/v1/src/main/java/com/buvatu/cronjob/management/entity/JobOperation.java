package com.buvatu.cronjob.management.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_operation")
@Data @NoArgsConstructor
public class JobOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String jobName;
    @Enumerated(EnumType.STRING)
    private Operation operation;
    private String executor;
    private String description;
    private Instant executedAt;

    public JobOperation(String jobName, Operation operation, String executor, String description) {
        this.jobName = jobName;
        this.operation = operation;
        this.executor = executor;
        this.description = description;
        this.executedAt = Instant.now();
    }

    public enum Operation {
        UPDATE_POOL_SIZE, UPDATE_CRON_EXPRESSION, INITIALIZE, SCHEDULE_JOB, CANCEL_JOB, START_JOB_MANUALLY, STOP_JOB_MANUALLY, EXECUTE_JOB_ON_A_SCHEDULE
    }
}
