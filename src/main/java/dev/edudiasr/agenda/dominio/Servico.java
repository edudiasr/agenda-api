package dev.edudiasr.agenda.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(name = "duracao_minutos")
    private int duracaoMinutos;

    @Column(name = "preco_centavos")
    private int precoCentavos;

    private boolean ativo = true;

    protected Servico() {
    }

    public Servico(String nome, int duracaoMinutos, int precoCentavos) {
        this.nome = nome;
        this.duracaoMinutos = duracaoMinutos;
        this.precoCentavos = precoCentavos;
    }

    public void atualizar(String nome, int duracaoMinutos, int precoCentavos, boolean ativo) {
        this.nome = nome;
        this.duracaoMinutos = duracaoMinutos;
        this.precoCentavos = precoCentavos;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public int getDuracaoMinutos() { return duracaoMinutos; }
    public int getPrecoCentavos() { return precoCentavos; }
    public boolean isAtivo() { return ativo; }
}
