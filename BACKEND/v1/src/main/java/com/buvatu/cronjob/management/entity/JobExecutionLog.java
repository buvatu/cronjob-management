package com.buvatu.cronjob.management.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data @NoArgsConstructor
@Table(name = "job_execution_log")
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private UUID sessionId;
    private String activityName;
    private Integer progressValue;
    private String description;
    private Instant createdAt;

    public JobExecutionLog(UUID sessionId, String activityName, Integer progressValue, String description, Instant createdAt) {
        this.sessionId = sessionId;
        this.activityName = activityName;
        this.progressValue = progressValue;
        this.description = description;
        this.createdAt = createdAt;
    }
}
