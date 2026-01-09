package com.buvatu.cronjob.management.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.Duration;

@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "job_execution", indexes = @Index(name = "idx_unique_running_job", columnList = "job_name", unique = true))
@Data
public class JobExecution extends BaseEntity {
    @Column(name = "job_name")
    private String jobName;
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;
    private String executor;
    private String instanceId;
    @Enumerated(EnumType.ORDINAL)
    private ExitCode exitCode;
    private String output;
    private Duration duration;

    public enum TriggerType {
        MANUAL, AUTO;
    }

    public enum ExitCode {
        SUCCESS, FAILURE;
    }
}
