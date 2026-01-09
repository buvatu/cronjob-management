package com.buvatu.cronjob.management.repository;

import com.buvatu.cronjob.management.entity.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, UUID> {
    List<JobExecutionLog> findBySessionIdOrderById(UUID sessionId);
}
