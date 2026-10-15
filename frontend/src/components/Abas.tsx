import { House, ListChecks, Trophy, User, Users } from "lucide-react";
import { NavLink, Outlet } from "react-router";

import { useLogado } from "@/lib/sessao";

const ABAS = [
  { para: "/", rotulo: "Início", Icone: House },
  { para: "/missoes", rotulo: "Missões", Icone: ListChecks },
  { para: "/tripulacao", rotulo: "Tripulação", Icone: Users },
  { para: "/ranking", rotulo: "Ranking", Icone: Trophy },
  { para: "/perfil", rotulo: "Perfil", Icone: User },
];

// Moldura das telas de quem está logado: a tela da rota atual e, embaixo, a
// barra de 5 abas. Fica presa ao rodapé porque o portal é usado no celular, com
// o polegar. Visitante não vê a barra: as abas só fazem sentido com login.
export function ComAbas() {
  const logado = useLogado();

  return (
    // A <div> existe com ou sem login de propósito: se a estrutura mudasse ao
    // sair da conta, o React recriaria a tela aberta no meio da navegação.
    // Com login, a barra tem 4rem de altura. O espaço embaixo impede que ela
    // cubra o fim da tela, e a altura mínima de cada tela desconta esses 4rem
    // para não sobrar rolagem em telas curtas.
    <div className={logado ? "pb-16 [&>main]:min-h-[calc(100svh-4rem)]" : undefined}>
      <Outlet />
      {logado && (
        <nav aria-label="Seções do portal" className="fixed inset-x-0 bottom-0 z-10 border-t bg-background">
          <ul className="mx-auto grid h-16 max-w-md grid-cols-5">
            {ABAS.map(({ para, rotulo, Icone }) => (
              <li key={para}>
                <NavLink
                  to={para}
                  // "end": o início só fica marcado no endereço "/" exato.
                  end
                  className={({ isActive }) =>
                    // text-[11px]: com 5 abas, "Tripulação" precisa caber em 72px (tela de 360px).
                    "flex h-full flex-col items-center justify-center gap-1 text-[11px] " +
                    (isActive ? "font-semibold text-foreground" : "text-muted-foreground")
                  }
                >
                  <Icone className="size-5" aria-hidden="true" />
                  {rotulo}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </div>
  );
}
