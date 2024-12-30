package com.buvatu.cronjob.management.repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import com.buvatu.cronjob.management.model.CronjobConstant;
import com.buvatu.cronjob.management.model.CronjobStatus;
import com.buvatu.cronjob.management.model.ExecutionResult;
import com.buvatu.cronjob.management.model.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository @Slf4j
public class CronjobManagementRepository {

    @PersistenceContext
    private EntityManager em;

    public boolean isCronjobExist(String cronjobName) {
        return (boolean) em.createNativeQuery("select exists(select 1 from cronjob_config where cronjob_name = :cronjobName)").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getSingleResult();
    }

    @Transactional
    public void insertCronjobConfig(String cronjobName) {
        try {
            em.createNativeQuery("insert into cronjob_config(cronjob_name, cronjob_status, current_session_id) values (:cronjobName, :status, :sessionId)")
                   .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                   .setParameter(CronjobConstant.STATUS, CronjobStatus.UNSCHEDULED.name())
                   .setParameter(CronjobConstant.SESSION_ID, UUID.randomUUID().toString())
              .executeUpdate();
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
    public void updateCronjobExpression(String cronjobName, String expression, String executor) {
        try {
            em.createNativeQuery("update cronjob_config set cronjob_expression = :expression, updated_user = :executor where cronjob_name = :cronjobName")
                    .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                    .setParameter("expression", expression)
                    .setParameter("updated_user", executor)
                    .executeUpdate();
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
    public void updateCronjobPoolSize(String cronjobName, Integer poolSize, String executor) {
        try {
            em.createNativeQuery("update cronjob_config set cronjob_pool_size = :poolSize, updated_user = :executor where cronjob_name = :cronjobName")
                    .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                    .setParameter("poolSize", poolSize)
                    .setParameter("updated_user", executor)
                    .executeUpdate();
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
    public void updateCronjobStatus(String cronjobName, String status) {
        try {
            em.createNativeQuery("update cronjob_config set cronjob_status = :status where cronjob_name = :cronjobName").setParameter(CronjobConstant.STATUS, status).setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).executeUpdate();
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
    public void updateCronjobSessionId(String cronjobName, String sessionId) {
        try {
            em.createNativeQuery("update cronjob_config set current_session_id = :sessionId where cronjob_name = :cronjobName").setParameter(CronjobConstant.SESSION_ID, sessionId).setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("updateCronjobSessionId: {}", e.getMessage());
        }
    }

    @Transactional
    public void insertTracingLog(String cronjobName, String sessionId, String activityName, Integer progressValue) {
        try {
            em.createNativeQuery("insert into cronjob_running_log(cronjob_name, session_id, activity_name, progress_value) values (:cronjobName, :sessionId, :activityName, :progressValue)")
                    .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                    .setParameter(CronjobConstant.SESSION_ID, sessionId)
                    .setParameter(CronjobConstant.ACTIVITY_NAME, activityName)
                    .setParameter(CronjobConstant.PROGRESS_VALUE, progressValue)
              .executeUpdate();
            em.flush();
        } catch (Exception e) {
            log.error("insertTracingLog: {}", e.getMessage());
        }
    }

    @Transactional
    public void insertCronjobChangeHistoryLog(String cronjobName, String sessionId, LocalDateTime startTime, String executor, Operation operation, ExecutionResult executionResult, String description) {
        try {
            em.createNativeQuery("insert into cronjob_change_history(cronjob_name, session_id, start_time, executor, operation, execution_result, description) values (:cronjobName, :sessionId, :startTime, :executor, :operation, :executionResult, :description)")
                    .setParameter(CronjobConstant.CRONJOB_NAME, cronjobName)
                    .setParameter(CronjobConstant.SESSION_ID, sessionId)
                    .setParameter(CronjobConstant.START_TIME, startTime)
                    .setParameter(CronjobConstant.EXECUTOR, executor)
                    .setParameter(CronjobConstant.OPERATION, operation.name())
                    .setParameter(CronjobConstant.EXECUTION_RESULT, executionResult.name())
                    .setParameter(CronjobConstant.DESCRIPTION, description)
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
            Stream<?> queryStream = em.createNativeQuery("select session_id, start_time, stop_time, executor, operation, execution_result, description from cronjob_change_history where cronjob_name = :cronjobName").setParameter(CronjobConstant.CRONJOB_NAME, cronjobName).getResultStream();
            return queryStream.map(recordData -> {
                Object[] cronjobChangeValues = (Object[]) recordData;
                Map<String, Object> cronjobChangeHistoryMap = new HashMap<>();
                cronjobChangeHistoryMap.put(CronjobConstant.CRONJOB_NAME, cronjobName);
                cronjobChangeHistoryMap.put(CronjobConstant.SESSION_ID, cronjobChangeValues[0]);
                cronjobChangeHistoryMap.put(CronjobConstant.START_TIME, cronjobChangeValues[1]);
                cronjobChangeHistoryMap.put(CronjobConstant.STOP_TIME, cronjobChangeValues[2]);
                cronjobChangeHistoryMap.put(CronjobConstant.EXECUTOR, cronjobChangeValues[3]);
                cronjobChangeHistoryMap.put(CronjobConstant.OPERATION, cronjobChangeValues[4]);
                cronjobChangeHistoryMap.put(CronjobConstant.EXECUTION_RESULT, cronjobChangeValues[5]);
                cronjobChangeHistoryMap.put(CronjobConstant.DESCRIPTION, cronjobChangeValues[6]);
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
            return queryStream.map(recordData -> {
                Object[] runningLogValues = (Object[]) recordData;
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
