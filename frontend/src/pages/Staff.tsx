import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";

import { CartaoTripulacaoStaff, type Tripulacao } from "@/components/CartaoTripulacaoStaff";
import { PainelMissoes } from "@/components/PainelMissoes";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { contar } from "@/lib/opcoes";

type Resumo = { congressistas: number; comTripulacao: number; tripulacoes: number };
type Montagem = {
  tripulacoes: number;
  pessoas: number;
  menorTripulacao: number;
  maiorTripulacao: number;
  colegasDeMesmaEj: number;
};
type Estado = "carregando" | "pronto" | "sem-acesso" | "sem-login" | "falha";
type Painel = { resumo: Resumo; tripulacoes: Tripulacao[] };
type Aba = "tripulacoes" | "missoes";

// Busca o resumo e, se já houver tripulações, a lista completa.
async function buscarPainel(): Promise<Painel> {
  const resumo = await api<Resumo>("/api/staff/tripulacoes/resumo", { autenticado: true });
  const tripulacoes =
    resumo.tripulacoes > 0 ? await api<Tripulacao[]>("/api/staff/tripulacoes", { autenticado: true }) : [];
  return { resumo, tripulacoes };
}

function estadoDoErro(e: unknown): Estado {
  if (e instanceof ErroApi && e.status === 403) return "sem-acesso";
  if (e instanceof ErroApi && e.status === 401) return "sem-login";
  return "falha";
}

