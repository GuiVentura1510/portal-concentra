package br.org.concentro.portal.tripulacao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import jakarta.persistence.EntityManager;
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

// Diário de Bordo pela API: qualquer integrante anexa, só a própria tripulação vê,
// quem anexou ou o staff apaga, e cada tripulação tem um limite de fotos.
@SpringBootTest(properties = "app.staff.emails=staff.teste@exemplo.com")
@AutoConfigureMockMvc
@Transactional
class DiarioTests {

    private static final String SENHA = "senhaSegura123";
    // O menor "JPEG" que a API aceita: só confere os bytes iniciais FF D8 FF.
    private static final String JPEG = "/9j/4AAQSkZJRgABAQ==";

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    // Começa sem tripulações, cria 12 congressistas em 4 EJs e uma conta do staff,
    // e monta as tripulações.
    @BeforeEach
    void preparaTripulacoes() {
        em.createQuery("delete from Lancamento").executeUpdate();
        em.createQuery("delete from Conclusao").executeUpdate();
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
                quatroEjs.get(i % 4), Cargo.ASSESSOR_CONSULTOR, TempoMej.MENOS_DE_6_MESES, AreaAtuacao.PROJETOS));
        }
        congressistas.save(new Congressista("Staff Teste", "staff.teste@exemplo.com", hash, quatroEjs.get(0),
            Cargo.DIRETOR, TempoMej.MAIS_DE_2_ANOS, AreaAtuacao.PROJETOS));
        em.flush();
        tripulacaoService.montar();
        em.flush();
    }

    private Congressista pessoa(int i) {
        return congressistas.findByEmail("pessoa" + i + ".teste@exemplo.com").orElseThrow();
    }

    // Uma pessoa de teste da mesma tripulação (ou de outra) que a pessoa 0.
    private Congressista daTripulacaoDaPessoa0(boolean mesma) {
        Long tripulacaoId = pessoa(0).getTripulacao().getId();
        for (int i = 1; i < 12; i++) {
            if (pessoa(i).getTripulacao().getId().equals(tripulacaoId) == mesma) {
                return pessoa(i);
            }
        }
        throw new IllegalStateException("Montagem sem a pessoa procurada.");
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

    private ResultActions anexar(String token, String imagem) throws Exception {
        return mvc.perform(post("/api/tripulacao/diario").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"imagem\": \"" + imagem + "\"}"));
    }

    // Anexa uma foto e devolve [id, chave].
    private Object[] anexarFoto(String token) throws Exception {
        String resposta = anexar(token, JPEG).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return new Object[] {((Number) JsonPath.read(resposta, "$.id")).longValue(), JsonPath.read(resposta, "$.chave")};
    }

    @Test
    void integranteAnexaEATripulacaoVe() throws Exception {
        String autora = token(pessoa(0).getEmail());
        String chave = (String) anexarFoto(autora)[1];

        // Qualquer integrante vê a foto, com o nome de quem anexou e o limite.
        mvc.perform(get("/api/tripulacao/diario").header("Authorization", token(daTripulacaoDaPessoa0(true).getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.limite").value(DiarioService.LIMITE_POR_TRIPULACAO))
            .andExpect(jsonPath("$.fotos[*].chave").value(hasItem(chave)))
            .andExpect(jsonPath("$.fotos[0].autorNome").value("Pessoa 0"));
        // A imagem abre sem token, pela chave.
        mvc.perform(get("/api/diario/" + chave)).andExpect(status().isOk());

        // Outra tripulação não vê.
        mvc.perform(get("/api/tripulacao/diario").header("Authorization", token(daTripulacaoDaPessoa0(false).getEmail())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fotos[*].chave").value(not(hasItem(chave))));
    }

    @Test
    void legendaEscritaPorQuemAnexou() throws Exception {
        String autora = token(pessoa(0).getEmail());
        anexarComLegenda(autora, "  Primeiro dia!  ")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.legenda").value("Primeiro dia!"));
        // Sem legenda (ou só com espaços): fica vazia.
        anexarComLegenda(autora, "   ").andExpect(status().isCreated()).andExpect(jsonPath("$.legenda").value(nullValue()));
        // Mais de 30 caracteres não cabem na polaroide.
        anexarComLegenda(autora, "x".repeat(31)).andExpect(status().isBadRequest());

        mvc.perform(get("/api/tripulacao/diario").header("Authorization", token(daTripulacaoDaPessoa0(true).getEmail())))
            .andExpect(jsonPath("$.fotos[0].legenda").value("Primeiro dia!"));
    }

    private ResultActions anexarComLegenda(String token, String legenda) throws Exception {
        return mvc.perform(post("/api/tripulacao/diario").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"imagem\": \"" + JPEG + "\", \"legenda\": \"" + legenda + "\"}"));
    }

    @Test
    void soQuemAnexouApaga() throws Exception {
        String autora = token(pessoa(0).getEmail());
        Object[] foto = anexarFoto(autora);

        mvc.perform(delete("/api/tripulacao/diario/" + foto[0])
                .header("Authorization", token(daTripulacaoDaPessoa0(true).getEmail())))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/tripulacao/diario/" + foto[0]).header("Authorization", autora))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/tripulacao/diario").header("Authorization", autora))
            .andExpect(jsonPath("$.fotos", hasSize(0)));
        mvc.perform(get("/api/diario/" + foto[1])).andExpect(status().isNotFound());
    }

    @Test
    void staffVeTodasEApaga() throws Exception {
        Object[] foto = anexarFoto(token(pessoa(0).getEmail()));
        String staff = token("staff.teste@exemplo.com");

        mvc.perform(get("/api/staff/diario").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].chave").value(hasItem(foto[1])))
            .andExpect(jsonPath("$[0].tripulacaoNome").value(pessoa(0).getTripulacao().getNome()));
        mvc.perform(delete("/api/staff/diario/" + foto[0]).header("Authorization", staff))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/diario/" + foto[1])).andExpect(status().isNotFound());

        // Congressista não usa as rotas do staff.
        mvc.perform(get("/api/staff/diario").header("Authorization", token(pessoa(0).getEmail())))
            .andExpect(status().isForbidden());
    }

    @Test
    void cadaTripulacaoTemUmLimiteDeFotos() throws Exception {
        String autora = token(pessoa(0).getEmail());
        for (int i = 0; i < DiarioService.LIMITE_POR_TRIPULACAO; i++) {
            anexar(autora, JPEG).andExpect(status().isCreated());
        }
        anexar(token(daTripulacaoDaPessoa0(true).getEmail()), JPEG).andExpect(status().isConflict());
        // O limite é por tripulação: a outra continua anexando.
        anexar(token(daTripulacaoDaPessoa0(false).getEmail()), JPEG).andExpect(status().isCreated());
    }

    @Test
    void semTripulacaoOuSemJpegNaoAnexa() throws Exception {
        // O staff não entra em tripulação.
        anexar(token("staff.teste@exemplo.com"), JPEG).andExpect(status().isNotFound());
        // "aGVsbG8=" é "hello": não é JPEG.
        anexar(token(pessoa(0).getEmail()), "aGVsbG8=").andExpect(status().isBadRequest());
    }

    @Test
    void desfazerAMontagemApagaODiario() throws Exception {
        String chave = (String) anexarFoto(token(pessoa(0).getEmail()))[1];
        // Quem apaga as fotos é o banco (on delete cascade), não o JPA.
        em.flush();
        em.clear();
        for (Congressista c : congressistas.comTripulacao()) {
            c.entrarNa(null);
        }
        em.flush();
        tripulacoes.deleteAll();
        em.flush();
        mvc.perform(get("/api/diario/" + chave)).andExpect(status().isNotFound());
    }
}
