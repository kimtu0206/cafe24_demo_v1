package org.example.cafe24_demo_v1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Cafe24DemoV1Application {

    public static void main(String[] args) {
        SpringApplication.run(Cafe24DemoV1Application.class, args);
    }
}