package br.org.concentro.portal.tripulacao;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Ações do staff sobre as tripulações. Tudo em /api/staff exige o papel STAFF.
@RestController
@RequestMapping("/api/staff/tripulacoes")
public class StaffTripulacaoController {

    private final TripulacaoService tripulacoes;

    public StaffTripulacaoController(TripulacaoService tripulacoes) {
        this.tripulacoes = tripulacoes;
    }

    @GetMapping
    public List<TripulacaoResposta> todas() {
        return tripulacoes.todas();
    }

    // Roda uma vez, em 15/10. Uma segunda chamada responde 409.
    @PostMapping("/montar")
    public MontagemResposta montar() {
        return tripulacoes.montar();
    }

    @PutMapping("/{id}/capitao")
    public TripulacaoResposta capitao(@PathVariable Long id, @Valid @RequestBody CapitaoRequisicao requisicao) {
        return tripulacoes.definirCapitao(id, requisicao.congressistaId());
    }

    @PutMapping("/{id}/ponto-encontro")
    public TripulacaoResposta pontoEncontro(@PathVariable Long id,
            @Valid @RequestBody PontoEncontroRequisicao requisicao) {
        return tripulacoes.definirPontoEncontro(id, requisicao.texto());
    }
}
