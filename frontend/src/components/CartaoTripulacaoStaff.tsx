import { useState } from "react";

import { Avatar } from "@/components/Avatar";
import { Selecao } from "@/components/Selecao";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { CARGOS, contar, rotuloDe } from "@/lib/opcoes";

export type Membro = { id: number; nome: string; ejNome: string; cargo: string; fotoChave: string | null };
export type Tripulacao = {
  id: number;
  nome: string;
  pontoEncontro: string | null;
  capitaoId: number | null;
  membros: Membro[];
};

type Aviso = { tipo: "ok" | "erro"; texto: string } | null;

// Missão que o staff registra direto (forma "STAFF_MARCA").
export type MissaoMarcavel = { id: number; titulo: string; pontos: number };

// Uma tripulação no painel do staff: membros, capitão e ponto de encontro.
// Cada campo é salvo separado, para um erro num não apagar o outro.
export function CartaoTripulacaoStaff({
  tripulacao,
  missoesMarcaveis,
  aoAtualizar,
}: {
  tripulacao: Tripulacao;
  missoesMarcaveis: MissaoMarcavel[];
  aoAtualizar: (atualizada: Tripulacao) => void;
}) {
  const [capitaoId, setCapitaoId] = useState(tripulacao.capitaoId ? String(tripulacao.capitaoId) : "");
  const [ponto, setPonto] = useState(tripulacao.pontoEncontro ?? "");
  const [missaoId, setMissaoId] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [aviso, setAviso] = useState<Aviso>(null);

  async function salvar(caminho: string, corpo: unknown, mensagem: string) {
    setSalvando(true);
    setAviso(null);
    try {
      const atualizada = await api<Tripulacao>(caminho, { metodo: "PUT", corpo, autenticado: true });
      aoAtualizar(atualizada);
      setAviso({ tipo: "ok", texto: mensagem });
    } catch {
      setAviso({ tipo: "erro", texto: "Não foi possível salvar. Tente de novo." });
    } finally {
      setSalvando(false);
    }
  }

  async function marcar() {
    setSalvando(true);
    setAviso(null);
    const missao = missoesMarcaveis.find((m) => m.id === Number(missaoId));
    try {
      await api(`/api/staff/missoes/${missaoId}/marcar`, {
        metodo: "POST",
        corpo: { tripulacaoId: tripulacao.id },
        autenticado: true,
      });
      setAviso({ tipo: "ok", texto: `Missão registrada: +${missao?.pontos ?? 0} pontos para ${tripulacao.nome}.` });
      setMissaoId("");
    } catch (e) {
      setAviso({
        tipo: "erro",
        texto:
          e instanceof ErroApi && e.status === 409
            ? "Não registrado: a tripulação já cumpriu esta missão, tem envio na fila, ou as vagas acabaram."
            : "Não foi possível registrar. Tente de novo.",
      });
    } finally {
      setSalvando(false);
    }
  }

  return (
    <section className="flex flex-col gap-3 rounded-lg border p-3">
      <p className="font-semibold">
        {tripulacao.nome} <span className="text-sm font-normal text-muted-foreground">· {contar(tripulacao.membros.length, "pessoa", "pessoas")}</span>
      </p>

      <ul className="flex flex-col gap-1">
        {tripulacao.membros.map((membro) => (
          <li key={membro.id} className="flex items-center gap-2 text-sm">
            <Avatar nome={membro.nome} fotoChave={membro.fotoChave} className="size-7 text-xs" />
            <span className="min-w-0">
              {membro.nome}
              {membro.id === tripulacao.capitaoId && " · capitão"}
              <span className="text-xs text-muted-foreground">
                {" "}
                — {membro.ejNome}, {rotuloDe(CARGOS, membro.cargo)}
              </span>
            </span>
          </li>
        ))}
      </ul>

      <div className="flex flex-col gap-2">
        <label htmlFor={`capitao-${tripulacao.id}`} className="text-sm font-medium">
          Capitão
        </label>
        <div className="flex gap-2">
          <Selecao id={`capitao-${tripulacao.id}`} value={capitaoId} onChange={(e) => setCapitaoId(e.target.value)}>
            <option value="" disabled>
              Escolha quem a tripulação indicou
            </option>
            {tripulacao.membros.map((membro) => (
              <option key={membro.id} value={membro.id}>
                {membro.nome}
              </option>
            ))}
          </Selecao>
          <Button
            className="h-10"
            disabled={salvando || capitaoId === "" || Number(capitaoId) === tripulacao.capitaoId}
            onClick={() =>
              salvar(
                `/api/staff/tripulacoes/${tripulacao.id}/capitao`,
                { congressistaId: Number(capitaoId) },
                "Capitão salvo.",
              )
            }
          >
            Salvar
          </Button>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <label htmlFor={`ponto-${tripulacao.id}`} className="text-sm font-medium">
          Ponto de encontro
        </label>
        <div className="flex gap-2">
          <Input
            id={`ponto-${tripulacao.id}`}
            value={ponto}
            maxLength={200}
            placeholder="Ex.: totem azul, hall de entrada"
            onChange={(e) => setPonto(e.target.value)}
            className="h-10"
          />
          <Button
            className="h-10"
            disabled={salvando || ponto.trim() === (tripulacao.pontoEncontro ?? "")}
            onClick={() =>
              salvar(
                `/api/staff/tripulacoes/${tripulacao.id}/ponto-encontro`,
                { texto: ponto },
                "Ponto de encontro salvo.",
              )
            }
          >
            Salvar
          </Button>
        </div>
      </div>

      {missoesMarcaveis.length > 0 && (
        <div className="flex flex-col gap-2">
          <label htmlFor={`missao-${tripulacao.id}`} className="text-sm font-medium">
            Registrar missão cumprida
          </label>
          <div className="flex gap-2">
            <Selecao id={`missao-${tripulacao.id}`} value={missaoId} onChange={(e) => setMissaoId(e.target.value)}>
              <option value="" disabled>
                Escolha a missão
              </option>
              {missoesMarcaveis.map((missao) => (
                <option key={missao.id} value={missao.id}>
                  {missao.titulo} ({missao.pontos} pts)
                </option>
              ))}
            </Selecao>
            <Button className="h-10" disabled={salvando || missaoId === ""} onClick={marcar}>
              Registrar
            </Button>
          </div>
        </div>
      )}

      {aviso && (
        <p role="status" className={aviso.tipo === "ok" ? "text-sm" : "text-sm text-destructive"}>
          {aviso.texto}
        </p>
      )}
    </section>
  );
}
