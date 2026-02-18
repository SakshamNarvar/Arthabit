package com.nstrange.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"com.nstrange.authservice.repository"})
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
