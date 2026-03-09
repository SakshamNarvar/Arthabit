package com.nstrange.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("REST API for User Service - manages user profiles in the Expense Tracker application")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NStrange")
                                .email("support@nstrange.com")))
                .servers(List.of(
                        new Server().url("http://localhost:9810").description("Local Development Server")
                ));
    }
}

