package dev.edudiasr.agenda.agenda;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.edudiasr.agenda.AgendaProperties;
import dev.edudiasr.agenda.agenda.CalculadoraDeHorarios.Intervalo;
import dev.edudiasr.agenda.agenda.Dtos.AgendamentoResposta;
import dev.edudiasr.agenda.agenda.Dtos.NovoAgendamento;
import dev.edudiasr.agenda.dominio.Agendamento;
import dev.edudiasr.agenda.dominio.AgendamentoRepository;
import dev.edudiasr.agenda.dominio.Expediente;
import dev.edudiasr.agenda.dominio.ExpedienteRepository;
import dev.edudiasr.agenda.dominio.Profissional;
import dev.edudiasr.agenda.dominio.ProfissionalRepository;
import dev.edudiasr.agenda.dominio.Servico;
import dev.edudiasr.agenda.dominio.ServicoRepository;
import dev.edudiasr.agenda.dominio.StatusAgendamento;
import dev.edudiasr.agenda.erro.NegocioException;

@Service
public class AgendaService {

    // sem 0/O, 1/I/L: o cliente vai ler o código e às vezes ditar pelo telefone
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;
    private static final int DIAS_A_FRENTE = 60;

    private final AgendamentoRepository agendamentos;
    private final ServicoRepository servicos;
    private final ProfissionalRepository profissionais;
    private final ExpedienteRepository expedientes;
    private final AgendaProperties props;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    public AgendaService(AgendamentoRepository agendamentos, ServicoRepository servicos,
            ProfissionalRepository profissionais, ExpedienteRepository expedientes,
            AgendaProperties props, Clock relogio) {
        this.agendamentos = agendamentos;
        this.servicos = servicos;
        this.profissionais = profissionais;
        this.expedientes = expedientes;
        this.props = props;
        this.relogio = relogio;
    }