// Painel do staff: tripulações e missões. Só abre para contas do staff;
// a própria API recusa as outras.
export default function Staff() {
  const [estado, setEstado] = useState<Estado>("carregando");
  const [resumo, setResumo] = useState<Resumo | null>(null);
  const [tripulacoes, setTripulacoes] = useState<Tripulacao[]>([]);
  const [busca, setBusca] = useState("");
  const [aba, setAba] = useState<Aba>("tripulacoes");

  const aplicar = useCallback((painel: Painel) => {
    setResumo(painel.resumo);
    setTripulacoes(painel.tripulacoes);
    setEstado("pronto");
  }, []);

  useEffect(() => {
    buscarPainel().then(aplicar, (e) => setEstado(estadoDoErro(e)));
  }, [aplicar]);

  // Usado depois da montagem, para mostrar as tripulações recém-criadas.
  const recarregar = () => buscarPainel().then(aplicar, (e) => setEstado(estadoDoErro(e)));

  function atualizar(atualizada: Tripulacao) {
    setTripulacoes((lista) => lista.map((t) => (t.id === atualizada.id ? atualizada : t)));
  }

  const termo = busca.trim().toLowerCase();
  const visiveis = termo
    ? tripulacoes.filter(
        (t) =>
          t.nome.toLowerCase().includes(termo) ||
          t.membros.some((m) => m.nome.toLowerCase().includes(termo) || m.ejNome.toLowerCase().includes(termo)),
      )
    : tripulacoes;

  return (
    <main className="flex min-h-svh items-start justify-center p-4">
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle>Painel do staff</CardTitle>
          <CardDescription>
            <Link to="/" className="underline">
              Voltar ao início
            </Link>
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {estado === "carregando" && <p className="text-sm">Carregando...</p>}
          {estado === "sem-acesso" && <p className="text-sm">Esta área é só para o staff do evento.</p>}
          {estado === "sem-login" && (
            <p className="text-sm">
              Sua sessão terminou.{" "}
              <Link to="/login" className="underline">
                Entre de novo
              </Link>
              .
            </p>
          )}
          {estado === "falha" && (
            <>
              <p className="text-sm">Não foi possível carregar o painel.</p>
              <Button className="h-10" onClick={() => window.location.reload()}>
                Tentar de novo
              </Button>
            </>
          )}

          {estado === "pronto" && (
            <div className="grid grid-cols-2 gap-2">
              <Button
                variant={aba === "tripulacoes" ? "default" : "outline"}
                className="h-10"
                onClick={() => setAba("tripulacoes")}
              >
                Tripulações
              </Button>
              <Button
                variant={aba === "missoes" ? "default" : "outline"}
                className="h-10"
                onClick={() => setAba("missoes")}
              >
                Missões
              </Button>
            </div>
          )}

          {estado === "pronto" && aba === "missoes" && <PainelMissoes />}

          {estado === "pronto" && aba === "tripulacoes" && resumo && (
            <>
              <p className="text-sm">
                {contar(resumo.congressistas, "congressista cadastrado", "congressistas cadastrados")} ·{" "}
                {contar(resumo.tripulacoes, "tripulação", "tripulações")}
                {resumo.tripulacoes > 0 &&
                  ` · ${contar(resumo.comTripulacao, "pessoa", "pessoas")} em tripulações`}
              </p>

              {resumo.tripulacoes === 0 ? (
                <Montar pessoas={resumo.congressistas} aoMontar={recarregar} />
              ) : (
                <>
                  <Input
                    value={busca}
                    onChange={(e) => setBusca(e.target.value)}
                    placeholder="Buscar por tripulação, pessoa ou EJ"
                    className="h-10"
                  />
                  {visiveis.length === 0 && <p className="text-sm">Nada encontrado.</p>}
                  {visiveis.map((t) => (
                    <CartaoTripulacaoStaff key={t.id} tripulacao={t} aoAtualizar={atualizar} />
                  ))}
                </>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </main>
  );
}

// Botão de montagem com confirmação em dois toques, porque a montagem só roda uma vez.
function Montar({ pessoas, aoMontar }: { pessoas: number; aoMontar: () => Promise<void> }) {
  const [confirmando, setConfirmando] = useState(false);
  const [montando, setMontando] = useState(false);
  const [resultado, setResultado] = useState<Montagem | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  async function montar() {
    setMontando(true);
    setErro(null);
    try {
      const dados = await api<Montagem>("/api/staff/tripulacoes/montar", { metodo: "POST", autenticado: true });
      setResultado(dados);
      await aoMontar();
    } catch (e) {
      setErro(
        e instanceof ErroApi && e.status === 409
          ? "As tripulações já foram montadas. Recarregue a página."
          : "Não foi possível montar. Tente de novo.",
      );
    } finally {
      setMontando(false);
      setConfirmando(false);
    }
  }

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-3">
      <p className="font-semibold">Montar tripulações</p>
      <p className="text-sm">
        Distribui {pessoas === 1 ? "o congressista cadastrado" : `os ${pessoas} congressistas`} em tripulações de 5 a 6 pessoas, de EJs diferentes e misturando os
        tempos de MEJ. Roda uma única vez, no dia 15/10. Quem se cadastrar depois entra automaticamente numa
        tripulação com vaga.
      </p>
      {resultado && (
        <p className="text-sm">
          Montadas {resultado.tripulacoes} tripulações com {resultado.pessoas} pessoas (de {resultado.menorTripulacao}{" "}
          a {resultado.maiorTripulacao} por tripulação). Colegas da mesma EJ juntos: {resultado.colegasDeMesmaEj}.
        </p>
      )}
      {erro && <p className="text-sm text-destructive">{erro}</p>}
      {!confirmando ? (
        <Button className="h-10" disabled={pessoas === 0} onClick={() => setConfirmando(true)}>
          Montar tripulações
        </Button>
      ) : (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">Tem certeza? Isso não pode ser desfeito pelo painel.</p>
          <div className="flex gap-2">
            <Button className="h-10 flex-1" disabled={montando} onClick={montar}>
              {montando ? "Montando..." : "Sim, montar agora"}
            </Button>
            <Button variant="outline" className="h-10 flex-1" disabled={montando} onClick={() => setConfirmando(false)}>
              Cancelar
            </Button>
          </div>
        </div>
      )}
    </section>
  );
}
