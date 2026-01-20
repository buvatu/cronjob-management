package com.buvatu.cronjob.management.repository;

import com.buvatu.cronjob.management.entity.BaseEntity;
import com.buvatu.cronjob.management.entity.JobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {
    Optional<JobExecution> findByJobNameAndStatus(String jobName, BaseEntity.Status status);

    List<JobExecution> findByJobName(String jobName);

    Page<JobExecution> findByJobName(String jobName, Pageable pageable);

    Optional<JobExecution> findFirstByJobNameOrderByCreatedAtDesc(String jobName);

    @Transactional
    @Modifying
    @Query("UPDATE JobExecution je SET je.status = ?2 WHERE je.jobName = ?1 AND je.status = 'RUNNING'")
    void updateStatusOfRunningJob(String jobName, BaseEntity.Status status);
}
