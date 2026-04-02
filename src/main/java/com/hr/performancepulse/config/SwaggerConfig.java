package com.hr.performancepulse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3 configuration.
 *
 * <p><b>LLD §12.2</b> – Swagger UI available at:
 * {@code http://localhost:8080/swagger-ui/index.html}
 *
 * <p>All controllers will be tagged via {@code @Tag(name = "...")}
 * and responses documented with {@code @ApiResponse} as each
 * module is implemented per the LLD.
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Talent Performance Pulse API")
                        .description("""
                                Backend API for the HR Performance Tracking platform.
                                Manages employees, review cycles, performance reviews, and goals.
                                """)
                        .version("v0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("HR Platform Team")
                                .email("hr-platform@company.com"))
                        .license(new License()
                                .name("Internal – Confidential")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local / Docker development server")));
    }
}
