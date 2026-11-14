package dev.edudiasr.agenda;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(AgendaProperties.class)
public class AgendaApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgendaApiApplication.class, args);
    }

    /** O "agora" vem daqui; nos testes ele é trocado por um relógio parado numa data conhecida. */
    @Bean
    Clock relogio(AgendaProperties props) {
        return Clock.system(props.fuso());
    }
}
