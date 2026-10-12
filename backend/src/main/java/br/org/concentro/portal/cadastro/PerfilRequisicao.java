package br.org.concentro.portal.cadastro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Dados que a pessoa pode editar na aba Perfil. O e-mail fica de fora: é o
// login e é usado no cruzamento com a lista de inscritos.
public record PerfilRequisicao(
    @NotBlank @Size(max = 120) String nome,
    @NotNull Long ejId,
    @NotNull Cargo cargo,
    @NotNull TempoMej tempoMej,
    // Opcional só para trainee: o serviço confere com o AreaPorCargo.
    AreaAtuacao areaAtuacao) {
}
