package org.itmo.secs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactivefeign.spring.config.EnableReactiveFeignClients;

@SpringBootApplication
@EnableReactiveFeignClients(basePackages = "org.itmo.secs.client")
public class App {
    public static void main (String[] args) {
        SpringApplication.run(App.class, args);
    }
}
