package org.itmo.user.accounter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().info(new Info()
                .title("User accounter API")
                .description("User accounter Service API")
                .version("1.0.3")
        )
                .addServersItem(
                        new Server().url("http://localhost:8080")
                );
    }
}
