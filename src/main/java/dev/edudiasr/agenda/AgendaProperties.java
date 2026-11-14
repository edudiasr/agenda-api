package dev.edudiasr.agenda;

import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurações do negócio, lidas do application.yml (bloco "agenda"). */
@ConfigurationProperties(prefix = "agenda")
public record AgendaProperties(
        ZoneId fuso,
        int antecedenciaMinutos,
        int cancelamentoHoras,
        List<String> corsOrigens,
        String jwtSegredo,
        int tokenHoras,
        String adminEmail,
        String adminSenha) {
}
