package com.buvatu.cronjob.management.repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.buvatu.cronjob.management.model.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository @Slf4j
public class CronjobManagementRepository {

    @PersistenceContext
    private EntityManager em;

    public Map<String, Object> getCronjobConfig(String cronjobName) {
        try {
            return getCronjobConfigMap(em.createNativeQuery("select id, cronjob_name, pool_size, cronjob_expression, cronjob_status from cronjob where cronjob_name = :cronjobName").setParameter("cronjobName", cronjobName).getSingleResult());
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> getCronjobConfigList() {
        try {
            Stream<?> queryStream = em.createNativeQuery("select id, cronjob_name, pool_size, cronjob_expression, cronjob_status from cronjob").getResultStream();
            return queryStream.map(record -> getCronjobConfigMap(record)).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> getCronjobConfigMap(Object record) {
        Object[] cronjobConfig = (Object[]) record;
        Map<String, Object> cronjobConfigMap = new HashMap<>();
        cronjobConfigMap.put("id", cronjobConfig[0]);
        cronjobConfigMap.put("cronjob_name", cronjobConfig[1]);
        cronjobConfigMap.put("pool_size", cronjobConfig[2]);
        cronjobConfigMap.put("cronjob_expression", cronjobConfig[3]);
        cronjobConfigMap.put("cronjob_status", cronjobConfig[4]);
        return cronjobConfigMap;
    }

    public void updateCronjobStatus(String status, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set cronjob_status = :status where cronjob_name = :cronjobName").setParameter("status", status).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to execute update table workflow_config");
        }
    }

    public void updateCronjobExpression(String expression, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set cronjob_expression = :expression where cronjob_name = :cronjobName").setParameter("expression", expression).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to execute update table workflow_config");
        }
    }

    public void updateCronjobSessionId(String sessionId, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set current_session_id = :sessionId where cronjob_name = :cronjobName").setParameter("sessionId", sessionId).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to execute update table workflow_config");
        }
    }

    public void updateCronjobPoolSize(Integer poolSize, String cronjobName) {
        try {
            em.createNativeQuery("update workflow_config set pool_size = :poolSize where cronjob_name = :cronjobName").setParameter("poolSize", poolSize).setParameter("cronjobName", cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to execute update table workflow_config");
        }
    }

    public String getCronjobStatus(String cronjobName) {
        try {
            return (String) em.createNativeQuery("select cronjob_status from workflow_config where cronjob_name = :cronjobName").getSingleResult();
        } catch (Exception e) {
            throw new BusinessException(500, "Failed to execute query");
        }
    }

    public void insertTracingLog(String cronjobName, String sessionId, String activityName, Integer progressValue) {
        try {
            em.createNativeQuery("insert into workflow_log(cronjob_name, session_id, activity_name, progress_value) values (:cronjobName, :sessionId,:activityName, : progressValue)")
                    .setParameter("cronjobName", cronjobName)
                    .setParameter("sessionId", sessionId)
                    .setParameter("activityName", activityName)
                    .setParameter("progressValue", progressValue)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new BusinessException(500, "Failed to insert tracing log to workflow_log table");
        }
    }

    public Map<String, Object> getLatestCronjobHistoryLog(String cronjobName) {
        try {
            Object queryResult = em.createNativeQuery("select id, operation, execute_result from workflow_change_history where cronjob_name = :cronjobName order by id desc limit 1").getSingleResult();
            Object[] latestCronjobHistoryLog = (Object[]) queryResult;
            Map<String, Object> cronjobHistoryLogMap = new HashMap<>();
            cronjobHistoryLogMap.put("id", latestCronjobHistoryLog[0]);
            cronjobHistoryLogMap.put("operation", latestCronjobHistoryLog[1]);
            cronjobHistoryLogMap.put("executeResult", latestCronjobHistoryLog[2]);
            return cronjobHistoryLogMap;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new BusinessException(500, "Failed to get log from workflow_change_history");
        }
    }

    public void insertCronjobHistoryLog(Map<String, Object> paramMap) {
        try {
            Query query = em.createNativeQuery("insert into workflow_change_history(cronjob_name, start_at, stop_at, operation, executed_by, execute_result) values (:cronjobName, :beginTime, :endTime, :operation, :executedBy, :executedResult)");
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
            query.executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void updateCronjobHistoryLog(Map<String, Object> paramMap) {
        try {
            Query query = em.createNativeQuery("update workflow_change_history set sessionId = :sessionId, stop_at = :endTime, execute_result = :executedResult where id = :id");
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
            query.executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public List<Map<String, Object>> getLatestLogList(String cronjobName) {
        try {
            Stream<?> queryStream = em.createNativeQuery("select j.cronjob_name, l.activity_name, l.progress_value, j.cronjob_status from cronjob j join workflow_log l on l.cronjob_name = j.cronjob_name order by l.id desc limit 1").getResultStream();
            return queryStream.map(record -> {
                Object[] workflowValues = (Object[]) record;
                Map<String, Object> workflowLogMap = new HashMap<>();
                workflowLogMap.put("cronjob_name", workflowValues[1]);
                workflowLogMap.put("activity_name", workflowValues[2]);
                workflowLogMap.put("progress_value", workflowValues[3]);
                workflowLogMap.put("cronjob_status", workflowValues[4]);
                return workflowLogMap;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    public List<Map<String, Object>> getActiveLogs() {
        try {
            Stream<?> queryStream = em.createNativeQuery("select j.cronjob_name, l.activity_name, l.progress_value, j.cronjob_status from cronjob j join workflow_log l on l.cronjob_name = j.cronjob_name order by l.id desc limit 1").getResultStream();
            return queryStream.map(record -> {
                Object[] workflowValues = (Object[]) record;
                Map<String, Object> workflowLogMap = new HashMap<>();
                workflowLogMap.put("cronjob_name", workflowValues[1]);
                workflowLogMap.put("activity_name", workflowValues[2]);
                workflowLogMap.put("progress_value", workflowValues[3]);
                workflowLogMap.put("cronjob_status", workflowValues[4]);
                return workflowLogMap;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

}
