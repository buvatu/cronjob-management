package com.buvatu.cronjob.management.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Step {

    private String jobName;
    private String activityName;
    private Map<String, Activity> nextActivityMap;

}