    private ZoneId fuso() {
        return props.fuso();
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio.withZone(fuso()));
    }

    @Transactional(readOnly = true)
    public List<LocalTime> horariosLivres(Long profissionalId, Long servicoId, LocalDate data) {
        Profissional profissional = profissionalAtivo(profissionalId);
        Servico servico = servicoAtivo(servicoId);
        if (data.isBefore(agora().toLocalDate()) || data.isAfter(agora().toLocalDate().plusDays(DIAS_A_FRENTE))) {
            return List.of();
        }

        short diaSemana = (short) data.getDayOfWeek().getValue();
        Expediente expediente = expedientes.findByProfissionalIdAndDiaSemana(profissional.getId(), diaSemana).orElse(null);
        if (expediente == null) {
            return List.of(); // não trabalha nesse dia
        }

        LocalDateTime abre = data.atTime(expediente.getInicio());
        LocalDateTime fecha = data.atTime(expediente.getFim());
        List<Intervalo> ocupados = agendamentos.ocupados(profissional.getId(), instante(abre), instante(fecha)).stream()
                .map(a -> new Intervalo(local(a.getInicio()), local(a.getFim())))
                .toList();
        LocalDateTime naoAntesDe = agora().plusMinutes(props.antecedenciaMinutos());

        return CalculadoraDeHorarios.livres(abre, fecha, servico.getDuracaoMinutos(), ocupados, naoAntesDe);
    }

    @Transactional
    public AgendamentoResposta agendar(NovoAgendamento pedido) {
        Profissional profissional = profissionalAtivo(pedido.profissionalId());
        Servico servico = servicoAtivo(pedido.servicoId());
        LocalDateTime inicio = pedido.inicio().withSecond(0).withNano(0);

        // confere de novo no servidor: o front pode estar mostrando uma lista velha
        List<LocalTime> livres = horariosLivres(profissional.getId(), servico.getId(), inicio.toLocalDate());
        if (!livres.contains(inicio.toLocalTime())) {
            throw NegocioException.conflito("Esse horário não está disponível. Escolha outro da lista.");
        }

        Agendamento novo = new Agendamento(novoCodigo(), profissional, servico, pedido.clienteNome().strip(),
                pedido.clienteTelefone().replaceAll("\\D", ""), instante(inicio),
                instante(inicio.plusMinutes(servico.getDuracaoMinutos())), relogio.instant());
        try {
            // saveAndFlush manda o INSERT agora, dentro do try: se dois clientes escolheram o mesmo
            // horário ao mesmo tempo, os dois passam na conferência acima, mas a restrição de
            // exclusão do banco (agendamento_sem_sobreposicao) só deixa um gravar
            agendamentos.saveAndFlush(novo);
        } catch (DataIntegrityViolationException e) {
            throw NegocioException.conflito("Esse horário acabou de ser ocupado. Escolha outro.");
        }
        return resposta(novo);
    }

    @Transactional(readOnly = true)
    public AgendamentoResposta consultar(String codigo) {
        return resposta(porCodigo(codigo));
    }

    @Transactional
    public AgendamentoResposta cancelarPeloCliente(String codigo) {
        Agendamento a = porCodigo(codigo);
        if (a.getStatus() != StatusAgendamento.AGENDADO) {
            throw NegocioException.conflito("Esse agendamento não está mais ativo.");
        }
        Duration faltam = Duration.between(relogio.instant(), a.getInicio());
        if (faltam.compareTo(Duration.ofHours(props.cancelamentoHoras())) < 0) {
            throw NegocioException.conflito("Cancelamento pelo site só até " + props.cancelamentoHoras()
                    + " horas antes. Fale com a barbearia.");
        }
        a.mudarStatus(StatusAgendamento.CANCELADO);
        return resposta(a);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResposta> agendaDoDia(LocalDate data) {
        return agendamentos.doPeriodo(instante(data.atStartOfDay()), instante(data.plusDays(1).atStartOfDay()))
                .stream().map(this::resposta).toList();
    }

    @Transactional
    public AgendamentoResposta mudarStatus(Long id, StatusAgendamento status) {
        Agendamento a = agendamentos.findById(id)
                .orElseThrow(() -> NegocioException.naoEncontrado("Agendamento não encontrado."));
        if (status == StatusAgendamento.AGENDADO) {
            throw NegocioException.invalido("Use CONCLUIDO, FALTOU ou CANCELADO.");
        }
        if (a.getStatus() == StatusAgendamento.CANCELADO) {
            throw NegocioException.conflito("Agendamento cancelado não muda mais de status.");
        }
        a.mudarStatus(status);
        return resposta(a);
    }

    // ------------------------------------------------------------------ auxiliares

    private Profissional profissionalAtivo(Long id) {
        return profissionais.findById(id).filter(Profissional::isAtivo)
                .orElseThrow(() -> NegocioException.naoEncontrado("Profissional não encontrado."));
    }

    private Servico servicoAtivo(Long id) {
        return servicos.findById(id).filter(Servico::isAtivo)
                .orElseThrow(() -> NegocioException.naoEncontrado("Serviço não encontrado."));
    }

    private Agendamento porCodigo(String codigo) {
        return agendamentos.findByCodigo(codigo.strip().toUpperCase())
                .orElseThrow(() -> NegocioException.naoEncontrado("Nenhum agendamento com esse código."));
    }

    private String novoCodigo() {
        String codigo;
        do {
            StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                sb.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
            codigo = sb.toString();
        } while (agendamentos.existsByCodigo(codigo)); // 31^6 ≈ 887 milhões: repetir é raríssimo
        return codigo;
    }

    private Instant instante(LocalDateTime local) {
        return local.atZone(fuso()).toInstant();
    }

    private LocalDateTime local(Instant instante) {
        return LocalDateTime.ofInstant(instante, fuso());
    }

    private AgendamentoResposta resposta(Agendamento a) {
        return new AgendamentoResposta(a.getId(), a.getCodigo(), a.getProfissional().getNome(),
                a.getServico().getNome(), a.getServico().getPrecoCentavos(), a.getClienteNome(),
                a.getClienteTelefone(), local(a.getInicio()), local(a.getFim()), a.getStatus());
    }
}
