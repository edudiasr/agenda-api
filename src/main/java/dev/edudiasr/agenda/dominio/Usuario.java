package dev.edudiasr.agenda.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Quem administra a agenda (dono ou recepção). Cliente não tem conta. */
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String nome;

    @Column(name = "senha_hash")
    private String senhaHash;

    protected Usuario() {
    }

    public Usuario(String email, String nome, String senhaHash) {
        this.email = email;
        this.nome = nome;
        this.senhaHash = senhaHash;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNome() { return nome; }
    public String getSenhaHash() { return senhaHash; }
}
