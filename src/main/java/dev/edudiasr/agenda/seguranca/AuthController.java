package dev.edudiasr.agenda.seguranca;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.edudiasr.agenda.AgendaProperties;
import dev.edudiasr.agenda.dominio.Usuario;
import dev.edudiasr.agenda.dominio.UsuarioRepository;
import dev.edudiasr.agenda.erro.NegocioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Login")
public class AuthController {

    public record Login(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record Token(String token, Instant expiraEm, String nome) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtEncoder jwt;
    private final AgendaProperties props;
    private final Clock relogio;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder senhas, JwtEncoder jwt,
            AgendaProperties props, Clock relogio) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.jwt = jwt;
        this.props = props;
        this.relogio = relogio;
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar e receber o token da área administrativa")
    public Token login(@Valid @RequestBody Login login) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(login.email().strip()).orElse(null);
        // mesma resposta pra e-mail inexistente e senha errada: não revela quais e-mails têm conta
        if (usuario == null || !senhas.matches(login.senha(), usuario.getSenhaHash())) {
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }

        Instant agora = relogio.instant();
        Instant expira = agora.plus(props.tokenHoras(), ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getEmail())
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("nome", usuario.getNome())
                .build();
        String token = jwt.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new Token(token, expira, usuario.getNome());
    }
}
