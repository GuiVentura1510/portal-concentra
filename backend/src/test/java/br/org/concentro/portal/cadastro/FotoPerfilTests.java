package br.org.concentro.portal.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.org.concentro.portal.tripulacao.Tripulacao;
import br.org.concentro.portal.tripulacao.TripulacaoRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Base64;
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

// Foto de perfil: envio, troca, remoção (pela pessoa e pelo staff), leitura pública
// pela chave e a chave nas respostas que mostram pessoas.
@SpringBootTest(properties = "app.staff.emails=staff.foto@exemplo.com")
@AutoConfigureMockMvc
@Transactional
class FotoPerfilTests {

    private static final String SENHA = "senhaSegura123";

    // Não são imagens de verdade: a API só confere o começo do arquivo (FF D8 FF,
    // a assinatura de todo JPEG) e o tamanho.
    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
    private static final byte[] OUTRA_FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 9, 8, 7};

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EjRepository ejs;

    @Autowired
    private CongressistaRepository congressistas;

    @Autowired
    private TripulacaoRepository tripulacoes;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criaContas() {
        String hash = passwordEncoder.encode(SENHA);
        Ej ej = ejs.findAllByOrderByNomeAsc().get(0);
        congressistas.save(new Congressista("Staff Foto", "staff.foto@exemplo.com", hash, ej, Cargo.DIRETOR,
            TempoMej.MAIS_DE_2_ANOS, AreaAtuacao.PROJETOS));
        congressistas.save(new Congressista("Ana Fotografada", "ana.foto@exemplo.com", hash, ej,
            Cargo.PRESIDENTE, TempoMej.DE_1_A_2_ANOS, AreaAtuacao.PRESIDENCIA));
        congressistas.save(new Congressista("Bruno Sem Foto", "bruno.foto@exemplo.com", hash, ej,
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

    private Long id(String email) {
        return congressistas.findByEmail(email).orElseThrow().getId();
    }

    // Envia a foto como o site faz: o arquivo em base64 dentro de um JSON.
    private ResultActions enviar(String token, byte[] foto) throws Exception {
        return enviarTexto(token, Base64.getEncoder().encodeToString(foto));
    }

    private ResultActions enviarTexto(String token, String imagem) throws Exception {
        String corpo = "{\"imagem\": \"" + imagem + "\"}";
        return mvc.perform(put("/api/perfil/foto").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    // Envia e devolve a chave que forma o endereço da foto.
    private String enviarEPegarChave(String token, byte[] foto) throws Exception {
        String resposta = enviar(token, foto).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.fotoChave");
    }

    @Test
    void fotoEnviadaApareceNoPerfilEPodeSerLidaSemLogin() throws Exception {
        String ana = token("ana.foto@exemplo.com");
        String chave = enviarEPegarChave(ana, FOTO);

        mvc.perform(get("/api/perfil").header("Authorization", ana))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fotoChave").value(chave));

        // Sem token, como a tag <img> do navegador. O navegador pode guardar por um ano.
        mvc.perform(get("/api/fotos/" + chave))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
            .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
            .andExpect(content().bytes(FOTO));
    }

    @Test
    void quemNaoEnviouFotoNaoTemChave() throws Exception {
        mvc.perform(get("/api/perfil").header("Authorization", token("bruno.foto@exemplo.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fotoChave").doesNotExist());
        mvc.perform(get("/api/fotos/chave-que-nao-existe")).andExpect(status().isNotFound());
    }

    @Test
    void fotoNovaTrocaAChaveEAAntigaSome() throws Exception {
        String ana = token("ana.foto@exemplo.com");
        String antiga = enviarEPegarChave(ana, FOTO);
        // Lê uma vez, para a foto antiga ficar guardada em memória antes da troca.
        mvc.perform(get("/api/fotos/" + antiga)).andExpect(status().isOk());

        String nova = enviarEPegarChave(ana, OUTRA_FOTO);

        assertThat(nova).isNotEqualTo(antiga);
        mvc.perform(get("/api/fotos/" + antiga)).andExpect(status().isNotFound());
        mvc.perform(get("/api/fotos/" + nova)).andExpect(status().isOk()).andExpect(content().bytes(OUTRA_FOTO));
        mvc.perform(get("/api/perfil").header("Authorization", ana)).andExpect(jsonPath("$.fotoChave").value(nova));
    }

    @Test
    void pessoaRemoveAPropriaFoto() throws Exception {
        String ana = token("ana.foto@exemplo.com");
        String chave = enviarEPegarChave(ana, FOTO);
        mvc.perform(get("/api/fotos/" + chave)).andExpect(status().isOk());

        mvc.perform(delete("/api/perfil/foto").header("Authorization", ana)).andExpect(status().isNoContent());

        mvc.perform(get("/api/fotos/" + chave)).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfil").header("Authorization", ana))
            .andExpect(jsonPath("$.fotoChave").doesNotExist());
        // Remover de novo, já sem foto, não é erro.
        mvc.perform(delete("/api/perfil/foto").header("Authorization", ana)).andExpect(status().isNoContent());
    }

    @Test
    void recusaOQueNaoEJpeg() throws Exception {
        String ana = token("ana.foto@exemplo.com");
        // Começo de um arquivo PNG.
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        enviar(ana, png).andExpect(status().isBadRequest());
        enviarTexto(ana, "isto nao e base64!").andExpect(status().isBadRequest());
        enviarTexto(ana, "").andExpect(status().isBadRequest());
        mvc.perform(get("/api/perfil").header("Authorization", ana))
            .andExpect(jsonPath("$.fotoChave").doesNotExist());
    }

    @Test
    void recusaFotoAcimaDoLimite() throws Exception {
        byte[] grande = new byte[FotoService.LIMITE_BYTES + 1];
        grande[0] = (byte) 0xFF;
        grande[1] = (byte) 0xD8;
        grande[2] = (byte) 0xFF;
        enviar(token("ana.foto@exemplo.com"), grande).andExpect(status().isBadRequest());
    }

    @Test
    void envioERemocaoExigemLogin() throws Exception {
        String corpo = "{\"imagem\": \"" + Base64.getEncoder().encodeToString(FOTO) + "\"}";
        mvc.perform(put("/api/perfil/foto").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/perfil/foto")).andExpect(status().isUnauthorized());
    }

    @Test
    void staffVeAFotoNaBuscaEPodeRemover() throws Exception {
        String chave = enviarEPegarChave(token("ana.foto@exemplo.com"), FOTO);
        String staff = token("staff.foto@exemplo.com");
        Long anaId = id("ana.foto@exemplo.com");

        mvc.perform(get("/api/staff/congressistas").param("busca", "ana.foto").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].fotoChave").value(hasItem(chave)));

        // Só o staff remove a foto dos outros.
        mvc.perform(delete("/api/staff/congressistas/" + anaId + "/foto")
                .header("Authorization", token("bruno.foto@exemplo.com")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/fotos/" + chave)).andExpect(status().isOk());

        mvc.perform(delete("/api/staff/congressistas/" + anaId + "/foto").header("Authorization", staff))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/fotos/" + chave)).andExpect(status().isNotFound());
    }

    @Test
    void fotoApareceParaOsColegasDeTripulacao() throws Exception {
        String chave = enviarEPegarChave(token("ana.foto@exemplo.com"), FOTO);
        Tripulacao tripulacao = tripulacoes.save(new Tripulacao("Tripulação de teste da foto"));
        congressistas.findByEmail("ana.foto@exemplo.com").orElseThrow().entrarNa(tripulacao);
        congressistas.findByEmail("bruno.foto@exemplo.com").orElseThrow().entrarNa(tripulacao);

        mvc.perform(get("/api/tripulacao").header("Authorization", token("bruno.foto@exemplo.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membros.length()").value(2))
            .andExpect(jsonPath("$.membros[*].fotoChave").value(hasItem(chave)));
    }

    // Remover o cadastro leva a foto junto.
    @Test
    void removerOCadastroApagaAFoto() throws Exception {
        String chave = enviarEPegarChave(token("ana.foto@exemplo.com"), FOTO);
        mvc.perform(get("/api/fotos/" + chave)).andExpect(status().isOk());

        mvc.perform(delete("/api/staff/congressistas/" + id("ana.foto@exemplo.com"))
                .header("Authorization", token("staff.foto@exemplo.com")))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/fotos/" + chave)).andExpect(status().isNotFound());
    }
}
