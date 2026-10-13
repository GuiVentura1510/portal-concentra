import { User, Users } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { FotoDoEnvio } from "@/components/FotoDoEnvio";
import { Button } from "@/components/ui/button";
import { api, ErroApi } from "@/lib/api";
import { textoJanela } from "@/lib/datas";
import { reduzirFotoDeEnvio } from "@/lib/foto";

type StatusConclusao = "PENDENTE" | "APROVADA" | "RECUSADA";
type Situacao = "EM_BREVE" | "ABERTA" | "ENCERRADA" | "ESGOTADA";
// O que o envio pede, escolhido pelo staff em cada missão.
type Comprovacao = "TEXTO" | "TEXTO_OU_FOTO" | "FOTO";
type Missao = {
  id: number;
  titulo: string;
  descricao: string;
  pontos: number;
  vagas: number | null;
  vagasRestantes: number | null;
  inicio: string | null;
  fim: string | null;
  conclusao: "ENVIO_CAPITAO" | "STAFF_MARCA";
  alcance: "TRIPULACAO" | "INDIVIDUAL";
  comprovacao: Comprovacao;
  situacao: Situacao;
  minhaConclusao: {
    status: StatusConclusao;
    resposta: string | null;
    motivoRecusa: string | null;
    fotoChave: string | null;
  } | null;
  possoEnviar: boolean;
};

const SITUACOES: Record<Situacao, string> = {
  EM_BREVE: "Em breve",
  ABERTA: "Aberta",
  ENCERRADA: "Encerrada",
  ESGOTADA: "Vagas esgotadas",
};

type Aba = "TRIPULACAO" | "INDIVIDUAL";

// Ordem dentro de cada lista: abertas, em breve, esgotadas, encerradas e, por
// último, as já cumpridas (envio aceito).
const ORDEM: Record<Situacao, number> = { ABERTA: 0, EM_BREVE: 1, ESGOTADA: 2, ENCERRADA: 3 };

function cumprida(missao: Missao): boolean {
  return missao.minhaConclusao?.status === "APROVADA";
}

function ordenar(missoes: Missao[]): Missao[] {
  const peso = (m: Missao) => (cumprida(m) ? 4 : ORDEM[m.situacao]);
  // sort é estável: dentro do mesmo peso, fica a ordem que a API mandou.
  return [...missoes].sort((a, b) => peso(a) - peso(b));
}

const NOTAS: Record<Aba, string> = {
  TRIPULACAO: "Valem pontos para a tripulação. O capitão envia a resposta.",
  INDIVIDUAL: "Valem pontos só para você, no ranking individual.",
};

