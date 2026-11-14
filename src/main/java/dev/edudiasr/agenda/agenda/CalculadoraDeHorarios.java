package dev.edudiasr.agenda.agenda;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula os horários livres de um dia. Não fala com banco nem com relógio: recebe tudo
 * pronto, por isso dá pra testar cada regra isoladamente.
 *
 * Os horários começam de 15 em 15 minutos a partir do início do expediente. Um horário serve
 * se o serviço inteiro cabe antes do fim do expediente, não encosta em nenhum horário ocupado
 * e começa depois do limite de antecedência.
 */
public final class CalculadoraDeHorarios {

    public static final int PASSO_MINUTOS = 15;

    public record Intervalo(LocalDateTime inicio, LocalDateTime fim) {
        boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
            // [a, b) e [c, d) se sobrepõem se a < d e c < b; encostar (fim == início) não conta
            return inicio.isBefore(outroFim) && outroInicio.isBefore(fim);
        }
    }

    private CalculadoraDeHorarios() {
    }

    public static List<LocalTime> livres(LocalDateTime inicioExpediente, LocalDateTime fimExpediente,
            int duracaoMinutos, List<Intervalo> ocupados, LocalDateTime naoAntesDe) {
        List<LocalTime> livres = new ArrayList<>();
        for (LocalDateTime inicio = inicioExpediente;
                !inicio.plusMinutes(duracaoMinutos).isAfter(fimExpediente);
                inicio = inicio.plusMinutes(PASSO_MINUTOS)) {

            LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);
            if (inicio.isBefore(naoAntesDe)) {
                continue;
            }
            final LocalDateTime candidato = inicio;
            boolean conflita = ocupados.stream().anyMatch(o -> o.sobrepoe(candidato, fim));
            if (!conflita) {
                livres.add(inicio.toLocalTime());
            }
        }
        return livres;
    }
}
