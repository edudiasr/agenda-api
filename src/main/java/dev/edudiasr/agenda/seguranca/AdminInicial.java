package dev.edudiasr.agenda.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import dev.edudiasr.agenda.AgendaProperties;
import dev.edudiasr.agenda.dominio.Usuario;
import dev.edudiasr.agenda.dominio.UsuarioRepository;

/**
 * Cria o primeiro usuário a partir de ADMIN_EMAIL e ADMIN_SENHA, só se ainda não existir ninguém.
 * Assim a senha não fica escrita em nenhuma migração nem no repositório.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final AgendaProperties props;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder senhas, AgendaProperties props) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = props.adminEmail();
        String senha = props.adminSenha();
        if (email == null || email.isBlank() || senha == null || senha.isBlank() || usuarios.count() > 0) {
            return;
        }
        if (senha.length() < 10) {
            throw new IllegalStateException("ADMIN_SENHA precisa ter pelo menos 10 caracteres");
        }
        usuarios.save(new Usuario(email.strip().toLowerCase(), "Administrador", senhas.encode(senha)));
        log.info("Usuário administrador {} criado", email);
    }
}
