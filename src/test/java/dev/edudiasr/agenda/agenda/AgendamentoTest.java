package dev.edudiasr.agenda.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import dev.edudiasr.agenda.RelogioDeTeste;
import dev.edudiasr.agenda.TestcontainersConfiguration;

/**
 * Testes pela API, de ponta a ponta, com PostgreSQL de verdade (Testcontainers).
 * O relógio fica parado numa segunda-feira, 16/11/2026, às 8h. Rafael (id 1) trabalha de terça a
 * sábado das 9h às 18h; Bruno (id 2) de segunda a sexta das 10h às 19h. Corte (id 1) leva 30 min.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, AgendamentoTest.Relogio.class })
@TestPropertySource(properties = { "agenda.admin-email=dono@barbearia.test", "agenda.admin-senha=senha-do-dono-123" })
class AgendamentoTest {

    private static final LocalDateTime SEGUNDA_8H = LocalDateTime.of(2026, 11, 16, 8, 0);
    private static final String TERCA = "2026-11-17";

    @TestConfiguration
    static class Relogio {
        @Bean
        @Primary
        RelogioDeTeste relogioDeTeste() {
            return new RelogioDeTeste(SEGUNDA_8H);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired RelogioDeTeste relogio;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        jdbc.update("DELETE FROM agendamento");
        relogio.ir(SEGUNDA_8H);
    }

    private String marcar(String inicio, long profissional, long servico) {
        return """
                {"profissionalId": %d, "servicoId": %d, "inicio": "%s",
                 "clienteNome": "Lobo", "clienteTelefone": "(47) 99999-1234"}
                """.formatted(profissional, servico, inicio);
    }

    private MvcResult agendar(String inicio) throws Exception {
        return mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(marcar(inicio, 1, 1))).andReturn();
    }

    private String token() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"dono@barbearia.test\", \"senha\": \"senha-do-dono-123\"}"))
                .andExpect(status().isOk()).andReturn();
        return "Bearer " + JsonPath.read(r.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void listaServicosEProfissionaisDoExemplo() throws Exception {
        mvc.perform(get("/api/servicos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].nome").value("Barba"));
        mvc.perform(get("/api/profissionais")).andExpect(jsonPath("$[*].nome").value(hasItem("Rafael")));
    }

    @Test
    void marcarHorarioDevolveCodigoETiraOHorarioDaLista() throws Exception {
        mvc.perform(get("/api/horarios").param("profissionalId", "1").param("servicoId", "1").param("data", TERCA))
                .andExpect(jsonPath("$.horarios[0]").value("09:00:00"))
                .andExpect(jsonPath("$.horarios").value(hasItem("10:00:00")));

        MvcResult r = agendar(TERCA + "T10:00:00");
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        String codigo = JsonPath.read(r.getResponse().getContentAsString(), "$.codigo");
        assertThat(codigo).matches("[A-Z2-9]{6}");

        mvc.perform(get("/api/horarios").param("profissionalId", "1").param("servicoId", "1").param("data", TERCA))
                .andExpect(jsonPath("$.horarios").value(not(hasItem("10:00:00"))))
                .andExpect(jsonPath("$.horarios").value(not(hasItem("09:45:00")))) // terminaria às 10:15
                .andExpect(jsonPath("$.horarios").value(hasItem("10:30:00")));

        mvc.perform(get("/api/agendamentos/" + codigo.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profissional").value("Rafael"))
                .andExpect(jsonPath("$.clienteTelefone").value("47999991234"))
                .andExpect(jsonPath("$.fim").value("2026-11-17T10:30:00"));
    }

    @Test
    void mesmoHorarioDuasVezesDaConflito() throws Exception {
        assertThat(agendar(TERCA + "T10:00:00").getResponse().getStatus()).isEqualTo(201);
        MvcResult segundo = agendar(TERCA + "T10:15:00");
        assertThat(segundo.getResponse().getStatus()).isEqualTo(409);
        assertThat(segundo.getResponse().getContentAsString()).contains("não está disponível");
    }

    @Test
    void dezClientesNoMesmoHorarioAoMesmoTempoSoUmConsegue() throws Exception {
        int clientes = 10;
        ExecutorService pool = Executors.newFixedThreadPool(clientes);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (int i = 0; i < clientes; i++) {
            Callable<Integer> tentativa = () -> {
                largada.await();
                return agendar(TERCA + "T14:00:00").getResponse().getStatus();
            };
            respostas.add(pool.submit(tentativa));
        }
        largada.countDown(); // solta todo mundo junto
        List<Integer> status = new ArrayList<>();
        for (Future<Integer> f : respostas) {
            status.add(f.get());
        }
        pool.shutdown();

        assertThat(status).containsOnly(201, 409);
        assertThat(status.stream().filter(s -> s == 201)).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamento", Integer.class)).isEqualTo(1);
    }

    @Test
    void foraDoExpedienteNoPassadoOuDiaDeFolgaNaoMarca() throws Exception {
        assertThat(agendar(TERCA + "T08:30:00").getResponse().getStatus()).isEqualTo(409); // antes de abrir
        assertThat(agendar(TERCA + "T17:45:00").getResponse().getStatus()).isEqualTo(409); // acabaria 18:15
        assertThat(agendar("2026-11-16T09:00:00").getResponse().getStatus()).isEqualTo(409); // segunda: folga
        assertThat(agendar("2026-11-10T10:00:00").getResponse().getStatus()).isEqualTo(409); // passado

        mvc.perform(get("/api/horarios").param("profissionalId", "1").param("servicoId", "1").param("data", "2026-11-22"))
                .andExpect(jsonPath("$.horarios.length()").value(0)); // domingo
    }

    @Test
    void antecedenciaMinimaDe30Minutos() throws Exception {
        relogio.ir(LocalDateTime.of(2026, 11, 17, 9, 50)); // terça, 9h50
        mvc.perform(get("/api/horarios").param("profissionalId", "1").param("servicoId", "1").param("data", TERCA))
                .andExpect(jsonPath("$.horarios[0]").value("10:30:00"));
        assertThat(agendar(TERCA + "T10:15:00").getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void camposInvalidosVoltamComAMensagemDeCadaUm() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"profissionalId\": 1, \"clienteNome\": \"\", \"clienteTelefone\": \"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.servicoId").value("Escolha o serviço"))
                .andExpect(jsonPath("$.campos.clienteNome").value("Informe seu nome"))
                .andExpect(jsonPath("$.campos.clienteTelefone").value("Telefone no formato (47) 99999-9999"));
    }

    @Test
    void cancelarLiberaOHorarioMasSoAteDuasHorasAntes() throws Exception {
        String codigo = JsonPath.read(agendar(TERCA + "T15:00:00").getResponse().getContentAsString(), "$.codigo");

        mvc.perform(post("/api/agendamentos/" + codigo + "/cancelar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(get("/api/horarios").param("profissionalId", "1").param("servicoId", "1").param("data", TERCA))
                .andExpect(jsonPath("$.horarios").value(hasItem("15:00:00")));
        mvc.perform(post("/api/agendamentos/" + codigo + "/cancelar")).andExpect(status().isConflict());

        // outro agendamento, e o cliente tenta cancelar 1h antes
        String outro = JsonPath.read(agendar(TERCA + "T16:00:00").getResponse().getContentAsString(), "$.codigo");
        relogio.ir(LocalDateTime.of(2026, 11, 17, 15, 0));
        mvc.perform(post("/api/agendamentos/" + outro + "/cancelar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cancelamento pelo site só até 2 horas antes. Fale com a barbearia."));
    }

    @Test
    void areaAdministrativaExigeLogin() throws Exception {
        mvc.perform(get("/api/admin/agenda").param("data", TERCA)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/agenda").param("data", TERCA).header("Authorization", "Bearer token.falso.aqui"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"dono@barbearia.test\", \"senha\": \"errada-errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void tokenVenceDepoisDeOitoHoras() throws Exception {
        String token = token();
        mvc.perform(get("/api/admin/agenda").param("data", TERCA).header("Authorization", token)).andExpect(status().isOk());
        relogio.avancar(Duration.ofHours(7).plusMinutes(50));
        mvc.perform(get("/api/admin/agenda").param("data", TERCA).header("Authorization", token)).andExpect(status().isOk());
        relogio.avancar(Duration.ofMinutes(15)); // 8h05 depois do login (a tolerância é de 30 s)
        mvc.perform(get("/api/admin/agenda").param("data", TERCA).header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void donoVeAAgendaDoDiaEMarcaComoConcluido() throws Exception {
        agendar(TERCA + "T09:00:00");
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON).content(marcar(TERCA + "T11:00:00", 2, 3)));
        String token = token();

        MvcResult agenda = mvc.perform(get("/api/admin/agenda").param("data", TERCA).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].servico").value("Corte e barba"))
                .andExpect(jsonPath("$[1].precoCentavos").value(7000))
                .andReturn();
        Integer id = JsonPath.read(agenda.getResponse().getContentAsString(), "$[0].id");

        mvc.perform(patch("/api/admin/agendamentos/" + id + "/status").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"CONCLUIDO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDO"));
    }

    @Test
    void corsLiberaSoOFrontConfigurado() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/servicos")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get("/api/servicos").header("Origin", "https://site-qualquer.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cadastroDeServicoValidaDuracaoENomeRepetido() throws Exception {
        String token = token();
        mvc.perform(post("/api/admin/servicos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"Pigmentação\", \"duracaoMinutos\": 45, \"precoCentavos\": 6000, \"ativo\": true}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/servicos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"corte\", \"duracaoMinutos\": 30, \"precoCentavos\": 1, \"ativo\": true}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/servicos").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\": \"Hidratação\", \"duracaoMinutos\": 20, \"precoCentavos\": 1, \"ativo\": true}"))
                .andExpect(status().isUnprocessableContent());
        jdbc.update("DELETE FROM servico WHERE nome = 'Pigmentação'");
    }
}
