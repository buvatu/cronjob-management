package com.buvatu.cronjob.management.task;

import com.buvatu.cronjob.management.model.Activity;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class DemoTask extends Activity implements Callable<String> {

    @Override
    public String call() throws Exception {
        return "";
    }

}
