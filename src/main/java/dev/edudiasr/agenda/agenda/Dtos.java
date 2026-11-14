package dev.edudiasr.agenda.agenda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import dev.edudiasr.agenda.dominio.StatusAgendamento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Formatos de entrada e saída da API (records: imutáveis e sem código repetido). */
public final class Dtos {

    private Dtos() {
    }

    public record ServicoResposta(Long id, String nome, int duracaoMinutos, int precoCentavos, boolean ativo) {
    }

    public record ProfissionalResposta(Long id, String nome) {
    }

    public record HorariosResposta(LocalDate data, Long profissionalId, Long servicoId, List<LocalTime> horarios) {
    }

    public record NovoAgendamento(
            @NotNull(message = "Escolha o profissional") Long profissionalId,
            @NotNull(message = "Escolha o serviço") Long servicoId,
            @NotNull(message = "Escolha o horário") LocalDateTime inicio,
            @NotBlank(message = "Informe seu nome") @Size(max = 80, message = "Nome muito longo") String clienteNome,
            @NotBlank(message = "Informe seu telefone")
            @Pattern(regexp = "\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}", message = "Telefone no formato (47) 99999-9999")
            String clienteTelefone) {
    }

    public record AgendamentoResposta(
            Long id,
            String codigo,
            String profissional,
            String servico,
            int precoCentavos,
            String clienteNome,
            String clienteTelefone,
            LocalDateTime inicio,
            LocalDateTime fim,
            StatusAgendamento status) {
    }

    public record NovoServico(
            @NotBlank(message = "Informe o nome") @Size(max = 80) String nome,
            @Min(value = 15, message = "No mínimo 15 minutos") @Max(value = 240, message = "No máximo 4 horas")
            int duracaoMinutos,
            @Min(value = 0, message = "Preço não pode ser negativo") int precoCentavos,
            boolean ativo) {
    }

    public record MudancaDeStatus(@NotNull(message = "Informe o novo status") StatusAgendamento status) {
    }
}
