package br.org.concentro.portal.infra;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas: quem ainda não tem conta precisa alcançá-las.
                .requestMatchers("/api/ping", "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ejs").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/cadastro", "/api/login").permitAll()
                // Fotos de perfil: a tag <img> não envia o token. Quem protege a
                // foto é a chave sorteada no endereço.
                .requestMatchers(HttpMethod.GET, "/api/fotos/*").permitAll()
                // Página de erro interna do Spring: sem isto, todo erro vira 403.
                .requestMatchers("/error").permitAll()
                // Rotas do staff: exigem o papel STAFF no token.
                .requestMatchers("/api/staff/**").hasAuthority("STAFF")
                // Todo o resto exige o token do login.
                .anyRequest().authenticated())
            // Lê o cabeçalho "Authorization: Bearer <token>" e confere a assinatura.
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversorDePapeis())));
        return http.build();
    }

    // Transforma a lista "papeis" do token em permissões do Spring (ex.: "STAFF").
    private static JwtAuthenticationConverter conversorDePapeis() {
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName("papeis");
        papeis.setAuthorityPrefix("");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        return conversor;
    }

    // Gera e confere o hash das senhas (BCrypt).
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origens);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
