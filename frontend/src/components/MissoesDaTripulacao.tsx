import { useCallback, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { api, ErroApi } from "@/lib/api";
import { textoJanela } from "@/lib/datas";

type StatusConclusao = "PENDENTE" | "APROVADA" | "RECUSADA";
type Situacao = "EM_BREVE" | "ABERTA" | "ENCERRADA" | "ESGOTADA";
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
  situacao: Situacao;
  minhaConclusao: { status: StatusConclusao; resposta: string | null; motivoRecusa: string | null } | null;
  possoEnviar: boolean;
};

const SITUACOES: Record<Situacao, string> = {
  EM_BREVE: "Em breve",
  ABERTA: "Aberta",
  ENCERRADA: "Encerrada",
  ESGOTADA: "Vagas esgotadas",
};

// Missões visíveis para a tripulação de quem está logado, com o envio da resposta.
export function MissoesDaTripulacao({ temTripulacao }: { temTripulacao: boolean }) {
  const [missoes, setMissoes] = useState<Missao[] | null>(null);
  const [falha, setFalha] = useState(false);

  const carregar = useCallback(
    () => api<Missao[]>("/api/missoes", { autenticado: true }).then(setMissoes, () => setFalha(true)),
    [],
  );

  useEffect(() => {
    api<Missao[]>("/api/missoes", { autenticado: true }).then(setMissoes, () => setFalha(true));
  }, []);

  if (falha) return <p className="text-sm">Não foi possível carregar as missões.</p>;
  if (missoes === null) return <p className="text-sm text-muted-foreground">Carregando missões...</p>;
  if (missoes.length === 0) return <p className="text-sm">As missões aparecem aqui durante o evento.</p>;

  return (
    <section className="flex flex-col gap-3">
      <p className="text-lg font-semibold">Missões</p>
      {!temTripulacao && (
        <p className="text-sm text-muted-foreground">O envio das respostas abre quando sua tripulação for revelada.</p>
      )}
      {missoes.map((missao) => (
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
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const minha = missao.minhaConclusao;
  const ativa = minha !== null && minha.status !== "RECUSADA";
  const aberta = missao.situacao === "ABERTA";

  async function enviar() {
    setEnviando(true);
    setErro(null);
    try {
      await api(`/api/missoes/${missao.id}/resposta`, { metodo: "POST", corpo: { texto }, autenticado: true });
      setTexto("");
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
        {SITUACOES[missao.situacao]} · {textoJanela(missao.inicio, missao.fim)}
        {missao.vagasRestantes !== null && ` · ${missao.vagasRestantes} de ${missao.vagas} vagas restantes`}
      </p>
      {missao.descricao && <p className="text-sm whitespace-pre-line">{missao.descricao}</p>}

      {minha?.status === "APROVADA" && <p className="text-sm font-medium">Cumprida! +{missao.pontos} pontos.</p>}
      {minha?.status === "PENDENTE" && <p className="text-sm">Resposta enviada. Aguardando o staff.</p>}
      {minha?.status === "RECUSADA" && (
        <p className="text-sm text-destructive">
          A resposta foi recusada{minha.motivoRecusa ? `: ${minha.motivoRecusa}` : "."} Vocês podem enviar de novo.
        </p>
      )}

      {missao.possoEnviar && (
        <div className="flex flex-col gap-2">
          <label htmlFor={`resposta-${missao.id}`} className="text-sm font-medium">
            Resposta da tripulação
          </label>
          <textarea
            id={`resposta-${missao.id}`}
            value={texto}
            maxLength={4000}
            rows={4}
            onChange={(e) => setTexto(e.target.value)}
            className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          />
          <Button className="h-10" disabled={enviando || texto.trim() === ""} onClick={enviar}>
            {enviando ? "Enviando..." : "Enviar resposta"}
          </Button>
        </div>
      )}

      {aberta && !ativa && !missao.possoEnviar && temTripulacao && missao.conclusao === "ENVIO_CAPITAO" && (
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
    if (e.status === 403) return "Só o capitão envia as respostas da tripulação.";
    if (e.status === 409) return "Não deu para enviar: a missão fechou, as vagas acabaram ou sua tripulação já enviou.";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Não foi possível enviar. Tente de novo.";
}
