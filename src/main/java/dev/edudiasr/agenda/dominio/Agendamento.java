package dev.edudiasr.agenda.dominio;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código que o cliente recebe pra consultar ou cancelar, sem precisar de conta. */
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profissional_id")
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @Column(name = "cliente_nome")
    private String clienteNome;

    @Column(name = "cliente_telefone")
    private String clienteTelefone;

    private Instant inicio;

    private Instant fim;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    @Column(name = "criado_em")
    private Instant criadoEm;

    protected Agendamento() {
    }

    public Agendamento(String codigo, Profissional profissional, Servico servico, String clienteNome,
            String clienteTelefone, Instant inicio, Instant fim, Instant criadoEm) {
        this.codigo = codigo;
        this.profissional = profissional;
        this.servico = servico;
        this.clienteNome = clienteNome;
        this.clienteTelefone = clienteTelefone;
        this.inicio = inicio;
        this.fim = fim;
        this.criadoEm = criadoEm;
    }

    public void mudarStatus(StatusAgendamento novo) {
        this.status = novo;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Profissional getProfissional() { return profissional; }
    public Servico getServico() { return servico; }
    public String getClienteNome() { return clienteNome; }
    public String getClienteTelefone() { return clienteTelefone; }
    public Instant getInicio() { return inicio; }
    public Instant getFim() { return fim; }
    public StatusAgendamento getStatus() { return status; }
}
