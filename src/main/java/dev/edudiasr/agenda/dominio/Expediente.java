package dev.edudiasr.agenda.dominio;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Horário de trabalho de um profissional num dia da semana. */
@Entity
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profissional_id")
    private Long profissionalId;

    /** 1 = segunda ... 7 = domingo (mesmo número do java.time.DayOfWeek) */
    @Column(name = "dia_semana")
    private short diaSemana;

    private LocalTime inicio;

    private LocalTime fim;

    protected Expediente() {
    }

    public LocalTime getInicio() { return inicio; }
    public LocalTime getFim() { return fim; }
}
