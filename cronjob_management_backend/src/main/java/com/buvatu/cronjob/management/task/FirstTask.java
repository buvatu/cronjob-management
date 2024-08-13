package com.buvatu.cronjob.management.task;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

@Component
public class FirstTask implements Callable<String> {

    @Override
    public String call() throws Exception {
        return null;
    }

}
