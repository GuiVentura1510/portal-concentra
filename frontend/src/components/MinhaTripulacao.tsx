import { useEffect, useState } from "react";

import { Avatar } from "@/components/Avatar";
import { api, ErroApi } from "@/lib/api";
import { CARGOS, rotuloDe } from "@/lib/opcoes";

type Membro = { id: number; nome: string; ejNome: string; cargo: string; fotoChave: string | null };
type Tripulacao = {
  id: number;
  nome: string;
  pontoEncontro: string | null;
  capitaoId: number | null;
  membros: Membro[];
};
type Estado =
  | { tipo: "carregando" }
  | { tipo: "aguardando" }
  | { tipo: "falha" }
  | { tipo: "pronta"; tripulacao: Tripulacao };

// Tripulação de quem está logado. Até a montagem (sábado, 17/10, às 12h), a API responde 404
// e a tela mostra só o aviso.
export function MinhaTripulacao({
  meuId,
  minhaFotoChave,
  aoCarregar,
}: {
  meuId: number;
  // Foto atual de quem está logado. A lista de membros é buscada uma vez; sem
  // isto, quem troca a foto na aba Perfil continuaria vendo a antiga aqui.
  minhaFotoChave?: string | null;
  // Avisa a tela de cima se a pessoa já tem tripulação.
  aoCarregar?: (temTripulacao: boolean) => void;
}) {
  const [estado, setEstado] = useState<Estado>({ tipo: "carregando" });

  useEffect(() => {
    api<Tripulacao>("/api/tripulacao", { autenticado: true })
      .then((tripulacao) => {
        setEstado({ tipo: "pronta", tripulacao });
        aoCarregar?.(true);
      })
      .catch((e) => {
        setEstado(e instanceof ErroApi && e.status === 404 ? { tipo: "aguardando" } : { tipo: "falha" });
        aoCarregar?.(false);
      });
  }, [aoCarregar]);

  if (estado.tipo === "carregando") {
    return <p className="text-center text-sm text-muted-foreground">Carregando sua tripulação...</p>;
  }
  if (estado.tipo === "aguardando") {
    return (
      <p className="rounded-lg border border-dashed px-3 py-4 text-center text-sm">
        Sua tripulação será revelada no <strong>sábado, 17/10, ao meio-dia</strong>.
      </p>
    );
  }
  if (estado.tipo === "falha") {
    return <p className="text-center text-sm">Não foi possível carregar sua tripulação. Tente de novo em instantes.</p>;
  }

  const { tripulacao } = estado;
  const capitao = tripulacao.membros.find((m) => m.id === tripulacao.capitaoId);

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-3 text-center">
      <div>
        <p className="text-xs text-muted-foreground">Sua tripulação</p>
        <p className="text-lg font-semibold">{tripulacao.nome}</p>
      </div>
      <p className="text-sm">
        <span className="text-muted-foreground">Ponto de encontro: </span>
        {tripulacao.pontoEncontro ?? "o staff vai informar em breve."}
      </p>
      <p className="text-sm">
        <span className="text-muted-foreground">Capitão: </span>
        {capitao ? capitao.nome : "a tripulação escolhe no evento."}
      </p>
      <ul className="mx-auto flex w-fit flex-col gap-2 text-left">
        {tripulacao.membros.map((membro) => (
          <li key={membro.id} className="flex items-center gap-3 text-sm">
            <Avatar
              nome={membro.nome}
              fotoChave={membro.id === meuId && minhaFotoChave !== undefined ? minhaFotoChave : membro.fotoChave}
            />
            <div className="min-w-0">
              <p className="font-medium">
                {membro.nome}
                {membro.id === meuId && " (você)"}
                {membro.id === tripulacao.capitaoId && " · capitão"}
              </p>
              <p className="text-xs text-muted-foreground">
                {membro.ejNome} · {rotuloDe(CARGOS, membro.cargo)}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
