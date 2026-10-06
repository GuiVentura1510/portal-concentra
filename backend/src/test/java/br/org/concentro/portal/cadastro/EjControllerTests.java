package br.org.concentro.portal.cadastro;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class EjControllerTests {

    @Autowired
    private MockMvc mvc;

    // A chamada vai sem login de propósito: o teste também prova
    // que a rota está liberada no SecurityConfig.
    @Test
    void listaAsEjsSemExigirLogin() throws Exception {
        mvc.perform(get("/api/ejs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(54))))
            .andExpect(jsonPath("$[*].nome", hasItem("Orc'estra")))
            .andExpect(jsonPath("$[0].id").isNumber());
    }
}
