import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router";

import { CartaoTripulacaoStaff, type MissaoMarcavel, type Tripulacao } from "@/components/CartaoTripulacaoStaff";
import { DiarioStaff } from "@/components/DiarioStaff";
import { EnviosStaff } from "@/components/EnviosStaff";
import { PainelMissoes, type MissaoStaff } from "@/components/PainelMissoes";
import { PainelPessoas } from "@/components/PainelPessoas";
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
// Missões que o staff registra direto, separadas pelo alcance: as da tripulação
// aparecem nos cartões das tripulações; as individuais, na aba Pessoas.
type Painel = {
  resumo: Resumo;
  tripulacoes: Tripulacao[];
  marcaveis: MissaoMarcavel[];
  individuais: MissaoMarcavel[];
};
type Aba = "tripulacoes" | "missoes" | "envios" | "diario" | "pessoas";

// Busca o resumo, a lista de tripulações (se já houver) e as missões que o staff registra direto.
async function buscarPainel(): Promise<Painel> {
  const resumo = await api<Resumo>("/api/staff/tripulacoes/resumo", { autenticado: true });
  const tripulacoes =
    resumo.tripulacoes > 0 ? await api<Tripulacao[]>("/api/staff/tripulacoes", { autenticado: true }) : [];
  const missoes = await api<MissaoStaff[]>("/api/staff/missoes", { autenticado: true });
  const doStaff = missoes.filter((m) => m.conclusao === "STAFF_MARCA");
  const resumir = (m: MissaoStaff) => ({ id: m.id, titulo: m.titulo, pontos: m.pontos });
  return {
    resumo,
    tripulacoes,
    marcaveis: doStaff.filter((m) => m.alcance === "TRIPULACAO").map(resumir),
    individuais: doStaff.filter((m) => m.alcance === "INDIVIDUAL").map(resumir),
  };
}

function estadoDoErro(e: unknown): Estado {
  if (e instanceof ErroApi && e.status === 403) return "sem-acesso";
  if (e instanceof ErroApi && e.status === 401) return "sem-login";
  return "falha";
}

// Painel do staff: tripulações, missões, fila e pessoas. É a tela de início
// das contas do staff (quem decide isso é o Inicio.tsx), por isso não tem
// endereço próprio. A própria API recusa as contas que não são do staff.
export default function Staff() {
  const [estado, setEstado] = useState<Estado>("carregando");
  const [resumo, setResumo] = useState<Resumo | null>(null);
  const [tripulacoes, setTripulacoes] = useState<Tripulacao[]>([]);
  const [marcaveis, setMarcaveis] = useState<MissaoMarcavel[]>([]);
  const [individuais, setIndividuais] = useState<MissaoMarcavel[]>([]);
  const [busca, setBusca] = useState("");
  const [aba, setAba] = useState<Aba>("tripulacoes");

  const aplicar = useCallback((painel: Painel) => {
    setResumo(painel.resumo);
    setTripulacoes(painel.tripulacoes);
    setMarcaveis(painel.marcaveis);
    setIndividuais(painel.individuais);
    setEstado("pronto");
  }, []);

  useEffect(() => {
    buscarPainel().then(aplicar, (e) => setEstado(estadoDoErro(e)));
  }, [aplicar]);

  // Usado depois da montagem, para mostrar as tripulações recém-criadas, e depois
  // de mudanças nas missões, para atualizar as listas de "Registrar missão cumprida".
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
          <CardDescription>Concentra DF 2026 · 17 e 18 de outubro</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {estado === "carregando" && <p className="text-sm">Carregando...</p>}
          {/* A API recusou (403) uma conta que o perfil diz ser do staff: o
              login foi feito antes de o e-mail entrar na lista do staff, e o
              token ainda não tem o papel. Um login novo resolve. */}
          {estado === "sem-acesso" && (
            <p className="text-sm">
              Sua conta entrou no staff depois do seu último login. Abra a aba Perfil, toque em "Sair" e entre de
              novo para o painel aparecer.
            </p>
          )}
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
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-5">
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
              <Button
                variant={aba === "envios" ? "default" : "outline"}
                className="h-10"
                onClick={() => setAba("envios")}
              >
                Envios
              </Button>
              <Button
                variant={aba === "diario" ? "default" : "outline"}
                className="h-10"
                onClick={() => setAba("diario")}
              >
                Diário
              </Button>
              <Button
                variant={aba === "pessoas" ? "default" : "outline"}
                className="h-10"
                onClick={() => setAba("pessoas")}
              >
                Pessoas
              </Button>
            </div>
          )}

          {estado === "pronto" && aba === "missoes" && <PainelMissoes aoMudar={() => void recarregar()} />}
          {estado === "pronto" && aba === "envios" && <EnviosStaff />}
          {estado === "pronto" && aba === "diario" && <DiarioStaff />}
          {estado === "pronto" && aba === "pessoas" && <PainelPessoas missoesIndividuais={individuais} />}

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
                    <CartaoTripulacaoStaff key={t.id} tripulacao={t} missoesMarcaveis={marcaveis} aoAtualizar={atualizar} />
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
