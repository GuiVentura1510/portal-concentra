package br.org.concentro.portal.missao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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

// O jogo pela API: envio do capitão, fila do staff, marcação direta, vagas e ranking.
// O ranking sem cache (PT0S) deixa o teste ver o resultado na hora.
@SpringBootTest(properties = {"app.staff.emails=staff.teste@exemplo.com", "app.ranking.cache=PT0S"})
@AutoConfigureMockMvc
@Transactional
class ConclusaoTests {

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

    // Começa do zero (sem missões concluídas nem tripulações), cria 12 congressistas
    // em 4 EJs e uma conta do staff, e monta as tripulações.
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

    private Missao missao(FormaConclusao forma, Integer vagas, Instant inicio, Instant fim, boolean visivel) {
        return missoes.saveAndFlush(new Missao("Missão de teste", "Descrição", 100, vagas, inicio, fim, forma, visivel));
    }

    private Missao aberta(FormaConclusao forma, Integer vagas) {
        Instant agora = Instant.now();
        return missao(forma, vagas, agora.minus(Duration.ofHours(1)), agora.plus(Duration.ofHours(1)), true);
    }

    private Congressista pessoa(int i) {
        return congressistas.findByEmail("pessoa" + i + ".teste@exemplo.com").orElseThrow();
    }

    // Outra pessoa de teste da mesma tripulação da pessoa i.
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

    private ResultActions marcar(String staff, Long missaoId, Long tripulacaoId) throws Exception {
        return mvc.perform(post("/api/staff/missoes/" + missaoId + "/marcar").header("Authorization", staff)
            .contentType(MediaType.APPLICATION_JSON).content("{\"tripulacaoId\": " + tripulacaoId + "}"));
    }

    @Test
    void capitaoEnviaStaffAprovaEATripulacaoPontua() throws Exception {
        Missao m = aberta(FormaConclusao.ENVIO_CAPITAO, null);
        Congressista capitao = pessoa(0);
        Long tripulacaoId = capitao.getTripulacao().getId();
        tripulacaoService.definirCapitao(tripulacaoId, capitao.getId());
        String tokenCapitao = token(capitao.getEmail());

        enviar(tokenCapitao, m.getId(), "Nossas duas dores: vendas e liderança").andExpect(status().isCreated());

        mvc.perform(get("/api/missoes").header("Authorization", tokenCapitao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].minhaConclusao.status").value(hasItem("PENDENTE")))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].possoEnviar").value(hasItem(false)));

        String staff = token("staff.teste@exemplo.com");
        String fila = mvc.perform(get("/api/staff/conclusoes").header("Authorization", staff))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(fila, "$[?(@.missaoId == " + m.getId() + ")].id");

        mvc.perform(post("/api/staff/conclusoes/" + ids.get(0) + "/aprovar").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APROVADA"));

        mvc.perform(get("/api/ranking").header("Authorization", tokenCapitao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tripulacaoId").value(tripulacaoId))
            .andExpect(jsonPath("$[0].pontos").value(100))
            .andExpect(jsonPath("$[0].posicao").value(1));
    }

    @Test
    void comCapitaoDefinidoOutroMembroNaoEnvia() throws Exception {
        Missao m = aberta(FormaConclusao.ENVIO_CAPITAO, null);
        tripulacaoService.definirCapitao(pessoa(0).getTripulacao().getId(), pessoa(0).getId());
        enviar(token(colegaDe(0).getEmail()), m.getId(), "Tentativa").andExpect(status().isForbidden());
    }

    @Test
    void semCapitaoQualquerMembroEnviaMasSoUmaVez() throws Exception {
        Missao m = aberta(FormaConclusao.ENVIO_CAPITAO, null);
        enviar(token(pessoa(0).getEmail()), m.getId(), "Primeira").andExpect(status().isCreated());
        enviar(token(colegaDe(0).getEmail()), m.getId(), "Segunda").andExpect(status().isConflict());
    }

    @Test
    void recusadaPermiteReenviar() throws Exception {
        Missao m = aberta(FormaConclusao.ENVIO_CAPITAO, null);
        String membro = token(pessoa(1).getEmail());
        enviar(membro, m.getId(), "Incompleta").andExpect(status().isCreated());

        String staff = token("staff.teste@exemplo.com");
        Long conclusaoId = conclusoes.daTripulacao(pessoa(1).getTripulacao().getId()).get(0).getId();
        mvc.perform(post("/api/staff/conclusoes/" + conclusaoId + "/recusar").header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"Faltou a matriz\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECUSADA"));

        mvc.perform(get("/api/missoes").header("Authorization", membro))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].minhaConclusao.motivoRecusa")
                .value(hasItem("Faltou a matriz")))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].possoEnviar").value(hasItem(true)));

        enviar(membro, m.getId(), "Completa agora").andExpect(status().isCreated());
    }

    @Test
    void foraDaJanelaNaoAceitaEnvio() throws Exception {
        Instant agora = Instant.now();
        Missao futura = missao(FormaConclusao.ENVIO_CAPITAO, null, agora.plus(Duration.ofHours(1)),
            agora.plus(Duration.ofHours(2)), true);
        Missao passada = missao(FormaConclusao.ENVIO_CAPITAO, null, agora.minus(Duration.ofHours(2)),
            agora.minus(Duration.ofHours(1)), true);
        String membro = token(pessoa(2).getEmail());
        enviar(membro, futura.getId(), "Cedo").andExpect(status().isConflict());
        enviar(membro, passada.getId(), "Tarde").andExpect(status().isConflict());
    }

    @Test
    void missaoOcultaNaoAparece() throws Exception {
        Missao oculta = aberta(FormaConclusao.ENVIO_CAPITAO, null);
        oculta.atualizar("Oculta", "", 10, null, null, null, FormaConclusao.ENVIO_CAPITAO, false);
        em.flush();
        mvc.perform(get("/api/missoes").header("Authorization", token(pessoa(3).getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id").value(not(hasItem(oculta.getId().intValue()))));
    }

    @Test
    void staffMarcaDiretoEVagasAcabam() throws Exception {
        Missao m = aberta(FormaConclusao.STAFF_MARCA, 1);
        Long tripulacaoA = pessoa(0).getTripulacao().getId();
        Long tripulacaoB = tripulacoes.findAll().stream().map(t -> t.getId())
            .filter(id -> !id.equals(tripulacaoA)).findFirst().orElseThrow();
        String staff = token("staff.teste@exemplo.com");

        marcar(staff, m.getId(), tripulacaoA).andExpect(status().isOk());
        // De novo na mesma tripulação: já cumprida.
        marcar(staff, m.getId(), tripulacaoA).andExpect(status().isConflict());
        // Outra tripulação: a única vaga já foi ocupada.
        marcar(staff, m.getId(), tripulacaoB).andExpect(status().isConflict());

        mvc.perform(get("/api/missoes").header("Authorization", token(pessoa(0).getEmail())))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].situacao").value(hasItem("ABERTA")))
            .andExpect(jsonPath("$[?(@.id == " + m.getId() + ")].vagasRestantes").value(hasItem(0)));
    }

    @Test
    void congressistaNaoAcessaAFila() throws Exception {
        mvc.perform(get("/api/staff/conclusoes").header("Authorization", token(pessoa(0).getEmail())))
            .andExpect(status().isForbidden());
    }
}
