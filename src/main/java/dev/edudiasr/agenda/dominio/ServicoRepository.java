package dev.edudiasr.agenda.dominio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    List<Servico> findByAtivoTrueOrderByNome();

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
