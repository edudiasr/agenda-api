package dev.edudiasr.agenda.dominio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    Optional<Expediente> findByProfissionalIdAndDiaSemana(Long profissionalId, short diaSemana);
}
