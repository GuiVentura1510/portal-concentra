package br.org.concentro.portal.missao;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

// Cadastro de missões pelo staff. A conta staff.teste@exemplo.com é do staff
// só neste teste. Tudo roda numa transação desfeita no final.
@SpringBootTest(properties = "app.staff.emails=staff.teste@exemplo.com")
@AutoConfigureMockMvc
@Transactional
class MissaoTests {

    private static final String SENHA = "senhaSegura123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EjRepository ejs;

    @Autowired
    private CongressistaRepository congressistas;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criaContas() {
        String hash = passwordEncoder.encode(SENHA);
        Ej ej = ejs.findAllByOrderByNomeAsc().get(0);
        congressistas.save(new Congressista("Staff Teste", "staff.teste@exemplo.com", hash, ej, Cargo.DIRETOR,
            TempoMej.MAIS_DE_2_ANOS, AreaAtuacao.PROJETOS));
        congressistas.save(new Congressista("Congressista Teste", "congressista.teste@exemplo.com", hash, ej,
            Cargo.ASSESSOR_CONSULTOR, TempoMej.MENOS_DE_6_MESES, AreaAtuacao.MARKETING));
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

    // Missão 1 do documento, das 10h30 às 11h20 de Brasília.
    private static final String MAPA_DAS_DORES = """
        {"titulo": "O Mapa das Dores", "descricao": "Consenso sobre 2 dores comuns.", "pontos": 100,
         "vagas": null, "inicio": "2026-10-17T10:30:00-03:00", "fim": "2026-10-17T11:20:00-03:00",
         "conclusao": "STAFF_MARCA", "visivel": true}
        """;

    private ResultActions criar(String token, String corpo) throws Exception {
        return mvc.perform(post("/api/staff/missoes").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void staffCriaMissaoEHorarioFicaEmUtc() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        criar(staff, MAPA_DAS_DORES)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.inicio").value("2026-10-17T13:30:00Z"))
            .andExpect(jsonPath("$.fim").value("2026-10-17T14:20:00Z"))
            .andExpect(jsonPath("$.conclusao").value("STAFF_MARCA"))
            .andExpect(jsonPath("$.alcance").value("TRIPULACAO"));

        mvc.perform(get("/api/staff/missoes").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].titulo", hasItem("O Mapa das Dores")));
    }

    @Test
    void staffEditaMissao() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        String criada = criar(staff, MAPA_DAS_DORES).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(criada, "$.id")).longValue();

        String editada = MAPA_DAS_DORES.replace("\"pontos\": 100", "\"pontos\": 150")
            .replace("\"vagas\": null", "\"vagas\": 10");
        mvc.perform(put("/api/staff/missoes/" + id).header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content(editada))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pontos").value(150))
            .andExpect(jsonPath("$.vagas").value(10));
    }

    @Test
    void recusaFimAntesDoInicio() throws Exception {
        String invertida = MAPA_DAS_DORES.replace("T11:20:00", "T09:00:00");
        criar(token("staff.teste@exemplo.com"), invertida).andExpect(status().isBadRequest());
    }

