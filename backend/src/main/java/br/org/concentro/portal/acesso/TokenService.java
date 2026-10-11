package br.org.concentro.portal.acesso;

import br.org.concentro.portal.cadastro.Congressista;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// Emite o token de login. O "subject" do token é o id do congressista.
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration validade;
    private final EquipeStaff staff;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.validade}") Duration validade,
            EquipeStaff staff) {
        this.encoder = encoder;
        this.validade = validade;
        this.staff = staff;
    }

    public String emitir(Congressista congressista) {
        Instant agora = Instant.now();
        JwtClaimsSet dados = JwtClaimsSet.builder()
            .issuer("portal-concentra")
            .subject(String.valueOf(congressista.getId()))
            .issuedAt(agora)
            .expiresAt(agora.plus(validade))
            .claim("nome", congressista.getNome())
            // Papéis lidos pelo SecurityConfig. Só o staff tem "STAFF".
            .claim("papeis", staff.ehStaff(congressista.getEmail()) ? List.of("STAFF") : List.of())
            .build();
        // O cabeçalho precisa dizer HS256, senão o encoder procura uma chave RSA.
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();
    }
}
