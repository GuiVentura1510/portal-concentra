package br.org.concentro.portal.tripulacao;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Aba Diário do painel do staff: ver as fotos anexadas e tirar as inadequadas.
@RestController
@RequestMapping("/api/staff/diario")
public class StaffDiarioController {

    private final DiarioService diario;

    public StaffDiarioController(DiarioService diario) {
        this.diario = diario;
    }

    @GetMapping
    public List<FotoDiarioResposta> recentes() {
        return diario.recentes();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id) {
        diario.apagarPeloStaff(id);
    }
}
