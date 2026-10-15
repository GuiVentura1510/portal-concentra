import { Anchor, ArrowRight, MapPin } from "lucide-react";
import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router";

import { Avatar } from "@/components/Avatar";
import { Button } from "@/components/ui/button";
import { api, ErroApi } from "@/lib/api";
import { BOTAO_EXPEDICAO } from "@/lib/estilos";
import { CARGOS, TEMPOS_MEJ, contar, rotuloDe } from "@/lib/opcoes";
import type { Perfil } from "@/lib/perfil";
import { lerToken } from "@/lib/sessao";

type Membro = {
  id: number;
  nome: string;
  ejNome: string;
  cargo: string;
  // Pode faltar se o site subir antes da API numa publicação.
  tempoMej?: string | null;
  fotoChave: string | null;
};
type DadosTripulacao = {
  id: number;
  nome: string;
  pontoEncontro: string | null;
  capitaoId: number | null;
  membros: Membro[];
};
type PosicaoTripulacao = { posicao: number; tripulacaoId: number; pontos: number };
type MissaoResumida = { alcance: "TRIPULACAO" | "INDIVIDUAL"; minhaConclusao: { status: string } | null };

// Números do topo. Cada um pode faltar se a chamada falhar: a página continua
// mostrando a tripulação, que é o principal.
type Placar = {
  pontos: number | null;
  posicao: number | null;
  totalTripulacoes: number | null;
  cumpridas: number | null;
  totalMissoes: number | null;
};

type Estado =
  | { tipo: "carregando" }
  | { tipo: "sem-login" }
  | { tipo: "falha" }
  | { tipo: "staff" }
  | { tipo: "aguardando" }
  | { tipo: "pronta"; meuId: number; minhaFotoChave: string | null; tripulacao: DadosTripulacao; placar: Placar };

// Aba Tripulação: quem são os colegas, onde se encontram, quem é o capitão e
// como a tripulação está no ranking. Os pontos individuais de cada membro não
// aparecem aqui (ficam no ranking individual), para não virar disputa interna.
export default function Tripulacao() {
  const [estado, setEstado] = useState<Estado>(() => (lerToken() ? { tipo: "carregando" } : { tipo: "sem-login" }));

  useEffect(() => {
    if (!lerToken()) return;
    api<Perfil>("/api/perfil", { autenticado: true })
      .then(async (perfil) => {
        if (perfil.staff) {
          setEstado({ tipo: "staff" });
          return;
        }
        let tripulacao: DadosTripulacao;
        try {
          tripulacao = await api<DadosTripulacao>("/api/tripulacao", { autenticado: true });
        } catch (e) {
          // 404 até a montagem, no sábado (17/10), às 12h.
          setEstado(e instanceof ErroApi && e.status === 404 ? { tipo: "aguardando" } : { tipo: "falha" });
          return;
        }
        const [ranking, missoes] = await Promise.allSettled([
          api<PosicaoTripulacao[]>("/api/ranking", { autenticado: true }),
          api<MissaoResumida[]>("/api/missoes", { autenticado: true }),
        ]);
        setEstado({
          tipo: "pronta",
          meuId: perfil.id,
          minhaFotoChave: perfil.fotoChave,
          tripulacao,
          placar: montarPlacar(tripulacao.id, ranking, missoes),
        });
      })
      .catch((e) => {
        setEstado(e instanceof ErroApi && e.status === 401 ? { tipo: "sem-login" } : { tipo: "falha" });
      });
  }, []);

  if (estado.tipo === "sem-login") return <Navigate to="/login" replace />;

  return (
    <main className="mx-auto flex w-full max-w-md flex-col gap-6 px-5 pt-[max(1.5rem,env(safe-area-inset-top))] pb-8">
      <header className="text-center">
        <p className="text-sm font-semibold text-creme/80">Sua tripulação</p>
        <h1 className="mt-1 font-titulo text-3xl font-bold break-words text-creme italic">
          {estado.tipo === "pronta" ? estado.tripulacao.nome : "Tripulação"}
        </h1>
      </header>

      {estado.tipo === "carregando" && <p className="text-center text-sm text-muted-foreground">Carregando...</p>}

      {estado.tipo === "falha" && (
        <Cartao>
          <p className="text-center text-sm">Não foi possível carregar sua tripulação.</p>
          <Button className="h-10 w-full" onClick={() => window.location.reload()}>
            Tentar de novo
          </Button>
        </Cartao>
      )}

      {estado.tipo === "staff" && (
        <Cartao>
          <p className="text-center text-sm">
            Contas do staff não entram em tripulação. As tripulações ficam no painel, na aba Início.
          </p>
        </Cartao>
      )}

      {estado.tipo === "aguardando" && (
        <Cartao>
          <p className="text-center text-sm">
            Sua tripulação será revelada no <strong>sábado, 17/10, ao meio-dia</strong>.
          </p>
        </Cartao>
      )}

      {estado.tipo === "pronta" && <Quadro {...estado} />}
    </main>
  );
}

