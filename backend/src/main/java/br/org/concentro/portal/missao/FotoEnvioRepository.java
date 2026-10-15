package br.org.concentro.portal.missao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Acesso à tabela "foto_envio". O id é a chave da foto (V11).
public interface FotoEnvioRepository extends JpaRepository<FotoEnvio, String> {

    Optional<FotoEnvio> findByChave(String chave);
}
