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

import com.buvatu.cronjob.management.model.CronjobConstant;
import com.buvatu.cronjob.management.model.CronjobStatus;
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
                    .setParameter(CronjobConstant.SESSION_ID, sessionId)
                    .setParameter("activityName", activityName)
                    .setParameter("progressValue", progressValue)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertTracingLog: {}", e.getMessage());
        }
    }

    @Transactional
    public void insertCronjobChangeHistoryLog(String cronjobName, String sessionId, LocalDateTime startTime, String operation, String executor, String executionResult) {
        try {
            em.createNativeQuery("insert into cronjob_change_history(cronjob_name, session_id, start_time, operation, executor, execution_result) values (:cronjobName, :sessionId, :startTime, :operation, :executor, :executionResult)")
                    .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                    .setParameter(CronjobConstant.SESSION_ID, sessionId)
                    .setParameter(CronjobConstant.START_TIME, startTime)
                    .setParameter(CronjobConstant.OPERATION, operation)
                    .setParameter(CronjobConstant.EXECUTOR, executor)
                    .setParameter(CronjobConstant.EXECUTION_RESULT, executionResult)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertCronjobChangeHistoryLog: {}", e.getMessage());
        }
    }

    public LocalDateTime getLastExecutionTime(String cronjobName) {
        try {
            return (LocalDateTime) em.createNativeQuery("select start_at from cronjob_change_history where (operation = 'RUN JOB ON A SCHEDULE' or operation = 'START JOB MANUALLY') and cronjob_name = :cronjobName order by id desc limit 1").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getSingleResult();
        } catch (Exception e) {
            log.error("getLastExecutionTime: {}", e.getMessage());
            return null;
        }
    }

    public List<Map<String, Object>> getCronjobChangeHistoryLogList(String cronjobName) {
        try {
            Stream<?> queryStream = em.createNativeQuery("select session_id, start_time, stop_time, executor, operation, execution_result from cronjob_change_history where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getResultStream();
            return queryStream.map(record -> {
                Object[] cronjobChangeValues = (Object[]) record;
                Map<String, Object> cronjobChangeHistoryMap = new HashMap<>();
                cronjobChangeHistoryMap.put(CronjobConstant.CRONJOB_NAME, cronjobName);
                cronjobChangeHistoryMap.put(CronjobConstant.SESSION_ID, cronjobChangeValues[0]);
                cronjobChangeHistoryMap.put(CronjobConstant.START_TIME, cronjobChangeValues[1]);
                cronjobChangeHistoryMap.put(CronjobConstant.STOP_TIME, cronjobChangeValues[2]);
                cronjobChangeHistoryMap.put(CronjobConstant.EXECUTOR, cronjobChangeValues[3]);
                cronjobChangeHistoryMap.put(CronjobConstant.OPERATION, cronjobChangeValues[4]);
                cronjobChangeHistoryMap.put(CronjobConstant.EXECUTION_RESULT, cronjobChangeValues[5]);
                return cronjobChangeHistoryMap;
            }).toList();
        } catch (Exception e) {
            log.error("getCronjobChangeHistoryLogList: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getCronjobRunningLogList(String sessionId) {
        try {
            Stream<?> queryStream = em.createNativeQuery("select cronjob_name, activity_name, progress_value from cronjob_running_log where session_id = :sessionId").setParameter(CronjobConstant.SESSION_ID, sessionId).getResultStream();
            return queryStream.map(record -> {
                Object[] runningLogValues = (Object[]) record;
                Map<String, Object> cronjobRunningLogMap = new HashMap<>();
                cronjobRunningLogMap.put(CronjobConstant.CRONJOB_NAME, runningLogValues[0]);
                cronjobRunningLogMap.put(CronjobConstant.SESSION_ID, sessionId);
                cronjobRunningLogMap.put(CronjobConstant.ACTIVITY_NAME, runningLogValues[1]);
                cronjobRunningLogMap.put(CronjobConstant.PROGRESS_VALUE, runningLogValues[2]);
                return cronjobRunningLogMap;
            }).toList();
        } catch (Exception e) {
            log.error("getCronjobRunningLogList: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

}
