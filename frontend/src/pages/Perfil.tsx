import { useEffect, useState } from "react";
import { Navigate, useNavigate } from "react-router";

import { EditarDados, TrocarSenha } from "@/components/EditarPerfil";
import { FotoDoPerfil } from "@/components/FotoDoPerfil";
import { MinhaTripulacao } from "@/components/MinhaTripulacao";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { api, ErroApi } from "@/lib/api";
import { AREAS_ATUACAO, CARGOS, TEMPOS_MEJ, contar, rotuloDe } from "@/lib/opcoes";
import type { Perfil as DadosDoPerfil } from "@/lib/perfil";
import { lerToken, sair } from "@/lib/sessao";

type Estado = "carregando" | "sem-login" | "pronto" | "falha";

// Aba Perfil: a foto, os dados do cadastro, a tripulação e as missões já
// cumpridas. É também onde a pessoa sai da conta.
export default function Perfil() {
  const navegar = useNavigate();
  const [estado, setEstado] = useState<Estado>(() => (lerToken() ? "carregando" : "sem-login"));
  const [perfil, setPerfil] = useState<DadosDoPerfil | null>(null);
  const [editando, setEditando] = useState(false);

  useEffect(() => {
    if (!lerToken()) return;
    api<DadosDoPerfil>("/api/perfil", { autenticado: true })
      .then((dados) => {
        setPerfil(dados);
        setEstado("pronto");
      })
      .catch((e) => {
        // 401: o token venceu e já foi apagado; a pessoa entra de novo.
        setEstado(e instanceof ErroApi && e.status === 401 ? "sem-login" : "falha");
      });
  }, []);

  function sairDaConta() {
    sair();
    navegar("/");
  }

  if (estado === "sem-login") return <Navigate to="/login" replace />;

  return (
    <main className="flex min-h-svh items-start justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle>{perfil ? perfil.nome : "Seu perfil"}</CardTitle>
          <CardDescription>
            {perfil ? `${perfil.ejNome} · ${rotuloDe(CARGOS, perfil.cargo)}` : "A Expedição da Estrela Polar"}
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {estado === "carregando" && <p className="text-sm">Carregando...</p>}

          {estado === "falha" && (
            <>
              <p className="text-sm">Não foi possível carregar seus dados.</p>
              <Button className="h-10 w-full" onClick={() => window.location.reload()}>
                Tentar de novo
              </Button>
            </>
          )}

          {estado === "pronto" && perfil && (
            <>
              <FotoDoPerfil
                nome={perfil.nome}
                fotoChave={perfil.fotoChave}
                aoMudar={(fotoChave) => setPerfil({ ...perfil, fotoChave })}
                // O staff não entra em tripulação nem no ranking, então a foto dele não aparece lá.
                aviso={perfil.staff ? undefined : "Sua foto aparece para a sua tripulação e no ranking individual."}
              />

              <section className="flex flex-col gap-2">
                <div className="flex flex-col items-center gap-2">
                  <h2 className="text-lg font-semibold">Seus dados</h2>
                  {!editando && (
                    <Button variant="outline" size="sm" onClick={() => setEditando(true)}>
                      Editar dados
                    </Button>
                  )}
                </div>
                {editando ? (
                  <EditarDados
                    perfil={perfil}
                    aoSalvar={(atualizado) => {
                      setPerfil(atualizado);
                      setEditando(false);
                    }}
                    aoCancelar={() => setEditando(false)}
                  />
                ) : (
                  <dl className="mx-auto grid w-fit grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm">
                    <dt className="text-muted-foreground">EJ</dt>
                    <dd>{perfil.ejNome}</dd>
                    <dt className="text-muted-foreground">Cargo</dt>
                    <dd>{rotuloDe(CARGOS, perfil.cargo)}</dd>
                    <dt className="text-muted-foreground">Tempo de MEJ</dt>
                    <dd>{rotuloDe(TEMPOS_MEJ, perfil.tempoMej)}</dd>
                    <dt className="text-muted-foreground">Área</dt>
                    <dd>{rotuloDe(AREAS_ATUACAO, perfil.areaAtuacao)}</dd>
                    <dt className="text-muted-foreground">E-mail</dt>
                    <dd className="break-all">{perfil.email}</dd>
                  </dl>
                )}
              </section>

              <TrocarSenha />

              {perfil.staff ? (
                <p className="text-center text-sm">
                  Você está no staff, por isso não entra em tripulação nem cumpre missões. O painel do staff fica na
                  aba Início.
                </p>
              ) : (
                <>
                  <MinhaTripulacao meuId={perfil.id} minhaFotoChave={perfil.fotoChave} />
                  <MissoesCumpridas />
                </>
              )}

              <Button variant="outline" className="h-10 w-full" onClick={sairDaConta}>
                Sair
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </main>
  );
}

// Só os campos de GET /api/missoes que o perfil usa. Nas missões da tripulação,
// "minhaConclusao" é a da tripulação; nas individuais, a da própria pessoa.
type Missao = {
  id: number;
  titulo: string;
  pontos: number;
  alcance: "TRIPULACAO" | "INDIVIDUAL";
  minhaConclusao: { status: "PENDENTE" | "APROVADA" | "RECUSADA" } | null;
};

// Histórico da pessoa: o que já foi cumprido e o que espera a avaliação do staff.
// A lista vem das missões visíveis; uma missão que o staff esconder some daqui.
function MissoesCumpridas() {
  const [missoes, setMissoes] = useState<Missao[] | null>(null);
  const [falha, setFalha] = useState(false);

  useEffect(() => {
    api<Missao[]>("/api/missoes", { autenticado: true }).then(setMissoes, () => setFalha(true));
  }, []);

  const cumpridas = missoes?.filter((m) => m.minhaConclusao?.status === "APROVADA") ?? [];
  const pendentes = missoes?.filter((m) => m.minhaConclusao?.status === "PENDENTE") ?? [];

  return (
    <section className="flex flex-col gap-2 text-center">
      <h2 className="text-lg font-semibold">Missões cumpridas</h2>
      {falha && <p className="text-sm">Não foi possível carregar as missões.</p>}
      {!falha && missoes === null && <p className="text-sm text-muted-foreground">Carregando missões...</p>}
      {missoes !== null && cumpridas.length === 0 && (
        <p className="text-sm">Nenhuma ainda. As missões cumpridas aparecem aqui durante o evento.</p>
      )}
      {cumpridas.length > 0 && (
        <>
          <p className="text-xs text-muted-foreground">
            {contar(cumpridas.length, "missão cumprida", "missões cumpridas")}, somando as suas e as da tripulação.
          </p>
          <ListaDeMissoes missoes={cumpridas} />
        </>
      )}
      {pendentes.length > 0 && (
        <>
          <h3 className="text-sm font-semibold">Aguardando o staff</h3>
          <ListaDeMissoes missoes={pendentes} />
        </>
      )}
    </section>
  );
}

function ListaDeMissoes({ missoes }: { missoes: Missao[] }) {
  return (
    <ul className="flex flex-col gap-1">
      {missoes.map((missao) => (
        <li key={missao.id} className="flex items-start justify-between gap-2 rounded-lg border px-3 py-2 text-left text-sm">
          <span>
            {missao.titulo}
            <span className="block text-xs text-muted-foreground">
              {missao.alcance === "INDIVIDUAL" ? "Individual" : "Da tripulação"}
            </span>
          </span>
          <span className="shrink-0 font-medium">{missao.pontos} pts</span>
        </li>
      ))}
    </ul>
  );
}
