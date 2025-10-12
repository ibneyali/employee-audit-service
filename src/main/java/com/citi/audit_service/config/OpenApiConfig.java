package com.citi.audit_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Employee Audit Service API")
                        .description("REST API for managing employees, departments, addresses, trainings and employee training records")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Audit Service Team")
                                .email("audit-service@citi.com")));
    }
}
