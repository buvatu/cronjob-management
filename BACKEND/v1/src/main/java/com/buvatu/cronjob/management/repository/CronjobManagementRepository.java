package com.buvatu.cronjob.management.repository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import com.buvatu.cronjob.management.model.Cronjob;
import com.buvatu.cronjob.management.model.CronjobConstant;
import com.buvatu.cronjob.management.model.CronjobStatus;
import com.buvatu.cronjob.management.service.CronjobManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository @Slf4j
public class CronjobManagementRepository {

    @PersistenceContext
    private EntityManager em;

    public boolean isCronjobExist(String cronjobName) {
        return ((BigInteger) em.createNativeQuery("select count(cronjob_name) from cronjob_config where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getSingleResult()).intValue() > 0;
    }

    @Transactional
    public void insertCronjobConfig(String cronjobName) {
        try {
            em.createNativeQuery("insert into cronjob_config(cronjob_name) values (:cronjobName)").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertCronjobConfig: {}", e.getMessage());
        }
    }

    public String getCronjobExpression(String cronjobName) {
        try {
            return (String) em.createNativeQuery("select cronjob_expression from cronjob_config where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getSingleResult();
        } catch (Exception e) {
            log.error("getCronjobExpression: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void updateCronjobExpression(String expression, String cronjobName) {
        try {
            em.createNativeQuery("update cronjob_config set cronjob_expression = :expression where cronjob_name = :cronjobName").setParameter("expression", expression).setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobExpression: {}", e.getMessage());
        }
    }

    public Integer getCronjobPoolSize(String cronjobName) {
        try {
            return (Integer) em.createNativeQuery("select cronjob_pool_size from cronjob_config where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getSingleResult();
        } catch (Exception e) {
            log.error("getCronjobPoolSize: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void updateCronjobPoolSize(Integer poolSize, String cronjobName) {
        try {
            em.createNativeQuery("update cronjob_config set cronjob_pool_size = :poolSize where cronjob_name = :cronjobName").setParameter("poolSize", poolSize).setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobPoolSize: {}", e.getMessage());
        }
    }

    public CronjobStatus getCronjobStatus(String cronjobName) {
        try {
            return CronjobStatus.valueOf((String) em.createNativeQuery("select cronjob_status from cronjob_config where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getSingleResult());
        } catch (Exception e) {
            log.error("getCronjobStatus: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void updateCronjobStatus(String status, String cronjobName) {
        try {
            em.createNativeQuery("update cronjob_config set cronjob_status = :status where cronjob_name = :cronjobName").setParameter("status", status).setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobStatus: {}", e.getMessage());
        }
    }

    public String getCronjobCurrentSessionId(String cronjobName) {
        try {
            return (String) em.createNativeQuery("select current_session_id from cronjob_config where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getSingleResult();
        } catch (Exception e) {
            log.error("getCronjobCurrentSessionId: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void updateCronjobSessionId(String sessionId, String cronjobName) {
        try {
            em.createNativeQuery("update cronjob_config set current_session_id = :sessionId where cronjob_name = :cronjobName").setParameter("sessionId", sessionId).setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobSessionId: {}", e.getMessage());
        }
    }

    // --------------------------------------------------------------//
    @Transactional
    public void insertTracingLog(String cronjobName, String sessionId, String activityName, Integer progressValue) {
        try {
            em.createNativeQuery("insert into cronjob_running_log(cronjob_name, session_id, activity_name, progress_value) values (:cronjobName, :sessionId, :activityName, :progressValue)")
                    .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                    .setParameter("sessionId", sessionId)
                    .setParameter("activityName", activityName)
                    .setParameter("progressValue", progressValue)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertTracingLog: {}", e.getMessage());
        }
    }

    @Transactional
    public void insertCronjobChangeHistoryLog(String cronjobName, String sessionId, LocalDateTime startTime, String operation, String executor, String executeResult) {
        try {
            em.createNativeQuery("insert into cronjob_change_history(cronjob_name, session_id, start_at, operation, executed_by, execute_result) values (:cronjobName, :sessionId, :startTime, :operation, :executor, :executeResult)")
                    .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                    .setParameter("sessionId", sessionId)
                    .setParameter("startTime", startTime)
                    .setParameter("operation", operation)
                    .setParameter("executor", executor)
                    .setParameter("executeResult", executeResult)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertCronjobChangeHistoryLog: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> getAllCronjob() {
        try {
            Stream<?> queryStream = em.createNativeQuery("select cronjob_name, cronjob_expression, cronjob_pool_size, cronjob_status, current_session_id, (select start_at from cronjob_change_history where operation = 'RUN JOB ON A SCHEDULE' or operation = 'START JOB MANUALLY' order by id desc limit 1) as last_execution_time from cronjob_config").getResultStream();
            return queryStream.map(record -> {
                Object[] cronjobData = (Object[]) record;
                Map<String, Object> cronjobDataMap = new HashMap<>();
                cronjobDataMap.put("cronjob_name", cronjobData[0]);
                cronjobDataMap.put("cronjob_expression", cronjobData[1]);
                cronjobDataMap.put("cronjob_pool_size", cronjobData[2]);
                cronjobDataMap.put("cronjob_status", cronjobData[3]);
                cronjobDataMap.put("current_session_id", cronjobData[4]);
                cronjobDataMap.put("last_execution_time", cronjobData[5]);
                return cronjobDataMap;
            }).toList();
        } catch (Exception e) {
            log.error("getAllCronjob: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getCronjobChangeHistoryLogList(String cronjobName) {
        try {
            Stream<?> queryStream = em.createNativeQuery("select session_id, start_at, stop_at, executed_by, operation, execute_result from cronjob_change_history where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getResultStream();
            return queryStream.map(record -> {
                Object[] cronjobChangeValues = (Object[]) record;
                Map<String, Object> cronjobChangeHistoryMap = new HashMap<>();
                cronjobChangeHistoryMap.put("session_id", cronjobChangeValues[0]);
                cronjobChangeHistoryMap.put("start_at", cronjobChangeValues[1]);
                cronjobChangeHistoryMap.put("stop_at", cronjobChangeValues[2]);
                cronjobChangeHistoryMap.put("executed_by", cronjobChangeValues[3]);
                cronjobChangeHistoryMap.put("operation", cronjobChangeValues[4]);
                cronjobChangeHistoryMap.put("execute_result", cronjobChangeValues[5]);
                return cronjobChangeHistoryMap;
            }).toList();
        } catch (Exception e) {
            log.error("getCronjobChangeHistoryLogList: {}", e.getMessage());
            return new ArrayList<>();
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
            }).toList();
        } catch (Exception e) {
            log.error("getLatestLogList: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

}
