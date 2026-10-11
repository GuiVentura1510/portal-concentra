package br.org.concentro.portal.missao;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

// O que o formulário de missão do staff envia. Os horários chegam com fuso
// (ex.: 2026-10-17T10:30:00-03:00) e são guardados em UTC.
public record MissaoRequisicao(
    @NotBlank @Size(max = 120) String titulo,
    @Size(max = 4000) String descricao,
    @NotNull @Min(0) @Max(100000) Integer pontos,
    @Min(1) Integer vagas,
    OffsetDateTime inicio,
    OffsetDateTime fim,
    @NotNull FormaConclusao conclusao,
    boolean visivel) {
}