    @Test
    void recusaPontosNegativosESemTitulo() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        criar(staff, MAPA_DAS_DORES.replace("\"pontos\": 100", "\"pontos\": -5")).andExpect(status().isBadRequest());
        criar(staff, MAPA_DAS_DORES.replace("O Mapa das Dores", "  ")).andExpect(status().isBadRequest());
    }

    @Test
    void congressistaNaoMexeEmMissoes() throws Exception {
        String congressista = token("congressista.teste@exemplo.com");
        criar(congressista, MAPA_DAS_DORES).andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/missoes").header("Authorization", congressista))
            .andExpect(status().isForbidden());
    }

    private long idCriado(String staff, String corpo) throws Exception {
        String criada = criar(staff, corpo).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(criada, "$.id")).longValue();
    }

    // A conta de congressista deste teste cumpre a missão individual, marcada pelo staff.
    private void marcarCongressista(String staff, long missaoId) throws Exception {
        Long pessoaId = congressistas.findByEmail("congressista.teste@exemplo.com").orElseThrow().getId();
        mvc.perform(post("/api/staff/missoes/" + missaoId + "/marcar").header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content("{\"congressistaId\": " + pessoaId + "}"))
            .andExpect(status().isOk());
    }

    private static final String INDIVIDUAL = MAPA_DAS_DORES.replace("\"visivel\": true",
        "\"visivel\": true, \"alcance\": \"INDIVIDUAL\"");

    @Test
    void excluiMissaoSemRespostas() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        long id = idCriado(staff, MAPA_DAS_DORES);
        mvc.perform(delete("/api/staff/missoes/" + id).header("Authorization", staff))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/staff/missoes").header("Authorization", staff))
            .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        mvc.perform(delete("/api/staff/missoes/" + id).header("Authorization", staff))
            .andExpect(status().isNotFound());
    }

    @Test
    void naoExcluiMissaoComRespostas() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        long id = idCriado(staff, INDIVIDUAL);
        marcarCongressista(staff, id);
        mvc.perform(delete("/api/staff/missoes/" + id).header("Authorization", staff))
            .andExpect(status().isConflict());
    }

    @Test
    void criaMissaoComFotoEMantemAComprovacaoAoEditar() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        String comFoto = MAPA_DAS_DORES.replace("\"visivel\": true", "\"visivel\": true, \"comprovacao\": \"FOTO\"");
        long id = idCriado(staff, comFoto);
        // Sem o campo na edição, a comprovação continua a mesma.
        mvc.perform(put("/api/staff/missoes/" + id).header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content(MAPA_DAS_DORES))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comprovacao").value("FOTO"));
        // Missão sem o campo pede texto.
        criar(staff, MAPA_DAS_DORES).andExpect(jsonPath("$.comprovacao").value("TEXTO"));
    }

    @Test
    void staffEscolheOMaximoDeFotos() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        String tres = MAPA_DAS_DORES.replace("\"visivel\": true",
            "\"visivel\": true, \"comprovacao\": \"FOTO\", \"maxFotos\": 3");
        long id = idCriado(staff, tres);
        // Sem o campo na edição, o máximo continua o mesmo.
        mvc.perform(put("/api/staff/missoes/" + id).header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content(MAPA_DAS_DORES))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.maxFotos").value(3));
        // Missão sem o campo aceita 1 foto; acima de 5, recusa.
        criar(staff, MAPA_DAS_DORES).andExpect(jsonPath("$.maxFotos").value(1));
        criar(staff, MAPA_DAS_DORES.replace("\"visivel\": true", "\"visivel\": true, \"maxFotos\": 6"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void excluiMissaoDepoisDeRetirarOsEnvios() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        long id = idCriado(staff, INDIVIDUAL);
        marcarCongressista(staff, id);
        String envios = mvc.perform(get("/api/staff/conclusoes").header("Authorization", staff))
            .andReturn().getResponse().getContentAsString();
        Number conclusaoId = JsonPath.<java.util.List<Number>>read(envios, "$[?(@.missaoId == " + id + ")].id").get(0);
        mvc.perform(post("/api/staff/conclusoes/" + conclusaoId + "/retirar").header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\": \"\"}"))
            .andExpect(status().isOk());

        mvc.perform(delete("/api/staff/missoes/" + id).header("Authorization", staff))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/staff/missoes").header("Authorization", staff))
            .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
    }

    @Test
    void naoTrocaAlcanceDepoisDeRespostas() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        long id = idCriado(staff, INDIVIDUAL);
        marcarCongressista(staff, id);
        String daTripulacao = INDIVIDUAL.replace("\"INDIVIDUAL\"", "\"TRIPULACAO\"");
        mvc.perform(put("/api/staff/missoes/" + id).header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content(daTripulacao))
            .andExpect(status().isConflict());
        // Mudar outros campos continua permitido.
        mvc.perform(put("/api/staff/missoes/" + id).header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content(INDIVIDUAL.replace("\"pontos\": 100", "\"pontos\": 120")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pontos").value(120))
            .andExpect(jsonPath("$.alcance").value("INDIVIDUAL"));
    }

    @Test
    void congressistaNaoExcluiMissao() throws Exception {
        long id = idCriado(token("staff.teste@exemplo.com"), MAPA_DAS_DORES);
        mvc.perform(delete("/api/staff/missoes/" + id).header("Authorization", token("congressista.teste@exemplo.com")))
            .andExpect(status().isForbidden());
    }
}
