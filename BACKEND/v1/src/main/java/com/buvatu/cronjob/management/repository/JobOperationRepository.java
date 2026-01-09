package com.buvatu.cronjob.management.repository;

import com.buvatu.cronjob.management.entity.JobOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobOperationRepository extends JpaRepository<JobOperation, UUID> {
    List<JobOperation> findByJobName(String jobName);
}
