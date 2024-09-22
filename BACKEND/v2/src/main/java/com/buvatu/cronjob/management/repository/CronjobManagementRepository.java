package com.buvatu.cronjob.management.repository;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.buvatu.cronjob.management.model.Cronjob;
import com.buvatu.cronjob.management.model.Activity;

@Repository
public class CronjobManagementRepository {

    @PersistenceContext
    private EntityManager em;

    public Activity getNextStep(String jobName, String stepName, String stepExecutionResult) {
        try {
            Object[] queryResult = (Object[]) em.createNativeQuery("select s.step_name, s.pool_size, s.task_name from Step s join Workflow w on s.job_name = w.job_name and s.step_name = w.step_name where jc.jobName = :jobName").setParameter("jobName", jobName).setParameter("stepExecutionResult", stepExecutionResult).getSingleResult();
            return new Activity((String) queryResult[0], (String) queryResult[0], (String) queryResult[1], (Integer) queryResult[2]);
        } catch (Exception e) {
            return null;
        }
    }

    public List<Cronjob> getAllCronjob() {
        try {
            List<?> resultList = (List<?>) em.createNativeQuery("select s.step_name, s.pool_size, s.task_name from Step s join Workflow w on s.job_name = w.job_name and s.step_name = w.step_name where jc.jobName = :jobName").getResultList();
            return resultList.stream().map(e -> getCronJob(e)).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    private Cronjob getCronJob(Object e) {
        Object[] record = (Object[]) e;
        
        return null;
    }

    public List<Activity> getAllSep() {
        try {
            List<?> resultList = (List<?>) em.createNativeQuery("select s.step_name, s.pool_size, s.task_name from Step s join Workflow w on s.job_name = w.job_name and s.step_name = w.step_name where jc.jobName = :jobName").getResultList();
            return resultList.stream().map(e -> getStepMapper(e)).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    private Activity getStepMapper(Object e) {
        Object[] record = (Object[]) e;
        return null;
    }
}
