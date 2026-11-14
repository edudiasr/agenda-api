package dev.edudiasr.agenda;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Relógio que só anda quando o teste manda. */
public class RelogioDeTeste extends Clock {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Instant agora;

    public RelogioDeTeste(LocalDateTime inicio) {
        this.agora = inicio.atZone(FUSO).toInstant();
    }

    public void ir(LocalDateTime momento) {
        this.agora = momento.atZone(FUSO).toInstant();
    }

    public void avancar(Duration tempo) {
        this.agora = agora.plus(tempo);
    }

    @Override
    public ZoneId getZone() {
        return FUSO;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this; // os testes rodam sempre no fuso da barbearia
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
