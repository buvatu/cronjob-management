package com.buvatu.cronjob.management.repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import com.buvatu.cronjob.management.model.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository @Slf4j
public class CronjobManagementRepository {

    @PersistenceContext
    private EntityManager em;

    public Map<String, Object> getCronjobConfig(String cronjobName) {
        try {
            Object result = em.createNativeQuery("select cronjob_name, pool_size, cronjob_expression from workflow_config where cronjob_name = :cronjobName").setParameter("cronjobName", cronjobName).getSingleResult();
            Object[] cronjobConfig = (Object[]) result;
            Map<String, Object> cronjobConfigMap = new HashMap<>();
            cronjobConfigMap.put("cronjobName", cronjobConfig[0]);
            cronjobConfigMap.put("poolSize", cronjobConfig[1]);
            cronjobConfigMap.put("expression", cronjobConfig[2]);
            return cronjobConfigMap;
        } catch (Exception e) {
            log.error("getCronjobConfig: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void updateCronjobStatus(String status, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set cronjob_status = :status where cronjob_name = :cronjobName").setParameter("status", status).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobStatus: {}", e.getMessage());
        }
    }

    @Transactional
    public void updateCronjobExpression(String expression, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set cronjob_expression = :expression where cronjob_name = :cronjobName").setParameter("expression", expression).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobExpression: {}", e.getMessage());
        }
    }

    @Transactional
    public void updateCronjobSessionId(String sessionId, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set current_session_id = :sessionId where cronjob_name = :cronjobName").setParameter("sessionId", sessionId).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobSessionId: {}", e.getMessage());
        }
    }

    @Transactional
    public void updateCronjobPoolSize(Integer poolSize, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set pool_size = :poolSize where cronjob_name = :cronjobName").setParameter("poolSize", poolSize).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobPoolSize: {}", e.getMessage());
        }
    }

    @Transactional
    public void updateCronjobInterupptedStatus(boolean interupptedStatus, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set is_interuppted = :interupptedStatus where cronjob_name = :cronjobName").setParameter("interupptedStatus", interupptedStatus).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobInterupptedStatus: {}", e.getMessage());
        }
    }

    public String getCronjobStatus(String cronjobName) {
        try {
            return (String) em.createNativeQuery("select cronjob_status from workflow_config where cronjob_name = :cronjobName").setParameter("cronjobName", cronjobName).getSingleResult();
        } catch (Exception e) {
            log.error("getCronjobStatus: {}", e.getMessage());
            throw new BusinessException(500, "Failed to execute query");
        }
    }

    public boolean isCronjobInteruppted(String cronjobName) {
        try {
            return (boolean) em.createNativeQuery("select is_interuppted from workflow_config where cronjob_name = :cronjobName").setParameter("cronjobName", cronjobName).getSingleResult();
        } catch (Exception e) {
            log.error("isCronjobInteruppted: {}", e.getMessage());
            throw new BusinessException(500, "Failed to execute query");
        }
    }

    @Transactional
    public void insertTracingLog(String cronjobName, String sessionId, String activityName, Integer progressValue) {
        try {
            em.createNativeQuery("insert into workflow_log(cronjob_name, session_id, activity_name, progress_value) values (:cronjobName, :sessionId,:activityName, :progressValue)")
                    .setParameter("cronjobName", cronjobName)
                    .setParameter("sessionId", sessionId)
                    .setParameter("activityName", activityName)
                    .setParameter("progressValue", progressValue)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertTracingLog: {}", e.getMessage());
            throw new BusinessException(500, "Failed to insert tracing log to workflow_log table");
        }
    }

    @Transactional
    public void insertCronjobHistoryLog(String cronjobName, String sessionId, LocalDateTime startTime, String operation, String executor, String executeResult) {
        try {
            em.createNativeQuery("insert into workflow_change_history(cronjob_name, session_id, start_at, operation, executed_by, execute_result) values (:cronjobName, :sessionId, :startTime, :operation, :executor, :executeResult)")
                    .setParameter("cronjobName", cronjobName)
                    .setParameter("sessionId", sessionId)
                    .setParameter("startTime", startTime)
                    .setParameter("operation", operation)
                    .setParameter("executor", executor)
                    .setParameter("executeResult", executeResult)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertCronjobHistoryLog: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> getChangeHistoryLogList(String cronjobName) {
        try {
            Stream<?> queryStream = em.createNativeQuery("select session_id, start_at, stop_at, executed_by, operation, execute_result from workflow_change_history where cronjob_name = :cronjobName").setParameter("cronjobName", cronjobName).getResultStream();
            return queryStream.map(record -> {
                Object[] workflowChangeValues = (Object[]) record;
                Map<String, Object> workflowChangeHistoryMap = new HashMap<>();
                workflowChangeHistoryMap.put("session_id", workflowChangeValues[0]);
                workflowChangeHistoryMap.put("start_at", workflowChangeValues[1]);
                workflowChangeHistoryMap.put("stop_at", workflowChangeValues[2]);
                workflowChangeHistoryMap.put("executed_by", workflowChangeValues[3]);
                workflowChangeHistoryMap.put("operation", workflowChangeValues[4]);
                workflowChangeHistoryMap.put("execute_result", workflowChangeValues[5]);
                return workflowChangeHistoryMap;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getChangeHistoryLogList: {}", e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getWorkflowLogList(String sessionId) {
        try {
            Stream<?> queryStream = em.createNativeQuery("select cronjob_name, activity_name, progress_value from workflow_log where session_id = :sessionId").setParameter("sessionId", sessionId).getResultStream();
            return queryStream.map(record -> {
                Object[] workflowValues = (Object[]) record;
                Map<String, Object> workflowLogMap = new HashMap<>();
                workflowLogMap.put("cronjob_name", workflowValues[1]);
                workflowLogMap.put("activity_name", workflowValues[2]);
                workflowLogMap.put("progress_value", workflowValues[3]);
                return workflowLogMap;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("getLatestLogList: {}", e.getMessage());
            return null;
        }
    }

}
