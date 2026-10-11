package br.org.concentro.portal.acesso;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.org.concentro.portal.cadastro.AreaAtuacao;
import br.org.concentro.portal.cadastro.Cargo;
import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.cadastro.CongressistaRepository;
import br.org.concentro.portal.cadastro.Ej;
import br.org.concentro.portal.cadastro.EjRepository;
import br.org.concentro.portal.cadastro.TempoMej;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Cada teste cria uma conta dentro de uma transação desfeita no final.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EjRepository ejs;

    @Autowired
    private CongressistaRepository congressistas;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criaConta() {
        Ej ej = ejs.findAllByOrderByNomeAsc().get(0);
        congressistas.save(new Congressista("Pedro Teste", "pedro.teste@exemplo.com",
            passwordEncoder.encode("senhaSegura123"), ej, Cargo.DIRETOR,
            TempoMej.DE_1_A_2_ANOS, AreaAtuacao.PROJETOS));
    }

    private ResultActions login(String email, String senha) throws Exception {
        String corpo = """
            {"email": "%s", "senha": "%s"}
            """.formatted(email, senha);
        return mvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Test
    void loginCertoDevolveTokenQueAbreOPerfil() throws Exception {
        String resposta = login("Pedro.Teste@Exemplo.com", "senhaSegura123")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Pedro Teste"))
            .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(resposta, "$.token");

        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("pedro.teste@exemplo.com"))
            .andExpect(jsonPath("$.cargo").value("DIRETOR"))
            .andExpect(jsonPath("$.ejNome").isNotEmpty())
            .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    void senhaErradaDa401() throws Exception {
        login("pedro.teste@exemplo.com", "senhaErrada999").andExpect(status().isUnauthorized());
    }

    @Test
    void emailDesconhecidoDa401() throws Exception {
        login("ninguem@exemplo.com", "senhaSegura123").andExpect(status().isUnauthorized());
    }

    @Test
    void perfilSemTokenDa401() throws Exception {
        mvc.perform(get("/api/perfil")).andExpect(status().isUnauthorized());
    }

    @Test
    void perfilComTokenFalsoDa401() throws Exception {
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer token.falso.aqui"))
            .andExpect(status().isUnauthorized());
    }
}
