import { useCallback, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { formatarHorario } from "@/lib/datas";

type Conclusao = {
  id: number;
  missaoTitulo: string;
  tripulacaoNome: string;
  resposta: string | null;
  enviadaPor: string | null;
  enviadaEm: string;
};

function buscarFila() {
  return api<Conclusao[]>("/api/staff/conclusoes?status=PENDENTE", { autenticado: true });
}

// Fila do staff: respostas enviadas pelos capitães, na ordem de chegada.
export function FilaAprovacao() {
  const [fila, setFila] = useState<Conclusao[] | null>(null);
  const [falha, setFalha] = useState(false);

  const atualizar = useCallback(() => {
    setFalha(false);
    buscarFila().then(setFila, () => setFalha(true));
  }, []);

  useEffect(() => {
    buscarFila().then(setFila, () => setFalha(true));
  }, []);

  function remover(id: number) {
    setFila((lista) => (lista ?? []).filter((c) => c.id !== id));
  }

  return (
    <div className="flex flex-col gap-3">
      <Button variant="outline" className="h-10" onClick={atualizar}>
        Atualizar fila
      </Button>
      {falha && <p className="text-sm">Não foi possível carregar a fila.</p>}
      {fila === null && !falha && <p className="text-sm">Carregando...</p>}
      {fila?.length === 0 && <p className="text-sm">Nenhuma resposta esperando avaliação.</p>}
      {fila?.map((c) => <ItemFila key={c.id} conclusao={c} aoAvaliar={() => remover(c.id)} />)}
    </div>
  );
}

function ItemFila({ conclusao, aoAvaliar }: { conclusao: Conclusao; aoAvaliar: () => void }) {
  const [recusando, setRecusando] = useState(false);
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function avaliar(acao: "aprovar" | "recusar") {
    setEnviando(true);
    setErro(null);
    try {
      await api(`/api/staff/conclusoes/${conclusao.id}/${acao}`, {
        metodo: "POST",
        corpo: acao === "recusar" ? { motivo } : undefined,
        autenticado: true,
      });
      aoAvaliar();
    } catch (e) {
      setErro(
        e instanceof ErroApi && e.status === 409
          ? "Não foi possível: as vagas da missão acabaram ou outra pessoa do staff já avaliou. Atualize a fila."
          : "Não foi possível salvar. Tente de novo.",
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <article className="flex flex-col gap-2 rounded-lg border p-3">
      <p className="font-semibold">{conclusao.missaoTitulo}</p>
      <p className="text-xs text-muted-foreground">
        {conclusao.tripulacaoNome} · enviada por {conclusao.enviadaPor ?? "—"} em {formatarHorario(conclusao.enviadaEm)}
      </p>
      <p className="text-sm whitespace-pre-line">{conclusao.resposta}</p>

      {!recusando ? (
        <div className="grid grid-cols-2 gap-2">
          <Button className="h-10" disabled={enviando} onClick={() => avaliar("aprovar")}>
            Aprovar
          </Button>
          <Button variant="outline" className="h-10" disabled={enviando} onClick={() => setRecusando(true)}>
            Recusar
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          <Input
            value={motivo}
            maxLength={500}
            placeholder="Motivo (aparece para a tripulação)"
            onChange={(e) => setMotivo(e.target.value)}
            className="h-10"
          />
          <div className="grid grid-cols-2 gap-2">
            <Button className="h-10" disabled={enviando} onClick={() => avaliar("recusar")}>
              Confirmar recusa
            </Button>
            <Button variant="outline" className="h-10" disabled={enviando} onClick={() => setRecusando(false)}>
              Voltar
            </Button>
          </div>
        </div>
      )}
      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
    </article>
  );
}