function montarPlacar(
  tripulacaoId: number,
  ranking: PromiseSettledResult<PosicaoTripulacao[]>,
  missoes: PromiseSettledResult<MissaoResumida[]>,
): Placar {
  const placar: Placar = { pontos: null, posicao: null, totalTripulacoes: null, cumpridas: null, totalMissoes: null };
  if (ranking.status === "fulfilled") {
    // O ranking das tripulações lista todas, inclusive as que ainda têm 0 ponto.
    const minha = ranking.value.find((p) => p.tripulacaoId === tripulacaoId);
    if (minha) {
      placar.pontos = minha.pontos;
      placar.posicao = minha.posicao;
      placar.totalTripulacoes = ranking.value.length;
    }
  }
  if (missoes.status === "fulfilled") {
    // Mesma conta da aba Missões: só as missões da tripulação que estão visíveis.
    const daTripulacao = missoes.value.filter((m) => m.alcance === "TRIPULACAO");
    placar.totalMissoes = daTripulacao.length;
    placar.cumpridas = daTripulacao.filter((m) => m.minhaConclusao?.status === "APROVADA").length;
  }
  return placar;
}

// Cartão branco: o data-slot faz valerem as cores de superfície clara do index.css.
function Cartao({ children }: { children: ReactNode }) {
  return (
    <div data-slot="card" className="flex flex-col gap-4 rounded-[2rem] bg-white p-5 text-[#1d2252]">
      {children}
    </div>
  );
}

function Quadro({
  meuId,
  minhaFotoChave,
  tripulacao,
  placar,
}: {
  meuId: number;
  minhaFotoChave: string | null;
  tripulacao: DadosTripulacao;
  placar: Placar;
}) {
  const capitao = tripulacao.membros.find((m) => m.id === tripulacao.capitaoId);
  // Capitão primeiro; depois quem está logado; depois os outros, por nome.
  const membros = [...tripulacao.membros].sort((a, b) => {
    const peso = (m: Membro) => (m.id === tripulacao.capitaoId ? 0 : m.id === meuId ? 1 : 2);
    return peso(a) - peso(b) || a.nome.localeCompare(b.nome, "pt-BR");
  });

  return (
    <>
      <dl className="grid grid-cols-3 gap-2 text-center">
        <Numero rotulo="Pontos" valor={placar.pontos === null ? "–" : String(placar.pontos)} />
        <Numero
          rotulo={placar.totalTripulacoes ? `de ${placar.totalTripulacoes}` : "Posição"}
          valor={placar.posicao === null ? "–" : `${placar.posicao}º`}
        />
        <Numero
          rotulo="Missões"
          valor={placar.cumpridas === null ? "–" : `${placar.cumpridas}/${placar.totalMissoes}`}
        />
      </dl>

      <Cartao>
        <Info Icone={MapPin} titulo="Ponto de encontro">
          {tripulacao.pontoEncontro ?? "O staff vai informar em breve."}
        </Info>
        <Info Icone={Anchor} titulo="Capitão">
          {capitao ? capitao.nome : "A tripulação escolhe no evento."}
        </Info>
      </Cartao>

      <Cartao>
        <h2 className="text-center text-base font-bold">{contar(tripulacao.membros.length, "tripulante", "tripulantes")}</h2>
        <ul className="flex flex-col gap-3">
          {membros.map((membro) => (
            <li key={membro.id} className="flex items-center gap-3">
              <Avatar
                nome={membro.nome}
                fotoChave={membro.id === meuId ? minhaFotoChave : membro.fotoChave}
                className="size-12"
              />
              <div className="min-w-0 flex-1">
                <p className="font-semibold break-words">
                  {membro.nome}
                  {membro.id === meuId && <span className="font-normal text-muted-foreground"> (você)</span>}
                </p>
                <p className="text-xs break-words text-muted-foreground">
                  {membro.ejNome} · {rotuloDe(CARGOS, membro.cargo)}
                  {membro.tempoMej && ` · ${rotuloDe(TEMPOS_MEJ, membro.tempoMej)} de MEJ`}
                </p>
              </div>
              {membro.id === tripulacao.capitaoId && (
                <span className="shrink-0 rounded-full bg-ceu px-2 py-0.5 text-xs font-semibold text-noite">
                  Capitão
                </span>
              )}
            </li>
          ))}
        </ul>
      </Cartao>

      <Link to="/missoes" className={`flex h-12 items-center justify-center gap-2 ${BOTAO_EXPEDICAO}`}>
        Ver as missões
        <ArrowRight className="size-5" aria-hidden="true" />
      </Link>
    </>
  );
}

function Numero({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div className="flex flex-col-reverse rounded-2xl border border-lilas/25 bg-white/5 px-2 py-3">
      <dt className="text-xs text-creme/80">{rotulo}</dt>
      <dd className="font-titulo text-2xl font-bold text-ceu italic">{valor}</dd>
    </div>
  );
}

function Info({ Icone, titulo, children }: { Icone: typeof MapPin; titulo: string; children: ReactNode }) {
  return (
    <div className="flex items-start gap-3">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-[#e8f2fc]">
        <Icone className="size-5 text-[#2f5fd0]" aria-hidden="true" />
      </span>
      <div className="min-w-0">
        <p className="text-xs font-semibold text-muted-foreground uppercase">{titulo}</p>
        <p className="break-words">{children}</p>
      </div>
    </div>
  );
}
