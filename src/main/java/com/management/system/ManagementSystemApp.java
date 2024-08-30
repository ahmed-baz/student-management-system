package com.management.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ManagementSystemApp {

    public static void main(String[] args) {
        SpringApplication.run(ManagementSystemApp.class, args);
    }

}
