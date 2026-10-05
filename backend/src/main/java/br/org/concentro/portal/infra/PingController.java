package br.org.concentro.portal.infra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/api/ping")
    public Map<String, String> ping() {
        String banco = (String) em.createNativeQuery("select current_database()").getSingleResult();
        return Map.of("status", "ok", "banco", banco);
    }
}
