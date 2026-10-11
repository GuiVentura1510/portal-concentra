package br.org.concentro.portal.acesso;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Sabe quem é do staff. A lista vem da configuração (STAFF_EMAILS), então
// promover alguém é só incluir o e-mail no Render, sem mexer no banco.
@Component
public class EquipeStaff {

    private final Set<String> emails;

    public EquipeStaff(@Value("${app.staff.emails:}") List<String> emails) {
        this.emails = emails.stream()
            .map(e -> e.trim().toLowerCase(Locale.ROOT))
            .filter(e -> !e.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    public boolean ehStaff(String email) {
        return email != null && emails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
