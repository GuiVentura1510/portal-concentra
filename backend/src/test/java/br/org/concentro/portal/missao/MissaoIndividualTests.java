package br.org.concentro.portal.missao;

import static org.hamcrest.Matchers.hasItem;
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
import br.org.concentro.portal.tripulacao.TripulacaoRepository;
import br.org.concentro.portal.tripulacao.TripulacaoService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

// Missões individuais: cada pessoa envia a sua, o staff aprova ou marca direto,
// e os pontos contam só no ranking individual, nunca no das tripulações.
@SpringBootTest(properties = {"app.staff.emails=staff.teste@exemplo.com", "app.ranking.cache=PT0S"})
@AutoConfigureMockMvc
@Transactional
class MissaoIndividualTests {

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
    private TripulacaoService tripulacaoService;

    @Autowired
    private MissaoRepository missoes;

    @Autowired
    private ConclusaoRepository conclusoes;

    @Autowired
    private LancamentoRepository lancamentos;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    // Mesmo ponto de partida do ConclusaoTests: 12 congressistas em 4 EJs, uma
    // conta do staff e as tripulações montadas.
    @BeforeEach
    void preparaJogo() {
        lancamentos.deleteAll();
        conclusoes.deleteAll();
        for (Congressista c : congressistas.comTripulacao()) {
            c.entrarNa(null);
        }
        em.flush();
        tripulacoes.deleteAll();
        em.flush();

        String hash = passwordEncoder.encode(SENHA);
        List<Ej> quatroEjs = ejs.findAllByOrderByNomeAsc().subList(0, 4);
        for (int i = 0; i < 12; i++) {
            congressistas.save(new Congressista("Pessoa " + i, "pessoa" + i + ".teste@exemplo.com", hash,
                quatroEjs.get(i % 4), Cargo.ASSESSOR_CONSULTOR,
                i % 2 == 0 ? TempoMej.MENOS_DE_6_MESES : TempoMej.MAIS_DE_2_ANOS, AreaAtuacao.PROJETOS));
        }
        congressistas.save(new Congressista("Staff Teste", "staff.teste@exemplo.com", hash, quatroEjs.get(0),
            Cargo.DIRETOR, TempoMej.MAIS_DE_2_ANOS, AreaAtuacao.PROJETOS));
        em.flush();
        tripulacaoService.montar();
        em.flush();
    }

    private Missao individual(FormaConclusao forma, Integer vagas) {
        Instant agora = Instant.now();
        return missoes.saveAndFlush(new Missao("Missão individual", "Cada um responde", 10, vagas,
            agora.minus(Duration.ofHours(1)), agora.plus(Duration.ofHours(1)), forma, true, AlcanceMissao.INDIVIDUAL));
    }

    private Congressista pessoa(int i) {
        return congressistas.findByEmail("pessoa" + i + ".teste@exemplo.com").orElseThrow();
    }

    private Congressista colegaDe(int i) {
        Long tripulacaoId = pessoa(i).getTripulacao().getId();
        return congressistas.membrosDa(tripulacaoId).stream()
            .filter(c -> c.getEmail().startsWith("pessoa") && !c.getEmail().equals(pessoa(i).getEmail()))
            .findFirst().orElseThrow();
    }

    private String token(String email) throws Exception {
        String corpo = """
            {"email": "%s", "senha": "%s"}
            """.formatted(email, SENHA);
        String resposta = mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private ResultActions enviar(String token, Long missaoId, String texto) throws Exception {
        return mvc.perform(post("/api/missoes/" + missaoId + "/resposta").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"" + texto + "\"}"));
    }

    private ResultActions marcar(String staff, Long missaoId, String corpo) throws Exception {
        return mvc.perform(post("/api/staff/missoes/" + missaoId + "/marcar").header("Authorization", staff)
            .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void cadaMembroEnviaASuaEPontuaSoNoRankingIndividual() throws Exception {
        Missao m = individual(FormaConclusao.ENVIO_CAPITAO, null);
        Congressista eu = pessoa(0);
        Congressista colega = colegaDe(0);
        Long tripulacaoId = eu.getTripulacao().getId();
        String meuToken = token(eu.getEmail());

        enviar(meuToken, m.getId(), "Minha resposta").andExpect(status().isCreated());
        // A mesma pessoa não envia duas vezes.
        enviar(meuToken, m.getId(), "De novo").andExpect(status().isConflict());

        // O colega da mesma tripulação ainda pode enviar a dele.
        String tokenColega = token(colega.getEmail());
        mvc.perform(get("/api/missoes").header("Authorization", tokenColega))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].alcance").value(hasItem("INDIVIDUAL")))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].possoEnviar").value(hasItem(true)));
        enviar(tokenColega, m.getId(), "Resposta do colega").andExpect(status().isCreated());

