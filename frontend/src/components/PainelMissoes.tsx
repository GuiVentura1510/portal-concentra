import { useEffect, useState, type FormEvent } from "react";

import { Campo } from "@/components/Campo";
import { Selecao } from "@/components/Selecao";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { apiParaCampo, campoParaApi, formatarHorario } from "@/lib/datas";

type FormaConclusao = "ENVIO_CAPITAO" | "STAFF_MARCA";
type Missao = {
  id: number;
  titulo: string;
  descricao: string;
  pontos: number;
  vagas: number | null;
  inicio: string | null;
  fim: string | null;
  conclusao: FormaConclusao;
  visivel: boolean;
};

const FORMAS: { valor: FormaConclusao; rotulo: string }[] = [
  { valor: "ENVIO_CAPITAO", rotulo: "O capitão envia uma resposta e o staff aprova" },
  { valor: "STAFF_MARCA", rotulo: "O staff marca como cumprida" },
];

// Missão sendo editada: "nova", uma existente, ou nenhuma (só a lista).
type Edicao = { tipo: "nova" } | { tipo: "existente"; missao: Missao } | null;

// Painel do staff, parte 2: criar e editar missões.
export function PainelMissoes() {
  const [missoes, setMissoes] = useState<Missao[] | null>(null);
  const [falha, setFalha] = useState(false);
  const [edicao, setEdicao] = useState<Edicao>(null);

  useEffect(() => {
    api<Missao[]>("/api/staff/missoes", { autenticado: true }).then(setMissoes, () => setFalha(true));
  }, []);

  function aoSalvar(salva: Missao) {
    setMissoes((lista) => {
      const atual = lista ?? [];
      return atual.some((m) => m.id === salva.id)
        ? atual.map((m) => (m.id === salva.id ? salva : m))
        : [...atual, salva];
    });
    setEdicao(null);
  }

  if (falha) return <p className="text-sm">Não foi possível carregar as missões.</p>;
  if (missoes === null) return <p className="text-sm">Carregando missões...</p>;

  if (edicao) {
    return (
      <FormMissao
        missao={edicao.tipo === "existente" ? edicao.missao : null}
        aoSalvar={aoSalvar}
        aoCancelar={() => setEdicao(null)}
      />
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <Button className="h-10" onClick={() => setEdicao({ tipo: "nova" })}>
        Nova missão
      </Button>
      {missoes.length === 0 && <p className="text-sm">Nenhuma missão cadastrada ainda.</p>}
      {missoes.map((missao) => (
        <section key={missao.id} className="flex flex-col gap-2 rounded-lg border p-3">
          <div className="flex items-start justify-between gap-2">
            <p className="font-semibold">{missao.titulo}</p>
            <span className="shrink-0 text-xs text-muted-foreground">
              {missao.visivel ? "Visível" : "Oculta"}
            </span>
          </div>
          <p className="text-sm">
            {missao.pontos} pontos · {missao.vagas ? `${missao.vagas} vagas` : "sem limite de vagas"}
          </p>
          <p className="text-sm text-muted-foreground">{textoJanela(missao)}</p>
          <p className="text-sm text-muted-foreground">
            {FORMAS.find((f) => f.valor === missao.conclusao)?.rotulo}
          </p>
          <Button variant="outline" className="h-10" onClick={() => setEdicao({ tipo: "existente", missao })}>
            Editar
          </Button>
        </section>
      ))}
    </div>
  );
}

function textoJanela(missao: Missao): string {
  if (missao.inicio && missao.fim) return `De ${formatarHorario(missao.inicio)} até ${formatarHorario(missao.fim)}`;
  if (missao.inicio) return `A partir de ${formatarHorario(missao.inicio)}`;
  if (missao.fim) return `Até ${formatarHorario(missao.fim)}`;
  return "Sem horário definido";
}

// Formulário de criação e edição. Os horários são digitados no horário de Brasília.
function FormMissao({
  missao,
  aoSalvar,
  aoCancelar,
}: {
  missao: Missao | null;
  aoSalvar: (salva: Missao) => void;
  aoCancelar: () => void;
}) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const dados = new FormData(evento.currentTarget);
    const vagas = String(dados.get("vagas") ?? "").trim();
    const corpo = {
      titulo: String(dados.get("titulo") ?? "").trim(),
      descricao: String(dados.get("descricao") ?? ""),
      pontos: Number(dados.get("pontos")),
      vagas: vagas === "" ? null : Number(vagas),
      inicio: campoParaApi(String(dados.get("inicio") ?? "")),
      fim: campoParaApi(String(dados.get("fim") ?? "")),
      conclusao: dados.get("conclusao"),
      visivel: dados.get("visivel") === "on",
    };

    setErro(null);
    setEnviando(true);
    try {
      const salva = missao
        ? await api<Missao>(`/api/staff/missoes/${missao.id}`, { metodo: "PUT", corpo, autenticado: true })
        : await api<Missao>("/api/staff/missoes", { metodo: "POST", corpo, autenticado: true });
      aoSalvar(salva);
    } catch (e) {
      setErro(
        e instanceof ErroApi && e.status === 400
          ? "Algum dado não foi aceito. Confira os campos; o fim precisa ser depois do início."
          : "Não foi possível salvar. Tente de novo.",
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4 rounded-lg border p-3">
      <p className="font-semibold">{missao ? "Editar missão" : "Nova missão"}</p>
      <Campo rotulo="Título" id="titulo">
        <Input id="titulo" name="titulo" required maxLength={120} defaultValue={missao?.titulo} className="h-10" />
      </Campo>
      <Campo rotulo="Descrição (o desafio, como aparece para as tripulações)" id="descricao">
        <textarea
          id="descricao"
          name="descricao"
          maxLength={4000}
          rows={5}
          defaultValue={missao?.descricao}
          className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
      </Campo>
      <div className="grid grid-cols-2 gap-3">
        <Campo rotulo="Pontos" id="pontos">
          <Input
            id="pontos"
            name="pontos"
            type="number"
            required
            min={0}
            max={100000}
            defaultValue={missao?.pontos ?? 100}
            className="h-10"
          />
        </Campo>
        <Campo rotulo="Vagas (vazio = sem limite)" id="vagas">
          <Input id="vagas" name="vagas" type="number" min={1} defaultValue={missao?.vagas ?? ""} className="h-10" />
        </Campo>
      </div>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <Campo rotulo="Início (horário de Brasília)" id="inicio">
          <Input
            id="inicio"
            name="inicio"
            type="datetime-local"
            defaultValue={apiParaCampo(missao?.inicio ?? null)}
            className="h-10"
          />
        </Campo>
        <Campo rotulo="Fim (horário de Brasília)" id="fim">
          <Input
            id="fim"
            name="fim"
            type="datetime-local"
            defaultValue={apiParaCampo(missao?.fim ?? null)}
            className="h-10"
          />
        </Campo>
      </div>
      <Campo rotulo="Como é concluída" id="conclusao">
        <Selecao id="conclusao" name="conclusao" required defaultValue={missao?.conclusao ?? "ENVIO_CAPITAO"}>
          {FORMAS.map((forma) => (
            <option key={forma.valor} value={forma.valor}>
              {forma.rotulo}
            </option>
          ))}
        </Selecao>
      </Campo>
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" name="visivel" defaultChecked={missao?.visivel ?? false} />
        Visível para os congressistas
      </label>

      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
      <div className="flex gap-2">
        <Button type="submit" className="h-10 flex-1" disabled={enviando}>
          {enviando ? "Salvando..." : "Salvar missão"}
        </Button>
        <Button type="button" variant="outline" className="h-10 flex-1" disabled={enviando} onClick={aoCancelar}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}
