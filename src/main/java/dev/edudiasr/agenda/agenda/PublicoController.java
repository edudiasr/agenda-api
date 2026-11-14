package dev.edudiasr.agenda.agenda;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.edudiasr.agenda.agenda.Dtos.AgendamentoResposta;
import dev.edudiasr.agenda.agenda.Dtos.HorariosResposta;
import dev.edudiasr.agenda.agenda.Dtos.NovoAgendamento;
import dev.edudiasr.agenda.agenda.Dtos.ProfissionalResposta;
import dev.edudiasr.agenda.agenda.Dtos.ServicoResposta;
import dev.edudiasr.agenda.dominio.ProfissionalRepository;
import dev.edudiasr.agenda.dominio.ServicoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** O que o cliente usa pelo site, sem login. */
@RestController
@RequestMapping("/api")
@Tag(name = "Cliente", description = "Consultar horários e marcar, sem precisar de conta")
public class PublicoController {

    private final AgendaService agenda;
    private final ServicoRepository servicos;
    private final ProfissionalRepository profissionais;

    public PublicoController(AgendaService agenda, ServicoRepository servicos, ProfissionalRepository profissionais) {
        this.agenda = agenda;
        this.servicos = servicos;
        this.profissionais = profissionais;
    }

    @GetMapping("/servicos")
    @Operation(summary = "Serviços oferecidos")
    public List<ServicoResposta> servicos() {
        return servicos.findByAtivoTrueOrderByNome().stream()
                .map(s -> new ServicoResposta(s.getId(), s.getNome(), s.getDuracaoMinutos(), s.getPrecoCentavos(), s.isAtivo()))
                .toList();
    }

    @GetMapping("/profissionais")
    @Operation(summary = "Profissionais")
    public List<ProfissionalResposta> profissionais() {
        return profissionais.findByAtivoTrueOrderByNome().stream()
                .map(p -> new ProfissionalResposta(p.getId(), p.getNome()))
                .toList();
    }

    @GetMapping("/horarios")
    @Operation(summary = "Horários livres de um profissional, pra um serviço, num dia")
    public HorariosResposta horarios(@RequestParam Long profissionalId, @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return new HorariosResposta(data, profissionalId, servicoId, agenda.horariosLivres(profissionalId, servicoId, data));
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Marcar horário", description = "Devolve o código que o cliente usa pra consultar ou cancelar")
    public AgendamentoResposta agendar(@Valid @RequestBody NovoAgendamento pedido) {
        return agenda.agendar(pedido);
    }

    @GetMapping("/agendamentos/{codigo}")
    @Operation(summary = "Consultar pelo código")
    public AgendamentoResposta consultar(@PathVariable String codigo) {
        return agenda.consultar(codigo);
    }

    @PostMapping("/agendamentos/{codigo}/cancelar")
    @Operation(summary = "Cancelar pelo código", description = "Permitido até algumas horas antes do horário")
    public AgendamentoResposta cancelar(@PathVariable String codigo) {
        return agenda.cancelarPeloCliente(codigo);
    }
}
