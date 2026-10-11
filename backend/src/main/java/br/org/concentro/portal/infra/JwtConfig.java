package br.org.concentro.portal.infra;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// Cria e confere os tokens de login, assinados com um segredo (HMAC-SHA256).
@Configuration
public class JwtConfig {

    // Precisa ser igual ao valor padrão do application.yml.
    static final String SEGREDO_DE_DESENVOLVIMENTO = "dev-somente-local-troque-em-producao-0123456789abcdef";

    private final SecretKey chave;

    public JwtConfig(@Value("${app.jwt.secret}") String segredo) {
        // O Render define a variável RENDER em todo serviço. Lá, subir com o segredo de
        // desenvolvimento deixaria qualquer pessoa forjar tokens, então a API nem inicia.
        if (System.getenv("RENDER") != null && SEGREDO_DE_DESENVOLVIMENTO.equals(segredo)) {
            throw new IllegalStateException("Defina a variável JWT_SECRET no Render.");
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres.");
        }
        this.chave = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
