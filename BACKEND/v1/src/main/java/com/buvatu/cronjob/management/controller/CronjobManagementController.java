package com.buvatu.cronjob.management.controller;

import com.buvatu.cronjob.management.exception.BusinessException;
import com.buvatu.cronjob.management.service.CronjobManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RestControllerAdvice
@CrossOrigin
public class CronjobManagementController {

    private final CronjobManagementService cronjobManagementService;

    public CronjobManagementController(CronjobManagementService cronjobManagementService) {
        this.cronjobManagementService = cronjobManagementService;
    }

    @PutMapping("/cronjob/{cronjobName}/poolSize")
    public void updatePoolSize(@PathVariable String cronjobName, @RequestParam Integer poolSize, @RequestParam String executor, @RequestParam(required = false, defaultValue = "") String description) {
        cronjobManagementService.updatePoolSize(cronjobName, poolSize, executor, description);
    }

    @PutMapping("/cronjob/{cronjobName}/expression")
    public void updatePoolSize(@PathVariable String cronjobName, @RequestParam String expression, @RequestParam String executor, @RequestParam(required = false, defaultValue = "") String description) {
        cronjobManagementService.updateExpression(cronjobName, expression, executor, description);
    }

    @PostMapping("/cronjob/{cronjobName}/schedule")
    public void schedule(@PathVariable String cronjobName, @RequestParam String executor, @RequestParam(required = false, defaultValue = "") String description) {
        cronjobManagementService.schedule(cronjobName, executor, description);
    }

    @PostMapping("/cronjob/{cronjobName}/cancel")
    public void cancel(@PathVariable String cronjobName, @RequestParam String executor, @RequestParam(required = false, defaultValue = "") String description) {
        cronjobManagementService.cancel(cronjobName, executor, description);
    }

    @PostMapping("/cronjob/{cronjobName}/start")
    public void startCronjobManually(@PathVariable String cronjobName, @RequestParam String executor, @RequestParam(required = false, defaultValue = "") String description) {
        cronjobManagementService.forceStart(cronjobName, executor, description);
    }

    @PostMapping("/cronjob/{cronjobName}/stop")
    public void stopRunningCronjob(@PathVariable String cronjobName, @RequestParam String executor, @RequestParam(required = false, defaultValue = "") String description) {
        cronjobManagementService.forceStop(cronjobName, executor, description);
    }

    @GetMapping("/cronjob/list")
    public ResponseEntity<?> getCronjobList() {
        return ResponseEntity.ok(cronjobManagementService.getAllCronjob());
    }

    @GetMapping("/cronjob/{cronjobName}/history/logs")
    public ResponseEntity<?> getChangeHistoryList(@PathVariable String cronjobName) {
        return ResponseEntity.ok(cronjobManagementService.getChangeHistoryList(cronjobName));
    }

    @GetMapping("/cronjob/{cronjobName}/tracing/logs")
    public ResponseEntity<?> getTracingLogList(@PathVariable String cronjobName, @RequestParam UUID sessionId) {
        return ResponseEntity.ok(cronjobManagementService.getTracingLogList(cronjobName, sessionId));
    }

    @GetMapping("/cronjob/{cronjobName}/history/running")
    public ResponseEntity<?> getAllRunningHistory(@PathVariable String cronjobName) {
        return ResponseEntity.ok(cronjobManagementService.getAllRunningHistory(cronjobName));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getCode()).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        return ResponseEntity.internalServerError().body(ex);
    }

}
