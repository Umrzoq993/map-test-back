package com.agri.mapapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI mapAppOpenAPI() {
        // HTTP Basic (username + password) security scheme
        SecurityScheme basicAuthScheme = new SecurityScheme()
                .name("basicAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic"); // <— muhim: basic

        return new OpenAPI()
                // Har bir endpoint uchun default security talabi sifatida basicAuth qo‘shamiz
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("basicAuth", basicAuthScheme))
                .info(new Info()
                        .title("Zamin APP API")
                        .description("Agri MapApp backend API hujjati")
                        .version("v1.0"));
    }
}
