package br.org.concentro.portal.cadastro;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

// E-mails da lista de inscritos (compra de ingresso), como o staff colou.
public record CruzamentoRequisicao(@NotNull @Size(max = 5000) List<String> emails) {
}
