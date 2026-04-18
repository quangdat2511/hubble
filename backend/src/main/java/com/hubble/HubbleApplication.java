package com.hubble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HubbleApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubbleApplication.class, args);
    }

}