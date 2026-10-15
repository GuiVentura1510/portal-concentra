import { useCallback, useEffect, useState } from "react";

import { FotosDoEnvio } from "@/components/FotoDoEnvio";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { formatarHorario } from "@/lib/datas";

type Envio = {
  id: number;
  missaoTitulo: string;
  alcance: "TRIPULACAO" | "INDIVIDUAL";
  tripulacaoNome: string | null;
  congressistaNome: string | null;
  // Vazia quando o próprio staff registrou a missão (ex.: carimbo do passaporte).
  resposta: string | null;
  enviadaPor: string | null;
  enviadaEm: string;
  // Chave da primeira foto do envio, se houver, e de todas (desde a V11).
  fotoChave: string | null;
  fotoChaves?: string[];
};

function buscarEnvios() {
  return api<Envio[]>("/api/staff/conclusoes", { autenticado: true });
}

// Aba Envios do staff. Desde 12/10 os envios valem pontos na hora; aqui o staff
// vê os últimos (envios e registros do staff, do mais novo para o mais antigo)
// e retira o que não achar válido. A retirada apaga os pontos, devolve a vaga e
// deixa a tripulação (ou a pessoa) enviar de novo.
export function EnviosStaff() {
  const [envios, setEnvios] = useState<Envio[] | null>(null);
  const [falha, setFalha] = useState(false);
  const [busca, setBusca] = useState("");

  const atualizar = useCallback(() => {
    setFalha(false);
    buscarEnvios().then(setEnvios, () => setFalha(true));
  }, []);

  useEffect(() => {
    buscarEnvios().then(setEnvios, () => setFalha(true));
  }, []);

  function remover(id: number) {
    setEnvios((lista) => (lista ?? []).filter((e) => e.id !== id));
  }

  const termo = busca.trim().toLowerCase();
  const visiveis =
    envios?.filter(
      (e) =>
        termo === "" ||
        [e.missaoTitulo, e.tripulacaoNome, e.congressistaNome, e.enviadaPor].some((t) => t?.toLowerCase().includes(termo)),
    ) ?? null;

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        Os envios valem pontos na hora. Retire aqui o que não for válido: os pontos saem e a missão pode ser enviada de
        novo.
      </p>
      <div className="grid grid-cols-[1fr_auto] gap-2">
        <Input
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar nome ou missão"
          className="h-10"
        />
        <Button variant="outline" className="h-10" onClick={atualizar}>
          Atualizar
        </Button>
      </div>
      {falha && <p className="text-sm">Não foi possível carregar os envios.</p>}
      {envios === null && !falha && <p className="text-sm">Carregando...</p>}
      {envios?.length === 0 && <p className="text-sm">Nenhum envio ainda.</p>}
      {envios !== null && envios.length > 0 && visiveis?.length === 0 && (
        <p className="text-sm">Nenhum envio encontrado para essa busca.</p>
      )}
      {visiveis?.map((e) => <ItemEnvio key={e.id} envio={e} aoRetirar={() => remover(e.id)} />)}
    </div>
  );
}

function ItemEnvio({ envio, aoRetirar }: { envio: Envio; aoRetirar: () => void }) {
  // Primeiro toque abre o motivo; o segundo confirma a retirada.
  const [retirando, setRetirando] = useState(false);
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const registroDoStaff = envio.resposta === null;

  async function retirar() {
    setEnviando(true);
    setErro(null);
    try {
      await api(`/api/staff/conclusoes/${envio.id}/retirar`, {
        metodo: "POST",
        corpo: { motivo },
        autenticado: true,
      });
      aoRetirar();
    } catch (e) {
      setErro(
        e instanceof ErroApi && e.status === 409
          ? "Outra pessoa do staff já retirou este envio. Toque em Atualizar."
          : "Não foi possível retirar. Tente de novo.",
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <article className="flex flex-col gap-2 rounded-lg border p-3">
      <p className="font-semibold">{envio.missaoTitulo}</p>
      <p className="text-xs text-muted-foreground">
        {envio.alcance === "INDIVIDUAL"
          ? `Individual · ${envio.congressistaNome ?? "—"}${envio.tripulacaoNome ? ` (${envio.tripulacaoNome})` : ""}`
          : (envio.tripulacaoNome ?? "—")}
        {" · "}
        {registroDoStaff ? `registrada pelo staff (${envio.enviadaPor ?? "—"})` : `enviada por ${envio.enviadaPor ?? "—"}`}
        {" · "}
        {formatarHorario(envio.enviadaEm)}
      </p>
      {envio.resposta && <p className="text-sm whitespace-pre-line">{envio.resposta}</p>}
      <FotosDoEnvio fotoChaves={envio.fotoChaves} fotoChave={envio.fotoChave} />

      {!retirando ? (
        <Button variant="outline" className="h-10" onClick={() => setRetirando(true)}>
          Retirar
        </Button>
      ) : (
        <div className="flex flex-col gap-2">
          <Input
            value={motivo}
            maxLength={500}
            placeholder="Motivo (opcional, aparece para quem enviou)"
            onChange={(e) => setMotivo(e.target.value)}
            className="h-10"
          />
          <div className="grid grid-cols-2 gap-2">
            <Button variant="destructive" className="h-10" disabled={enviando} onClick={retirar}>
              {enviando ? "Retirando..." : "Confirmar retirada"}
            </Button>
            <Button variant="outline" className="h-10" disabled={enviando} onClick={() => setRetirando(false)}>
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
