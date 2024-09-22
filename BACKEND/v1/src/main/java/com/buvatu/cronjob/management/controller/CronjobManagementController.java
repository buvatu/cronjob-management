package com.buvatu.cronjob.management.controller;

import com.buvatu.cronjob.management.service.CronjobManagementService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
public class CronjobManagementController {

    private final CronjobManagementService cronjobManagementService;

    public CronjobManagementController(CronjobManagementService cronjobManagementService) {
        this.cronjobManagementService = cronjobManagementService;
    }

    @GetMapping(path = "/stream-cronjob-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<List<Map<String, Object>>> streamCronjobStatus() {
        return Flux.interval(Duration.ofSeconds(1)).map(sequence -> cronjobManagementService.getLatestLogList());
    }

    @PostMapping("/cronjob/{cronjobName}/schedule")
    public void scheduleCronjob(@PathVariable("cronjobName") String cronjobName, @RequestParam  String updatedExpression) {
        cronjobManagementService.schedule(cronjobName, updatedExpression);
    }

    @PostMapping("/cronjob/{cronjobName}/cancel")
    public void cancelScheduleCronjob(@PathVariable("cronjobName") String cronjobName) {
        cronjobManagementService.cancelSchedule(cronjobName);
    }

    @PostMapping("/cronjob/{cronjobName}/start")
    public void startCronjobManually(@PathVariable("cronjobName") String cronjobName) {
        cronjobManagementService.forceStart(cronjobName);
    }

    @PostMapping("/cronjob/{cronjobName}/stop")
    public void stopCronjob(@PathVariable("cronjobName") String cronjobName) {
        cronjobManagementService.forceStop(cronjobName);
    }

}
