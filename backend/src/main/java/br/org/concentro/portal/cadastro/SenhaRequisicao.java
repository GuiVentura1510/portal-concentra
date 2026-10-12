package br.org.concentro.portal.cadastro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Troca de senha pela própria pessoa: exige a senha atual.
public record SenhaRequisicao(
    @NotBlank String senhaAtual,
    @NotBlank @Size(min = 8, max = 72) String novaSenha) {
}
