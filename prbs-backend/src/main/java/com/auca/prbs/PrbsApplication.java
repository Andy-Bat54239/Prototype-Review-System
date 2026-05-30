package com.auca.prbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrbsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrbsApplication.class, args);
    }
}
