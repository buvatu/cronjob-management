package com.buvatu.cronjob.management.repository;

import com.buvatu.cronjob.management.entity.JobConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobConfigRepository extends JpaRepository<JobConfig, UUID> {
    Optional<JobConfig> findByName(String cronjobName);
    Optional<JobConfig> findByNameAndUpdatedAtAfter(String cronjobName, Instant updatedAt);
}
