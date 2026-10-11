package br.org.concentro.portal.acesso;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Dados que o formulário de login envia. Sem tamanho mínimo de senha:
// uma senha curta só recebe "e-mail ou senha incorretos".
public record LoginRequisicao(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 72) String senha) {
}
