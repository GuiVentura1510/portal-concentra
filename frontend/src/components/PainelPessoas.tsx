import { useState, type FormEvent } from "react";

import type { MissaoMarcavel } from "@/components/CartaoTripulacaoStaff";
import { Selecao } from "@/components/Selecao";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { CARGOS, contar, rotuloDe } from "@/lib/opcoes";

type Pessoa = {
  id: number;
  nome: string;
  email: string;
  ejNome: string;
  cargo: string;
  tripulacaoNome: string | null;
  staff: boolean;
};

// Separa os e-mails de um texto colado: uma coluna de planilha, uma lista com
// vírgulas ou uma mensagem inteira. O que não parece e-mail é ignorado.
function extrairEmails(texto: string): string[] {
  const achados = texto.toLowerCase().match(/[^\s,;<>"']+@[^\s,;<>"']+\.[^\s,;<>"']+/g) ?? [];
  return [...new Set(achados)];
}

// Painel do staff, aba Pessoas: busca, missões individuais marcadas pelo staff,
// senha esquecida, remoção e cruzamento com a lista de inscritos.
export function PainelPessoas({ missoesIndividuais }: { missoesIndividuais: MissaoMarcavel[] }) {
  return (
    <div className="flex flex-col gap-6">
      <Busca missoes={missoesIndividuais} />
      <Cruzamento missoes={missoesIndividuais} />
    </div>
  );
}

function Busca({ missoes }: { missoes: MissaoMarcavel[] }) {
  const [resultado, setResultado] = useState<Pessoa[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  async function buscar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const termo = String(new FormData(evento.currentTarget).get("busca") ?? "").trim();
    setErro(null);
    try {
      setResultado(
        await api<Pessoa[]>(`/api/staff/congressistas?busca=${encodeURIComponent(termo)}`, { autenticado: true }),
      );
    } catch (e) {
      setErro(e instanceof ErroApi && e.status === 400 ? "Digite pelo menos 2 letras." : "Não foi possível buscar.");
    }
  }

  return (
    <section className="flex flex-col gap-3">
      <p className="font-semibold">Buscar pessoa</p>
      <form onSubmit={buscar} className="flex gap-2">
        <Input name="busca" placeholder="Nome, e-mail ou EJ" minLength={2} required className="h-10" />
        <Button type="submit" className="h-10">
          Buscar
        </Button>
      </form>
      {erro && <p className="text-sm text-destructive">{erro}</p>}
      {resultado?.length === 0 && <p className="text-sm">Ninguém encontrado.</p>}
      {resultado?.map((p) => (
        <CartaoPessoa
          key={p.id}
          pessoa={p}
          missoes={missoes}
          aoRemover={() => setResultado((lista) => (lista ?? []).filter((x) => x.id !== p.id))}
        />
      ))}
    </section>
  );
}

