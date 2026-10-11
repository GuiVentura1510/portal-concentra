import { useEffect, useState } from "react";

import { api, ErroApi } from "@/lib/api";
import { CARGOS, rotuloDe } from "@/lib/opcoes";

type Membro = { id: number; nome: string; ejNome: string; cargo: string };
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

// Tripulação de quem está logado. Até a montagem, em 15/10, a API responde 404
// e a tela mostra só o aviso.
export function MinhaTripulacao({ meuId }: { meuId: number }) {
  const [estado, setEstado] = useState<Estado>({ tipo: "carregando" });

  useEffect(() => {
    api<Tripulacao>("/api/tripulacao", { autenticado: true })
      .then((tripulacao) => setEstado({ tipo: "pronta", tripulacao }))
      .catch((e) =>
        setEstado(e instanceof ErroApi && e.status === 404 ? { tipo: "aguardando" } : { tipo: "falha" }),
      );
  }, []);

  if (estado.tipo === "carregando") {
    return <p className="text-sm text-muted-foreground">Carregando sua tripulação...</p>;
  }
  if (estado.tipo === "aguardando") {
    return <p className="text-sm">Sua tripulação será revelada no dia 15 de outubro.</p>;
  }
  if (estado.tipo === "falha") {
    return <p className="text-sm">Não foi possível carregar sua tripulação. Tente de novo em instantes.</p>;
  }

  const { tripulacao } = estado;
  const capitao = tripulacao.membros.find((m) => m.id === tripulacao.capitaoId);

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-3">
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
      <ul className="flex flex-col gap-2">
        {tripulacao.membros.map((membro) => (
          <li key={membro.id} className="text-sm">
            <p className="font-medium">
              {membro.nome}
              {membro.id === meuId && " (você)"}
              {membro.id === tripulacao.capitaoId && " · capitão"}
            </p>
            <p className="text-xs text-muted-foreground">
              {membro.ejNome} · {rotuloDe(CARGOS, membro.cargo)}
            </p>
          </li>
        ))}
      </ul>
    </section>
  );
}