// Missões visíveis para quem está logado, com o envio da resposta, em duas
// listas alternadas por botões (como no Ranking): as da tripulação, enviadas
// pelo capitão, e as individuais, enviadas por cada pessoa.
export function MissoesDaTripulacao({ temTripulacao }: { temTripulacao: boolean }) {
  const [missoes, setMissoes] = useState<Missao[] | null>(null);
  const [falha, setFalha] = useState(false);
  // Antes da montagem não há missão da tripulação para cumprir: abre nas individuais.
  const [aba, setAba] = useState<Aba>(temTripulacao ? "TRIPULACAO" : "INDIVIDUAL");

  const carregar = useCallback(
    () => api<Missao[]>("/api/missoes", { autenticado: true }).then(setMissoes, () => setFalha(true)),
    [],
  );

  useEffect(() => {
    api<Missao[]>("/api/missoes", { autenticado: true }).then(setMissoes, () => setFalha(true));
  }, []);

  if (falha) return <p className="text-center text-sm">Não foi possível carregar as missões.</p>;
  if (missoes === null) return <p className="text-center text-sm text-muted-foreground">Carregando missões...</p>;
  if (missoes.length === 0) return <p className="text-center text-sm">As missões aparecem aqui durante o evento.</p>;

  const listas: Record<Aba, Missao[]> = {
    TRIPULACAO: ordenar(missoes.filter((m) => m.alcance === "TRIPULACAO")),
    INDIVIDUAL: ordenar(missoes.filter((m) => m.alcance === "INDIVIDUAL")),
  };
  const lista = listas[aba];
  const placar = (l: Missao[]) => `${l.filter(cumprida).length}/${l.length}`;

  return (
    <section className="flex flex-col gap-3">
      <div className="grid grid-cols-2 gap-2" role="group" aria-label="Tipo de missão">
        <Button
          variant={aba === "TRIPULACAO" ? "default" : "outline"}
          className="h-11"
          aria-pressed={aba === "TRIPULACAO"}
          onClick={() => setAba("TRIPULACAO")}
        >
          <Users className="size-4" aria-hidden="true" />
          Tripulação · {placar(listas.TRIPULACAO)}
        </Button>
        <Button
          variant={aba === "INDIVIDUAL" ? "default" : "outline"}
          className="h-11"
          aria-pressed={aba === "INDIVIDUAL"}
          onClick={() => setAba("INDIVIDUAL")}
        >
          <User className="size-4" aria-hidden="true" />
          Individuais · {placar(listas.INDIVIDUAL)}
        </Button>
      </div>
      <p className="text-xs text-muted-foreground">{NOTAS[aba]}</p>
      {aba === "TRIPULACAO" && !temTripulacao && (
        <p className="text-sm text-muted-foreground">
          As missões da tripulação abrem quando sua tripulação for revelada.
        </p>
      )}
      {lista.length === 0 && (
        <p className="text-center text-sm">
          {aba === "TRIPULACAO" ? "Nenhuma missão da tripulação por enquanto." : "Nenhuma missão individual por enquanto."}
        </p>
      )}
      {lista.map((missao) => (
        <CartaoMissao key={missao.id} missao={missao} temTripulacao={temTripulacao} aoEnviar={carregar} />
      ))}
    </section>
  );
}