        String staff = token("staff.teste@exemplo.com");
        String fila = mvc.perform(get("/api/staff/conclusoes").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.missaoId == " + m.getId() + ")].congressistaNome").value(hasItem("Pessoa 0")))
            .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(fila, "$[?(@.missaoId == " + m.getId() + ")].id");
        for (Integer id : ids) {
            mvc.perform(post("/api/staff/conclusoes/" + id + "/aprovar").header("Authorization", staff))
                .andExpect(status().isOk());
        }

        // A tripulação não ganha nada com as missões individuais.
        mvc.perform(get("/api/ranking").header("Authorization", meuToken))
            .andExpect(jsonPath("$[?(@.tripulacaoId == " + tripulacaoId + ")].pontos").value(hasItem(0)));

        // No ranking individual, as duas pessoas têm 10 pontos e dividem o 1º lugar.
        mvc.perform(get("/api/ranking/individual").header("Authorization", meuToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.posicoes[0].posicao").value(1))
            .andExpect(jsonPath("$.posicoes[1].posicao").value(1))
            .andExpect(jsonPath("$.minha.congressistaId").value(eu.getId()))
            .andExpect(jsonPath("$.minha.pontos").value(10))
            .andExpect(jsonPath("$.minha.missoesCumpridas").value(1));
    }

    // Missão da tripulação conta só no ranking das tripulações; a individual, só no individual.
    @Test
    void rankingsSeparados() throws Exception {
        Instant agora = Instant.now();
        Missao daTripulacao = missoes.saveAndFlush(new Missao("Da tripulação", "", 50, null,
            agora.minus(Duration.ofHours(1)), agora.plus(Duration.ofHours(1)), FormaConclusao.STAFF_MARCA, true));
        Missao individual = individual(FormaConclusao.STAFF_MARCA, null);
        Congressista eu = pessoa(6);
        Long tripulacaoId = eu.getTripulacao().getId();
        String staff = token("staff.teste@exemplo.com");

        marcar(staff, daTripulacao.getId(), "{\"tripulacaoId\": " + tripulacaoId + "}").andExpect(status().isOk());
        marcar(staff, individual.getId(), "{\"congressistaId\": " + eu.getId() + "}").andExpect(status().isOk());

        String meuToken = token(eu.getEmail());
        mvc.perform(get("/api/ranking").header("Authorization", meuToken))
            .andExpect(jsonPath("$[0].tripulacaoId").value(tripulacaoId))
            .andExpect(jsonPath("$[0].pontos").value(50))
            .andExpect(jsonPath("$[0].missoesCumpridas").value(1));
        mvc.perform(get("/api/ranking/individual").header("Authorization", meuToken))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.minha.pontos").value(10))
            .andExpect(jsonPath("$.minha.posicao").value(1));

        // Quem ainda não pontuou vê o ranking, mas sem posição própria.
        mvc.perform(get("/api/ranking/individual").header("Authorization", token(pessoa(7).getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.minha").doesNotExist());
    }

    @Test
    void comCapitaoDefinidoOutroMembroEnviaAIndividual() throws Exception {
        Missao m = individual(FormaConclusao.ENVIO_CAPITAO, null);
        tripulacaoService.definirCapitao(pessoa(0).getTripulacao().getId(), pessoa(0).getId());
        enviar(token(colegaDe(0).getEmail()), m.getId(), "Minha").andExpect(status().isCreated());
    }

    @Test
    void staffMarcaUmaPessoa() throws Exception {
        Missao m = individual(FormaConclusao.STAFF_MARCA, null);
        Long pessoaId = pessoa(1).getId();
        String staff = token("staff.teste@exemplo.com");

        marcar(staff, m.getId(), "{\"congressistaId\": " + pessoaId + "}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alcance").value("INDIVIDUAL"))
            .andExpect(jsonPath("$.congressistaNome").value("Pessoa 1"))
            .andExpect(jsonPath("$.status").value("APROVADA"));
        // De novo: já cumprida.
        marcar(staff, m.getId(), "{\"congressistaId\": " + pessoaId + "}").andExpect(status().isConflict());
        // Missão individual pede a pessoa, não a tripulação.
        marcar(staff, m.getId(), "{\"tripulacaoId\": " + pessoa(2).getTripulacao().getId() + "}")
            .andExpect(status().isBadRequest());
        // Conta do staff não participa.
        Long staffId = congressistas.findByEmail("staff.teste@exemplo.com").orElseThrow().getId();
        marcar(staff, m.getId(), "{\"congressistaId\": " + staffId + "}").andExpect(status().isBadRequest());

        mvc.perform(get("/api/missoes").header("Authorization", token(pessoa(1).getEmail())))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].minhaConclusao.status").value(hasItem("APROVADA")));
    }

    @Test
    void vagasContamPessoas() throws Exception {
        Missao m = individual(FormaConclusao.STAFF_MARCA, 1);
        String staff = token("staff.teste@exemplo.com");
        marcar(staff, m.getId(), "{\"congressistaId\": " + pessoa(3).getId() + "}").andExpect(status().isOk());
        marcar(staff, m.getId(), "{\"congressistaId\": " + pessoa(4).getId() + "}").andExpect(status().isConflict());
    }

    @Test
    void staffNaoEnviaResposta() throws Exception {
        Missao m = individual(FormaConclusao.ENVIO_CAPITAO, null);
        enviar(token("staff.teste@exemplo.com"), m.getId(), "Teste").andExpect(status().isForbidden());
    }

    @Test
    void missaoDaTripulacaoContinuaPedindoATripulacao() throws Exception {
        Instant agora = Instant.now();
        Missao m = missoes.saveAndFlush(new Missao("Da tripulação", "", 50, null, agora.minus(Duration.ofHours(1)),
            agora.plus(Duration.ofHours(1)), FormaConclusao.STAFF_MARCA, true));
        marcar(token("staff.teste@exemplo.com"), m.getId(), "{\"congressistaId\": " + pessoa(5).getId() + "}")
            .andExpect(status().isBadRequest());
    }
}
