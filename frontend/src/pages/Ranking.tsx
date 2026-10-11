import { useEffect, useState } from "react";
import { Link } from "react-router";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { api, ErroApi } from "@/lib/api";
import { contar } from "@/lib/opcoes";

type Posicao = { posicao: number; tripulacaoId: number; nome: string; pontos: number; missoesCumpridas: number };
type Estado = { tipo: "carregando" } | { tipo: "sem-login" } | { tipo: "falha" } | { tipo: "pronto"; lista: Posicao[] };

// Ranking das tripulações. Destaca a tripulação de quem está logado.
export default function Ranking() {
  const [estado, setEstado] = useState<Estado>({ tipo: "carregando" });
  const [minhaId, setMinhaId] = useState<number | null>(null);

  useEffect(() => {
    api<Posicao[]>("/api/ranking", { autenticado: true }).then(
      (lista) => setEstado({ tipo: "pronto", lista }),
      (e) => setEstado(e instanceof ErroApi && e.status === 401 ? { tipo: "sem-login" } : { tipo: "falha" }),
    );
    // Sem tripulação (404) ou staff: só não destaca nenhuma linha.
    api<{ id: number }>("/api/tripulacao", { autenticado: true }).then(
      (t) => setMinhaId(t.id),
      () => setMinhaId(null),
    );
  }, []);

  return (
    <main className="flex min-h-svh items-start justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Ranking da Expedição</CardTitle>
          <CardDescription>
            <Link to="/" className="underline">
              Voltar ao início
            </Link>
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-2">
          {estado.tipo === "carregando" && <p className="text-sm">Carregando...</p>}
          {estado.tipo === "falha" && <p className="text-sm">Não foi possível carregar o ranking.</p>}
          {estado.tipo === "sem-login" && (
            <p className="text-sm">
              <Link to="/login" className="underline">
                Entre
              </Link>{" "}
              para ver o ranking.
            </p>
          )}
          {estado.tipo === "pronto" && estado.lista.length === 0 && (
            <p className="text-sm">O ranking aparece quando as tripulações forem montadas.</p>
          )}
          {estado.tipo === "pronto" && estado.lista.length > 0 && (
            <>
              <p className="text-xs text-muted-foreground">Atualizado a cada poucos segundos.</p>
              <ol className="flex flex-col gap-1">
                {estado.lista.map((linha) => (
                  <li
                    key={linha.tripulacaoId}
                    className={
                      "flex items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-sm " +
                      (linha.tripulacaoId === minhaId ? "bg-muted font-semibold" : "")
                    }
                  >
                    <span>
                      {linha.posicao}º {linha.nome}
                      {linha.tripulacaoId === minhaId && " (sua)"}
                    </span>
                    <span className="shrink-0 text-right">
                      {linha.pontos} pts
                      <span className="block text-xs font-normal text-muted-foreground">
                        {contar(linha.missoesCumpridas, "missão", "missões")}
                      </span>
                    </span>
                  </li>
                ))}
              </ol>
            </>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
