package dev.edudiasr.agenda.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.edudiasr.agenda.agenda.CalculadoraDeHorarios.Intervalo;

class CalculadoraDeHorariosTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 17);

    private static LocalDateTime as(String hora) {
        return DIA.atTime(LocalTime.parse(hora));
    }

    private static List<LocalTime> horas(String... horas) {
        return java.util.Arrays.stream(horas).map(LocalTime::parse).toList();
    }

    @Test
    void diaLivreTemHorarioDe15Em15MinutosAteOServicoCaberNoExpediente() {
        var livres = CalculadoraDeHorarios.livres(as("09:00"), as("10:00"), 30, List.of(), as("00:00"));
        assertThat(livres).isEqualTo(horas("09:00", "09:15", "09:30"));
    }

    @Test
    void servicoMaiorQueOExpedienteNaoTemHorario() {
        assertThat(CalculadoraDeHorarios.livres(as("09:00"), as("09:45"), 60, List.of(), as("00:00"))).isEmpty();
    }

    @Test
    void horarioOcupadoBloqueiaTudoQueEncostaNele() {
        var ocupado = new Intervalo(as("10:00"), as("10:30"));
        var livres = CalculadoraDeHorarios.livres(as("09:00"), as("11:30"), 30, List.of(ocupado), as("00:00"));
        // 09:45 terminaria às 10:15 (dentro do ocupado); 10:15 começaria dentro dele
        assertThat(livres).isEqualTo(horas("09:00", "09:15", "09:30", "10:30", "10:45", "11:00"));
    }

    @Test
    void terminarExatamenteQuandoOutroComecaNaoEConflito() {
        var ocupado = new Intervalo(as("10:00"), as("11:00"));
        var livres = CalculadoraDeHorarios.livres(as("09:00"), as("12:00"), 60, List.of(ocupado), as("00:00"));
        assertThat(livres).contains(LocalTime.parse("09:00"), LocalTime.parse("11:00"));
        assertThat(livres).doesNotContain(LocalTime.parse("09:15"), LocalTime.parse("10:45"));
    }

    @Test
    void horariosAntesDoLimiteDeAntecedenciaSomem() {
        var livres = CalculadoraDeHorarios.livres(as("09:00"), as("11:00"), 30, List.of(), as("09:40"));
        assertThat(livres).isEqualTo(horas("09:45", "10:00", "10:15", "10:30"));
    }

    @Test
    void variosOcupadosNoMesmoDia() {
        var ocupados = List.of(new Intervalo(as("09:00"), as("09:30")), new Intervalo(as("10:00"), as("11:00")));
        var livres = CalculadoraDeHorarios.livres(as("09:00"), as("11:30"), 30, ocupados, as("00:00"));
        assertThat(livres).isEqualTo(horas("09:30", "11:00"));
    }
}
