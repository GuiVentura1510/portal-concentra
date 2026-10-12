package br.org.concentro.portal.cadastro;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Acesso à tabela "foto_perfil". O id é o do congressista dono da foto.
public interface FotoPerfilRepository extends JpaRepository<FotoPerfil, Long> {

    Optional<FotoPerfil> findByChave(String chave);
}
