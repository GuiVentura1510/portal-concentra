import { useEffect, useState, type FormEvent } from "react";

import { Campo } from "@/components/Campo";
import { Selecao } from "@/components/Selecao";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { AREAS_ATUACAO, CARGOS, TEMPOS_MEJ, rotuloDe, type Opcao } from "@/lib/opcoes";
import type { Perfil } from "@/lib/perfil";

type Ej = { id: number; nome: string };

// Formulário "Editar dados" da aba Perfil. Regras (decididas em 11/10):
// - nome, cargo e área mudam sempre;
// - EJ e tempo de MEJ só antes da montagem das tripulações (a API devolve
//   "podeMudarEjETempo" e recusa com 409 depois);
// - o e-mail não muda aqui: é o login e serve para conferir a inscrição.
export function EditarDados({
  perfil,
  aoSalvar,
  aoCancelar,
}: {
  perfil: Perfil;
  aoSalvar: (atualizado: Perfil) => void;
  aoCancelar: () => void;
}) {
  const [ejs, setEjs] = useState<Ej[]>([]);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const travado = !perfil.podeMudarEjETempo;

  useEffect(() => {
    // A lista de EJs só é necessária enquanto a EJ pode mudar.
    if (travado) return;
    api<Ej[]>("/api/ejs").then(setEjs, () => setEjs([]));
  }, [travado]);

  async function salvar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const dados = new FormData(evento.currentTarget);
    setErro(null);
    setEnviando(true);
    try {
      const atualizado = await api<Perfil>("/api/perfil", {
        metodo: "PUT",
        autenticado: true,
        corpo: {
          nome: String(dados.get("nome") ?? "").trim(),
          // Campos travados não vão no formulário: seguem os valores atuais.
          ejId: travado ? perfil.ejId : Number(dados.get("ejId")),
          tempoMej: travado ? perfil.tempoMej : String(dados.get("tempoMej")),
          cargo: String(dados.get("cargo")),
          areaAtuacao: String(dados.get("areaAtuacao")),
        },
      });
      aoSalvar(atualizado);
    } catch (e) {
      setErro(erroAoSalvar(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form onSubmit={salvar} className="flex flex-col gap-3">
      <Campo rotulo="Nome completo" id="nome">
        <Input id="nome" name="nome" required maxLength={120} defaultValue={perfil.nome} className="h-10" />
      </Campo>

      {travado ? (
        <p className="rounded-lg bg-muted px-3 py-2 text-sm">
          EJ ({perfil.ejNome}) e tempo de MEJ ({rotuloDe(TEMPOS_MEJ, perfil.tempoMej)}) foram usados na montagem das
          tripulações e não mudam mais.
        </p>
      ) : (
        <>
          <Campo rotulo="Empresa júnior" id="ejId">
            <Selecao id="ejId" name="ejId" required defaultValue={perfil.ejId} disabled={ejs.length === 0}>
              {/* Enquanto a lista carrega, mostra só a EJ atual. */}
              {ejs.length === 0 && <option value={perfil.ejId}>{perfil.ejNome}</option>}
              {ejs.map((ej) => (
                <option key={ej.id} value={ej.id}>
                  {ej.nome}
                </option>
              ))}
            </Selecao>
          </Campo>
          <Lista rotulo="Tempo de MEJ" nome="tempoMej" opcoes={TEMPOS_MEJ} valor={perfil.tempoMej} />
        </>
      )}

      <Lista rotulo="Cargo na EJ" nome="cargo" opcoes={CARGOS} valor={perfil.cargo} />
      <Lista rotulo="Área de atuação" nome="areaAtuacao" opcoes={AREAS_ATUACAO} valor={perfil.areaAtuacao} />
      <p className="text-xs text-muted-foreground">O e-mail não muda: ele é o seu login.</p>

      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
      <div className="grid grid-cols-2 gap-2">
        <Button type="button" variant="outline" className="h-10" onClick={aoCancelar} disabled={enviando}>
          Cancelar
        </Button>
        <Button type="submit" className="h-10" disabled={enviando}>
          {enviando ? "Salvando..." : "Salvar"}
        </Button>
      </div>
    </form>
  );
}

// Troca de senha pela própria pessoa, pedindo a senha atual. Útil para quem
// recebeu uma senha temporária do staff.
export function TrocarSenha() {
  const [aberto, setAberto] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [trocada, setTrocada] = useState(false);

  async function trocar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const formulario = evento.currentTarget;
    const dados = new FormData(formulario);
    const novaSenha = String(dados.get("novaSenha") ?? "");
    setErro(null);
    if (novaSenha !== String(dados.get("confirmacao") ?? "")) {
      setErro("A confirmação não é igual à nova senha.");
      return;
    }
    setEnviando(true);
    try {
      await api("/api/perfil/senha", {
        metodo: "PUT",
        autenticado: true,
        corpo: { senhaAtual: String(dados.get("senhaAtual") ?? ""), novaSenha },
      });
      formulario.reset();
      setAberto(false);
      setTrocada(true);
    } catch (e) {
      setErro(erroAoTrocar(e));
    } finally {
      setEnviando(false);
    }
  }

  if (!aberto) {
    return (
      <div className="flex flex-col gap-2">
        {trocada && <p className="text-sm">Senha trocada. Use a nova senha no próximo login.</p>}
        <Button
          variant="outline"
          className="h-10 w-full"
          onClick={() => {
            setTrocada(false);
            setAberto(true);
          }}
        >
          Trocar senha
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={trocar} className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold">Trocar senha</h2>
      <Campo rotulo="Senha atual" id="senhaAtual">
        <Input id="senhaAtual" name="senhaAtual" type="password" required autoComplete="current-password" className="h-10" />
      </Campo>
      <Campo rotulo="Nova senha (mínimo de 8 caracteres)" id="novaSenha">
        <Input
          id="novaSenha"
          name="novaSenha"
          type="password"
          required
          minLength={8}
          maxLength={72}
          autoComplete="new-password"
          className="h-10"
        />
      </Campo>
      <Campo rotulo="Repita a nova senha" id="confirmacao">
        <Input id="confirmacao" name="confirmacao" type="password" required autoComplete="new-password" className="h-10" />
      </Campo>
      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
      <div className="grid grid-cols-2 gap-2">
        <Button type="button" variant="outline" className="h-10" onClick={() => setAberto(false)} disabled={enviando}>
          Cancelar
        </Button>
        <Button type="submit" className="h-10" disabled={enviando}>
          {enviando ? "Trocando..." : "Trocar"}
        </Button>
      </div>
    </form>
  );
}

function Lista({ rotulo, nome, opcoes, valor }: { rotulo: string; nome: string; opcoes: Opcao[]; valor: string }) {
  return (
    <Campo rotulo={rotulo} id={nome}>
      <Selecao id={nome} name={nome} required defaultValue={valor}>
        {opcoes.map((opcao) => (
          <option key={opcao.valor} value={opcao.valor}>
            {opcao.rotulo}
          </option>
        ))}
      </Selecao>
    </Campo>
  );
}

function erroAoSalvar(e: unknown): string {
  if (e instanceof ErroApi) {
    if (e.status === 409) return "EJ e tempo de MEJ não mudam depois da montagem das tripulações.";
    if (e.status === 400) return "Algum dado não foi aceito. Confira os campos e tente de novo.";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Algo deu errado. Tente de novo em instantes.";
}

function erroAoTrocar(e: unknown): string {
  if (e instanceof ErroApi) {
    if (e.status === 400) return "Senha atual incorreta, ou nova senha fora do tamanho (8 a 72 caracteres).";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Algo deu errado. Tente de novo em instantes.";
}