function CartaoMissao({
  missao,
  temTripulacao,
  aoEnviar,
}: {
  missao: Missao;
  temTripulacao: boolean;
  aoEnviar: () => Promise<void>;
}) {
  const [texto, setTexto] = useState("");
  // Foto já reduzida, em base64, pronta para o envio.
  const [foto, setFoto] = useState<string | null>(null);
  const [preparandoFoto, setPreparandoFoto] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const minha = missao.minhaConclusao;
  const ativa = minha !== null && minha.status !== "RECUSADA";
  const aberta = missao.situacao === "ABERTA";
  const individual = missao.alcance === "INDIVIDUAL";
  const comprovacao = missao.comprovacao ?? "TEXTO";
  const aceitaFoto = comprovacao !== "TEXTO";
  const temTexto = texto.trim() !== "";
  const podeEnviar =
    comprovacao === "TEXTO" ? temTexto : comprovacao === "FOTO" ? foto !== null : temTexto || foto !== null;

  async function escolherFoto(arquivo: File | undefined) {
    if (!arquivo) return;
    setErro(null);
    setPreparandoFoto(true);
    try {
      setFoto(await reduzirFotoDeEnvio(arquivo));
    } catch {
      setErro("Não foi possível usar esta foto. Tente outra.");
    } finally {
      setPreparandoFoto(false);
    }
  }

  async function enviar() {
    setEnviando(true);
    setErro(null);
    try {
      await api(`/api/missoes/${missao.id}/resposta`, {
        metodo: "POST",
        corpo: { texto, foto },
        autenticado: true,
      });
      setTexto("");
      setFoto(null);
      await aoEnviar();
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <article className="flex flex-col gap-2 rounded-lg border p-3">
      <div className="flex items-start justify-between gap-2">
        <p className="font-semibold">{missao.titulo}</p>
        <span className="shrink-0 text-sm font-medium">{missao.pontos} pts</span>
      </div>
      <p className="text-xs text-muted-foreground">
        {individual ? "Individual" : "Da tripulação"} · {SITUACOES[missao.situacao]} · {textoJanela(missao.inicio, missao.fim)}
        {missao.vagasRestantes !== null && ` · ${missao.vagasRestantes} de ${missao.vagas} vagas restantes`}
      </p>
      {missao.descricao && <p className="text-sm whitespace-pre-line">{missao.descricao}</p>}

      {minha?.status === "APROVADA" && (
        <>
          <p className="text-sm font-medium">
            Cumprida! +{missao.pontos} pontos {individual ? "no seu ranking individual" : "para a tripulação"}.
          </p>
          {minha.fotoChave && <FotoDoEnvio chave={minha.fotoChave} />}
        </>
      )}
      {/* Desde 12/10 o envio vale na hora; "RECUSADA" é um envio retirado pelo staff. */}
      {minha?.status === "RECUSADA" && (
        <p className="text-sm text-destructive">
          O staff retirou este envio{minha.motivoRecusa ? `: ${minha.motivoRecusa}` : "."}{" "}
          {individual ? "Você pode enviar de novo." : "Vocês podem enviar de novo."}
        </p>
      )}

      {missao.possoEnviar && (
        <div className="flex flex-col gap-2">
          <label htmlFor={`resposta-${missao.id}`} className="text-sm font-medium">
            {individual ? "Sua resposta" : "Resposta da tripulação"}
            {comprovacao === "FOTO" && " (opcional)"}
          </label>
          <textarea
            id={`resposta-${missao.id}`}
            value={texto}
            maxLength={4000}
            rows={4}
            onChange={(e) => setTexto(e.target.value)}
            className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          />
          {aceitaFoto && (
            <div className="flex flex-col gap-2">
              <span className="text-sm font-medium">
                {comprovacao === "FOTO" ? "Foto (obrigatória)" : "Foto (opcional se escrever a resposta)"}
              </span>
              {foto && (
                <img
                  src={`data:image/jpeg;base64,${foto}`}
                  alt="Foto escolhida para o envio"
                  className="max-h-48 w-fit max-w-full rounded-lg border object-contain"
                />
              )}
              <label className="inline-flex h-10 cursor-pointer items-center justify-center rounded-lg border px-3 text-sm font-medium">
                {preparandoFoto ? "Preparando a foto..." : foto ? "Trocar foto" : "Tirar ou escolher foto"}
                <input
                  type="file"
                  accept="image/*"
                  className="sr-only"
                  disabled={preparandoFoto || enviando}
                  onChange={(e) => {
                    void escolherFoto(e.target.files?.[0]);
                    e.target.value = "";
                  }}
                />
              </label>
              {foto && (
                <Button variant="outline" className="h-10" disabled={enviando} onClick={() => setFoto(null)}>
                  Tirar a foto do envio
                </Button>
              )}
            </div>
          )}
          <Button className="h-10" disabled={enviando || preparandoFoto || !podeEnviar} onClick={enviar}>
            {enviando ? "Enviando..." : "Enviar resposta"}
          </Button>
        </div>
      )}

      {aberta && !ativa && !missao.possoEnviar && !individual && temTripulacao && missao.conclusao === "ENVIO_CAPITAO" && (
        <p className="text-sm text-muted-foreground">O capitão da tripulação envia a resposta desta missão.</p>
      )}
      {aberta && !ativa && missao.conclusao === "STAFF_MARCA" && (
        <p className="text-sm text-muted-foreground">
          Esta missão é registrada pelo staff: procure um Guardião da Rota quando terminar.
        </p>
      )}
      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
    </article>
  );
}

function mensagemDeErro(e: unknown): string {
  if (e instanceof ErroApi) {
    if (e.status === 403) return "Você não pode enviar esta resposta (nas missões da tripulação, só o capitão envia).";
    if (e.status === 409) return "Não deu para enviar: a missão fechou, as vagas acabaram ou a resposta já foi enviada.";
    if (e.status === 400) return "Envio recusado: confira a resposta e a foto que esta missão pede.";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Não foi possível enviar. Tente de novo.";
}
