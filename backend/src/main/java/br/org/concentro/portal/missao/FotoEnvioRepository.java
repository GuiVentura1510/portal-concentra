package br.org.concentro.portal.missao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Acesso à tabela "foto_envio". O id é o da conclusão.
public interface FotoEnvioRepository extends JpaRepository<FotoEnvio, Long> {

    Optional<FotoEnvio> findByChave(String chave);
}
