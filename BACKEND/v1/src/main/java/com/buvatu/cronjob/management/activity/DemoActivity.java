package com.buvatu.cronjob.management.activity;

import com.buvatu.cronjob.management.model.Activity;
import org.springframework.stereotype.Component;

@Component
public class DemoActivity extends Activity {

    public DemoActivity() {
        setTask(this::run);
    }

    private void runStep1() {

    }

    private void runStep2() {

    }

    private String run() {
        runStep1();
        runStep2();
        return "";
    }
}