function Cruzamento({ missoes }: { missoes: MissaoMarcavel[] }) {
  const [texto, setTexto] = useState("");
  const [fora, setFora] = useState<Pessoa[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const emails = extrairEmails(texto);

  async function cruzar() {
    setEnviando(true);
    setErro(null);
    try {
      setFora(
        await api<Pessoa[]>("/api/staff/congressistas/cruzamento", {
          metodo: "POST",
          corpo: { emails },
          autenticado: true,
        }),
      );
    } catch {
      setErro("Não foi possível fazer o cruzamento.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="flex flex-col gap-3">
      <p className="font-semibold">Cruzar com a lista de inscritos</p>
      <p className="text-sm text-muted-foreground">
        Cole os e-mails da lista de compradores de ingresso (a coluna inteira da planilha serve). O portal mostra quem se
        cadastrou com um e-mail que não está na lista. Contas do staff não aparecem.
      </p>
      <textarea
        value={texto}
        rows={5}
        onChange={(e) => setTexto(e.target.value)}
        placeholder="fulano@exemplo.com"
        className="w-full rounded-lg border border-input bg-transparent px-2.5 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
      />
      <Button className="h-10" disabled={enviando || emails.length === 0} onClick={cruzar}>
        {enviando ? "Cruzando..." : `Cruzar com ${contar(emails.length, "e-mail", "e-mails")}`}
      </Button>
      {erro && <p className="text-sm text-destructive">{erro}</p>}
      {fora !== null && (
        <p className="text-sm font-medium">
          {fora.length === 0
            ? "Todos os cadastros estão na lista."
            : `${contar(fora.length, "cadastro não está", "cadastros não estão")} na lista. Confira antes de remover: a pessoa pode ter usado outro e-mail.`}
        </p>
      )}
      {fora?.map((p) => (
        <CartaoPessoa
          key={p.id}
          pessoa={p}
          missoes={missoes}
          aoRemover={() => setFora((lista) => (lista ?? []).filter((x) => x.id !== p.id))}
        />
      ))}
    </section>
  );
}

type Confirmacao = "senha" | "remover" | null;

function CartaoPessoa({
  pessoa,
  missoes,
  aoRemover,
}: {
  pessoa: Pessoa;
  missoes: MissaoMarcavel[];
  aoRemover: () => void;
}) {
  const [confirmando, setConfirmando] = useState<Confirmacao>(null);
  const [senha, setSenha] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function executar() {
    setEnviando(true);
    setErro(null);
    try {
      if (confirmando === "senha") {
        const resposta = await api<{ senha: string }>(`/api/staff/congressistas/${pessoa.id}/senha`, {
          metodo: "POST",
          autenticado: true,
        });
        setSenha(resposta.senha);
      } else if (confirmando === "remover") {
        await api(`/api/staff/congressistas/${pessoa.id}`, { metodo: "DELETE", autenticado: true });
        aoRemover();
      }
      setConfirmando(null);
    } catch (e) {
      setErro(
        e instanceof ErroApi && e.status === 409
          ? "Não removido: esta pessoa já participou de missões ou é capitã de uma tripulação."
          : e instanceof ErroApi && e.status === 400
            ? "Você não pode remover a própria conta."
            : "Não foi possível concluir. Tente de novo.",
      );
    } finally {
      setEnviando(false);
    }
  }

  return (
    <article className="flex flex-col gap-2 rounded-lg border p-3">
      <div>
        <p className="font-medium">
          {pessoa.nome}
          {pessoa.staff && " · staff"}
        </p>
        <p className="text-xs break-all text-muted-foreground">{pessoa.email}</p>
        <p className="text-xs text-muted-foreground">
          {pessoa.ejNome} · {rotuloDe(CARGOS, pessoa.cargo)}
          {pessoa.tripulacaoNome && ` · tripulação ${pessoa.tripulacaoNome}`}
        </p>
      </div>

      {senha && (
        <p className="rounded-lg bg-muted p-2 text-sm">
          Senha temporária: <strong className="font-mono text-base">{senha}</strong>
          <br />
          Passe para a pessoa agora: ela não aparece de novo.
        </p>
      )}

      {!pessoa.staff && missoes.length > 0 && <RegistrarIndividual pessoa={pessoa} missoes={missoes} />}

      {confirmando === null ? (
        <div className="grid grid-cols-2 gap-2">
          <Button variant="outline" className="h-10" onClick={() => setConfirmando("senha")}>
            Nova senha
          </Button>
          <Button variant="outline" className="h-10" onClick={() => setConfirmando("remover")}>
            Remover
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          <p className="text-sm font-medium">
            {confirmando === "senha"
              ? "Gerar uma senha nova? A senha atual deixa de funcionar."
              : "Remover este cadastro? Não dá para desfazer."}
          </p>
          <div className="grid grid-cols-2 gap-2">
            <Button className="h-10" disabled={enviando} onClick={executar}>
              Confirmar
            </Button>
            <Button variant="outline" className="h-10" disabled={enviando} onClick={() => setConfirmando(null)}>
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

// Registro de uma missão individual cumprida por esta pessoa (forma "staff marca").
function RegistrarIndividual({ pessoa, missoes }: { pessoa: Pessoa; missoes: MissaoMarcavel[] }) {
  const [missaoId, setMissaoId] = useState("");
  const [salvando, setSalvando] = useState(false);
  const [aviso, setAviso] = useState<{ tipo: "ok" | "erro"; texto: string } | null>(null);

  async function registrar() {
    setSalvando(true);
    setAviso(null);
    const missao = missoes.find((m) => m.id === Number(missaoId));
    try {
      await api(`/api/staff/missoes/${missaoId}/marcar`, {
        metodo: "POST",
        corpo: { congressistaId: pessoa.id },
        autenticado: true,
      });
      setAviso({ tipo: "ok", texto: `Missão registrada: +${missao?.pontos ?? 0} pontos para ${pessoa.nome}.` });
      setMissaoId("");
    } catch (e) {
      setAviso({
        tipo: "erro",
        texto:
          e instanceof ErroApi && e.status === 409
            ? "Não registrado: a pessoa já cumpriu esta missão, tem envio na fila, ou as vagas acabaram."
            : "Não foi possível registrar. Tente de novo.",
      });
    } finally {
      setSalvando(false);
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={`individual-${pessoa.id}`} className="text-sm font-medium">
        Registrar missão individual cumprida
      </label>
      <div className="flex gap-2">
        <Selecao id={`individual-${pessoa.id}`} value={missaoId} onChange={(e) => setMissaoId(e.target.value)}>
          <option value="" disabled>
            Escolha a missão
          </option>
          {missoes.map((missao) => (
            <option key={missao.id} value={missao.id}>
              {missao.titulo} ({missao.pontos} pts)
            </option>
          ))}
        </Selecao>
        <Button className="h-10" disabled={salvando || missaoId === ""} onClick={registrar}>
          Registrar
        </Button>
      </div>
      {aviso && (
        <p role="status" className={aviso.tipo === "ok" ? "text-sm" : "text-sm text-destructive"}>
          {aviso.texto}
        </p>
      )}
    </div>
  );
}
