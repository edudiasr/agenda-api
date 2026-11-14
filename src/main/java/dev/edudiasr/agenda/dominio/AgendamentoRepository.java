package dev.edudiasr.agenda.dominio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    Optional<Agendamento> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    /** Horários que ocupam a agenda do profissional dentro do intervalo (cancelados liberam o horário). */
    @Query("""
            select a from Agendamento a
            where a.profissional.id = :profissionalId
              and a.status <> dev.edudiasr.agenda.dominio.StatusAgendamento.CANCELADO
              and a.inicio < :ate and a.fim > :de
            """)
    List<Agendamento> ocupados(Long profissionalId, Instant de, Instant ate);

    /** Agenda do dia pra tela do dono, já trazendo profissional e serviço numa consulta só. */
    @Query("""
            select a from Agendamento a
            join fetch a.profissional join fetch a.servico
            where a.inicio >= :de and a.inicio < :ate
            order by a.inicio, a.profissional.nome
            """)
    List<Agendamento> doPeriodo(Instant de, Instant ate);
}
