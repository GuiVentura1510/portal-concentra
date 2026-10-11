package br.org.concentro.portal.cadastro;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.org.concentro.portal.missao.Conclusao;
import br.org.concentro.portal.missao.ConclusaoRepository;
import br.org.concentro.portal.missao.FormaConclusao;
import br.org.concentro.portal.missao.Missao;
import br.org.concentro.portal.missao.MissaoRepository;
import br.org.concentro.portal.tripulacao.Tripulacao;
import br.org.concentro.portal.tripulacao.TripulacaoRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
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

// Aba Pessoas do painel do staff: busca, senha esquecida, remoção e cruzamento.
@SpringBootTest(properties = "app.staff.emails=staff.teste@exemplo.com")
@AutoConfigureMockMvc
@Transactional
class StaffPessoasTests {

    private static final String SENHA = "senhaSegura123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EjRepository ejs;

    @Autowired
    private CongressistaRepository congressistas;

    @Autowired
    private TripulacaoRepository tripulacoes;

    @Autowired
    private MissaoRepository missoes;

    @Autowired
    private ConclusaoRepository conclusoes;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criaContas() {
        String hash = passwordEncoder.encode(SENHA);
        Ej ej = ejs.findAllByOrderByNomeAsc().get(0);
        congressistas.save(new Congressista("Staff Teste", "staff.teste@exemplo.com", hash, ej, Cargo.DIRETOR,
            TempoMej.MAIS_DE_2_ANOS, AreaAtuacao.PROJETOS));
        congressistas.save(new Congressista("Zuleica Testadora", "zuleica.teste@exemplo.com", hash, ej,
            Cargo.PRESIDENTE, TempoMej.DE_1_A_2_ANOS, AreaAtuacao.PRESIDENCIA));
        congressistas.save(new Congressista("Joaquim Testador", "joaquim.teste@exemplo.com", hash, ej,
            Cargo.ASSESSOR_CONSULTOR, TempoMej.MENOS_DE_6_MESES, AreaAtuacao.MARKETING));
    }

    private String token(String email, String senha) throws Exception {
        String resposta = login(email, senha).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private ResultActions login(String email, String senha) throws Exception {
        String corpo = """
            {"email": "%s", "senha": "%s"}
            """.formatted(email, senha);
        return mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private Long id(String email) {
        return congressistas.findByEmail(email).orElseThrow().getId();
    }

    @Test
    void buscaPorParteDoNomeOuEmail() throws Exception {
        String staff = token("staff.teste@exemplo.com", SENHA);
        mvc.perform(get("/api/staff/congressistas").param("busca", "ZULEI").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].email").value(hasItem("zuleica.teste@exemplo.com")));
        mvc.perform(get("/api/staff/congressistas").param("busca", "joaquim.teste").header("Authorization", staff))
            .andExpect(jsonPath("$[*].nome").value(hasItem("Joaquim Testador")));
    }

    @Test
    void senhaTemporariaSubstituiAAntiga() throws Exception {
        String staff = token("staff.teste@exemplo.com", SENHA);
        String resposta = mvc.perform(post("/api/staff/congressistas/" + id("zuleica.teste@exemplo.com") + "/senha")
                .header("Authorization", staff))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String nova = JsonPath.read(resposta, "$.senha");

        login("zuleica.teste@exemplo.com", nova).andExpect(status().isOk());
        login("zuleica.teste@exemplo.com", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void removeCadastro() throws Exception {
        String staff = token("staff.teste@exemplo.com", SENHA);
        mvc.perform(delete("/api/staff/congressistas/" + id("joaquim.teste@exemplo.com")).header("Authorization", staff))
            .andExpect(status().isNoContent());
        login("joaquim.teste@exemplo.com", SENHA).andExpect(status().isUnauthorized());
    }

    @Test
    void staffNaoRemoveAPropriaConta() throws Exception {
        String staff = token("staff.teste@exemplo.com", SENHA);
        mvc.perform(delete("/api/staff/congressistas/" + id("staff.teste@exemplo.com")).header("Authorization", staff))
            .andExpect(status().isBadRequest());
    }

    @Test
    void cruzamentoApontaQuemNaoEstaNaListaSemContarOStaff() throws Exception {
        String staff = token("staff.teste@exemplo.com", SENHA);
        mvc.perform(post("/api/staff/congressistas/cruzamento").header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emails\": [\"  ZULEICA.teste@exemplo.com \"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].email").value(hasItem("joaquim.teste@exemplo.com")))
            .andExpect(jsonPath("$[*].email").value(not(hasItem("zuleica.teste@exemplo.com"))))
            .andExpect(jsonPath("$[*].email").value(not(hasItem("staff.teste@exemplo.com"))));
    }

    @Test
    void congressistaNaoAcessaAAbaPessoas() throws Exception {
        String congressista = token("joaquim.teste@exemplo.com", SENHA);
        mvc.perform(get("/api/staff/congressistas").param("busca", "zu").header("Authorization", congressista))
            .andExpect(status().isForbidden());
    }

    // Quem já enviou resposta de missão fica no histórico e não pode ser removido.
    @Test
    void naoRemoveQuemJaParticipouDeMissao() throws Exception {
        Congressista zuleica = congressistas.findByEmail("zuleica.teste@exemplo.com").orElseThrow();
        Tripulacao tripulacao = tripulacoes.save(new Tripulacao("Tripulação de teste da remoção"));
        Missao missao = missoes.save(new Missao("Missão de teste", "", 10, null, null, null,
            FormaConclusao.ENVIO_CAPITAO, true));
        conclusoes.saveAndFlush(Conclusao.enviada(missao, tripulacao, zuleica, "Resposta", Instant.now()));

        String staff = token("staff.teste@exemplo.com", SENHA);
        mvc.perform(delete("/api/staff/congressistas/" + zuleica.getId()).header("Authorization", staff))
            .andExpect(status().isConflict());
    }
}
