package com.buvatu.cronjob.management.controller;

import com.buvatu.cronjob.management.model.BusinessException;
import com.buvatu.cronjob.management.service.CronjobManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CronjobManagementController {

    private final CronjobManagementService cronjobManagementService;

    public CronjobManagementController(CronjobManagementService cronjobManagementService) {
        this.cronjobManagementService = cronjobManagementService;
    }

    @PostMapping("/cronjob/{cronjobName}/schedule")
    public void schedule(@PathVariable("cronjobName") String cronjobName, @RequestParam(required = false, defaultValue = "") String updatedExpression) {
        cronjobManagementService.schedule(cronjobName, updatedExpression);
    }

    @PostMapping("/cronjob/{cronjobName}/postpone")
    public void postpone(@PathVariable("cronjobName") String cronjobName) {
        cronjobManagementService.postpone(cronjobName);
    }

    @PostMapping("/cronjob/{cronjobName}/start")
    public void startCronjobManually(@PathVariable("cronjobName") String cronjobName) {
        cronjobManagementService.forceStart(cronjobName);
    }

    @PostMapping("/cronjob/{cronjobName}/cancel")
    public void cancel(@PathVariable("cronjobName") String cronjobName) {
        cronjobManagementService.cancel(cronjobName);
    }

    @PostMapping("/cronjob/{cronjobName}/poolSize")
    public void updatePoolSize(@PathVariable("cronjobName") String cronjobName, @RequestParam Integer poolSize) {
        cronjobManagementService.updatePoolSize(cronjobName, poolSize);
    }

//    @GetMapping("/cronjob/list")
//    public ResponseEntity<?> getCronjobList() {
//        return ResponseEntity.ok(cronjobManagementService.getCronjobList());
//    }

//    @GetMapping("/cronjob/logs")
//    public ResponseEntity<?> getActiveLogs() {
//        return ResponseEntity.ok(cronjobManagementService.getActiveLogs());
//    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getCode()).body(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        return ResponseEntity.internalServerError().body(ex);
    }

}
