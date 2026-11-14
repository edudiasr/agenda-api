package dev.edudiasr.agenda.agenda;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.edudiasr.agenda.agenda.Dtos.AgendamentoResposta;
import dev.edudiasr.agenda.agenda.Dtos.MudancaDeStatus;
import dev.edudiasr.agenda.agenda.Dtos.NovoServico;
import dev.edudiasr.agenda.agenda.Dtos.ServicoResposta;
import dev.edudiasr.agenda.dominio.Servico;
import dev.edudiasr.agenda.dominio.ServicoRepository;
import dev.edudiasr.agenda.erro.NegocioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Área do dono: tudo aqui exige o token do login. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administração", description = "Agenda do dia e cadastro de serviços (exige login)")
@SecurityRequirement(name = "token")
public class AdminController {

    private final AgendaService agenda;
    private final ServicoRepository servicos;

    public AdminController(AgendaService agenda, ServicoRepository servicos) {
        this.agenda = agenda;
        this.servicos = servicos;
    }

    @GetMapping("/agenda")
    @Operation(summary = "Todos os agendamentos de um dia")
    public List<AgendamentoResposta> agenda(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return agenda.agendaDoDia(data);
    }

    @PatchMapping("/agendamentos/{id}/status")
    @Operation(summary = "Marcar como concluído, falta ou cancelado")
    public AgendamentoResposta status(@PathVariable Long id, @Valid @RequestBody MudancaDeStatus mudanca) {
        return agenda.mudarStatus(id, mudanca.status());
    }

    @GetMapping("/servicos")
    @Operation(summary = "Todos os serviços, inclusive os desativados")
    public List<ServicoResposta> servicos() {
        return servicos.findAll().stream().map(AdminController::resposta).toList();
    }

    @PostMapping("/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar serviço")
    @Transactional
    public ServicoResposta criar(@Valid @RequestBody NovoServico novo) {
        validar(novo, 0L);
        return resposta(servicos.save(new Servico(novo.nome().strip(), novo.duracaoMinutos(), novo.precoCentavos())));
    }

    @PutMapping("/servicos/{id}")
    @Operation(summary = "Alterar serviço (ou desativar, com ativo = false)")
    @Transactional
    public ServicoResposta alterar(@PathVariable Long id, @Valid @RequestBody NovoServico dados) {
        Servico servico = servicos.findById(id).orElseThrow(() -> NegocioException.naoEncontrado("Serviço não encontrado."));
        validar(dados, id);
        servico.atualizar(dados.nome().strip(), dados.duracaoMinutos(), dados.precoCentavos(), dados.ativo());
        return resposta(servico);
    }

    private void validar(NovoServico dados, Long id) {
        if (dados.duracaoMinutos() % CalculadoraDeHorarios.PASSO_MINUTOS != 0) {
            throw NegocioException.invalido("A duração precisa ser múltipla de 15 minutos.");
        }
        if (servicos.existsByNomeIgnoreCaseAndIdNot(dados.nome().strip(), id)) {
            throw NegocioException.conflito("Já existe um serviço com esse nome.");
        }
    }

    private static ServicoResposta resposta(Servico s) {
        return new ServicoResposta(s.getId(), s.getNome(), s.getDuracaoMinutos(), s.getPrecoCentavos(), s.isAtivo());
    }
}
