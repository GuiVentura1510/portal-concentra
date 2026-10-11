package br.org.concentro.portal.cadastro;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Dados que o formulário de cadastro envia, com as regras de cada campo.
// Se alguma regra falhar, a API responde 400 antes de chegar ao serviço.
public record CadastroRequisicao(
    @NotBlank @Size(max = 120) String nome,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 72) String senha,
    @NotNull Long ejId,
    @NotNull Cargo cargo,
    @NotNull TempoMej tempoMej,
    @NotNull AreaAtuacao areaAtuacao) {
}
