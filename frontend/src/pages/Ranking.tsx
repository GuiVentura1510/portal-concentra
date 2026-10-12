import { useEffect, useState } from "react";
import { Link } from "react-router";

import { Avatar } from "@/components/Avatar";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { api, ErroApi } from "@/lib/api";
import { contar } from "@/lib/opcoes";

type PosicaoTripulacao = {
  posicao: number;
  tripulacaoId: number;
  nome: string;
  pontos: number;
  missoesCumpridas: number;
};
type PosicaoIndividual = {
  posicao: number;
  congressistaId: number;
  nome: string;
  ejNome: string;
  tripulacaoNome: string | null;
  pontos: number;
  missoesCumpridas: number;
  fotoChave: string | null;
};
type RankingIndividual = { posicoes: PosicaoIndividual[]; minha: PosicaoIndividual | null; total: number };

type Carga<T> = { tipo: "carregando" } | { tipo: "sem-login" } | { tipo: "falha" } | { tipo: "pronto"; dados: T };
type Aba = "tripulacoes" | "individual";

function carga<T>(promessa: Promise<T>, aplicar: (c: Carga<T>) => void) {
  promessa.then(
    (dados) => aplicar({ tipo: "pronto", dados }),
    (e) => aplicar(e instanceof ErroApi && e.status === 401 ? { tipo: "sem-login" } : { tipo: "falha" }),
  );
}

// Os dois rankings da Expedição:
// - Tripulações: só as missões da tripulação, destacando a de quem está logado.
// - Individual: só as missões individuais, destacando a própria pessoa.
export default function Ranking() {
  const [aba, setAba] = useState<Aba>("tripulacoes");
  const [tripulacoes, setTripulacoes] = useState<Carga<PosicaoTripulacao[]>>({ tipo: "carregando" });
  const [individual, setIndividual] = useState<Carga<RankingIndividual>>({ tipo: "carregando" });
  const [minhaTripulacao, setMinhaTripulacao] = useState<number | null>(null);

  useEffect(() => {
    carga(api<PosicaoTripulacao[]>("/api/ranking", { autenticado: true }), setTripulacoes);
    carga(api<RankingIndividual>("/api/ranking/individual", { autenticado: true }), setIndividual);
    // Sem tripulação (404) ou staff: só não destaca nenhuma linha.
    api<{ id: number }>("/api/tripulacao", { autenticado: true }).then(
      (t) => setMinhaTripulacao(t.id),
      () => setMinhaTripulacao(null),
    );
  }, []);

  const atual = aba === "tripulacoes" ? tripulacoes : individual;

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
        <CardContent className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-2">
            <Button
              variant={aba === "tripulacoes" ? "default" : "outline"}
              className="h-10"
              onClick={() => setAba("tripulacoes")}
            >
              Tripulações
            </Button>
            <Button
              variant={aba === "individual" ? "default" : "outline"}
              className="h-10"
              onClick={() => setAba("individual")}
            >
              Individual
            </Button>
          </div>

          {atual.tipo === "carregando" && <p className="text-sm">Carregando...</p>}
          {atual.tipo === "falha" && <p className="text-sm">Não foi possível carregar o ranking.</p>}
          {atual.tipo === "sem-login" && (
            <p className="text-sm">
              <Link to="/login" className="underline">
                Entre
              </Link>{" "}
              para ver o ranking.
            </p>
          )}

          {aba === "tripulacoes" && tripulacoes.tipo === "pronto" && (
            <ListaTripulacoes lista={tripulacoes.dados} minha={minhaTripulacao} />
          )}
          {aba === "individual" && individual.tipo === "pronto" && <ListaIndividual ranking={individual.dados} />}
        </CardContent>
      </Card>
    </main>
  );
}

function ListaTripulacoes({ lista, minha }: { lista: PosicaoTripulacao[]; minha: number | null }) {
  if (lista.length === 0) {
    return <p className="text-sm">O ranking aparece quando as tripulações forem montadas.</p>;
  }
  return (
    <>
      <p className="text-xs text-muted-foreground">
        Conta só as missões da tripulação. Atualizado a cada poucos segundos.
      </p>
      <ol className="flex flex-col gap-1">
        {lista.map((linha) => (
          <li
            key={linha.tripulacaoId}
            className={
              "flex items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-sm " +
              (linha.tripulacaoId === minha ? "bg-muted font-semibold" : "")
            }
          >
            <span>
              {linha.posicao}º {linha.nome}
              {linha.tripulacaoId === minha && " (sua)"}
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
  );
}

function ListaIndividual({ ranking }: { ranking: RankingIndividual }) {
  const { posicoes, minha, total } = ranking;
  const minhaNaLista = minha !== null && posicoes.some((p) => p.congressistaId === minha.congressistaId);

  return (
    <>
      <p className="text-xs text-muted-foreground">
        Conta só as missões individuais. Atualizado a cada poucos segundos.
      </p>
      {minha ? (
        <p className="rounded-lg bg-muted px-2 py-1.5 text-sm font-semibold">
          Você está em {minha.posicao}º lugar, com {minha.pontos} pts ({contar(minha.missoesCumpridas, "missão", "missões")}).
        </p>
      ) : (
        <p className="text-sm">Você ainda não pontuou em missões individuais.</p>
      )}
      {total === 0 ? (
        <p className="text-sm">Ninguém pontuou em missões individuais ainda.</p>
      ) : (
        <ol className="flex flex-col gap-1">
          {posicoes.map((linha) => {
            const eu = minhaNaLista && linha.congressistaId === minha?.congressistaId;
            return (
              <li
                key={linha.congressistaId}
                className={
                  "flex items-center justify-between gap-2 rounded-lg px-2 py-1.5 text-sm " +
                  (eu ? "bg-muted font-semibold" : "")
                }
              >
                <span className="flex min-w-0 items-center gap-2">
                  <Avatar nome={linha.nome} fotoChave={linha.fotoChave} className="size-8 text-xs" />
                  <span className="min-w-0">
                    {linha.posicao}º {linha.nome}
                    {eu && " (você)"}
                    <span className="block text-xs font-normal text-muted-foreground">
                      {linha.ejNome}
                      {linha.tripulacaoNome && ` · ${linha.tripulacaoNome}`}
                    </span>
                  </span>
                </span>
                <span className="shrink-0 text-right">
                  {linha.pontos} pts
                  <span className="block text-xs font-normal text-muted-foreground">
                    {contar(linha.missoesCumpridas, "missão", "missões")}
                  </span>
                </span>
              </li>
            );
          })}
        </ol>
      )}
      {total > posicoes.length && (
        <p className="text-xs text-muted-foreground">
          Mostrando as {posicoes.length} primeiras posições de {contar(total, "pessoa", "pessoas")}.
        </p>
      )}
    </>
  );
}
