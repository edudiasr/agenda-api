package dev.edudiasr.agenda.dominio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    List<Profissional> findByAtivoTrueOrderByNome();
}
