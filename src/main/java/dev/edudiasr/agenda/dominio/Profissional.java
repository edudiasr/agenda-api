package dev.edudiasr.agenda.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private boolean ativo = true;

    protected Profissional() {
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
}
