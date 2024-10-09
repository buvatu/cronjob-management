package com.buvatu.cronjob.management.activity;

import com.buvatu.cronjob.management.model.Activity;
import org.springframework.stereotype.Component;

@Component
public class DemoActivity extends Activity {

    @Override
    public String getTaskExecutionResult() {
        return "";
    }

}
