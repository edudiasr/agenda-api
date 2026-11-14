package dev.edudiasr.agenda;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Swagger em /swagger-ui.html, com o botão "Authorize" pra colar o token do login. */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Agenda API",
        description = "Agendamento online para barbearias e salões: horários livres, marcação, cancelamento e agenda do dia.",
        version = "1.0"))
@SecurityScheme(name = "token", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class DocumentacaoConfig {
}
