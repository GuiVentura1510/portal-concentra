import { useEffect, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router";

import { Campo } from "@/components/Campo";
import { Selecao } from "@/components/Selecao";
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
import { AREAS_ATUACAO, CARGOS, TEMPOS_MEJ, type Opcao } from "@/lib/opcoes";
import { salvarToken } from "@/lib/sessao";

type Ej = { id: number; nome: string };
type RespostaLogin = { token: string; nome: string };

// Formulário de cadastro. Depois de criar a conta, já faz o login,
// para a pessoa não precisar digitar a senha de novo.
export default function Cadastro() {
  const navegar = useNavigate();
  const [ejs, setEjs] = useState<Ej[]>([]);
  const [falhaEjs, setFalhaEjs] = useState(false);
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    api<Ej[]>("/api/ejs")
      .then(setEjs)
      .catch(() => setFalhaEjs(true));
  }, []);

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const dados = new FormData(evento.currentTarget);
    const email = String(dados.get("email") ?? "").trim();
    const senha = String(dados.get("senha") ?? "");

    setErro(null);
    setEnviando(true);
    let contaCriada = false;
    try {
      await api("/api/cadastro", {
        metodo: "POST",
        corpo: {
          nome: String(dados.get("nome") ?? "").trim(),
          email,
          senha,
          ejId: Number(dados.get("ejId")),
          cargo: dados.get("cargo"),
          tempoMej: dados.get("tempoMej"),
          areaAtuacao: dados.get("areaAtuacao"),
        },
      });
      contaCriada = true;
      const login = await api<RespostaLogin>("/api/login", {
        metodo: "POST",
        corpo: { email, senha },
      });
      salvarToken(login.token);
      navegar("/", { replace: true });
    } catch (e) {
      if (contaCriada) {
        // A conta existe, só o login automático falhou: manda para a tela de login.
        navegar("/login", {
          replace: true,
          state: { aviso: "Cadastro feito! Agora entre com seu e-mail e senha." },
        });
        return;
      }
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  const textoEj = falhaEjs
    ? "Não foi possível carregar as EJs"
    : ejs.length === 0
      ? "Carregando..."
      : "Escolha sua EJ";

  return (
    <main className="flex min-h-svh items-start justify-center p-4 sm:items-center">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Cadastro na Expedição</CardTitle>
          <CardDescription>Use o mesmo e-mail da compra do ingresso.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={enviar} className="flex flex-col gap-4">
            <Campo rotulo="Nome completo" id="nome">
              <Input id="nome" name="nome" required maxLength={120} autoComplete="name" className="h-10" />
            </Campo>
            <Campo rotulo="E-mail" id="email">
              <Input id="email" name="email" type="email" required maxLength={254} autoComplete="email" className="h-10" />
            </Campo>
            <Campo rotulo="Senha (mínimo de 8 caracteres)" id="senha">
              <Input
                id="senha"
                name="senha"
                type={mostrarSenha ? "text" : "password"}
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                className="h-10"
              />
            </Campo>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={mostrarSenha} onChange={(e) => setMostrarSenha(e.target.checked)} />
              Mostrar senha
            </label>
            <Campo rotulo="Empresa júnior" id="ejId">
              <Selecao id="ejId" name="ejId" required defaultValue="" disabled={ejs.length === 0}>
                <option value="" disabled>
                  {textoEj}
                </option>
                {ejs.map((ej) => (
                  <option key={ej.id} value={ej.id}>
                    {ej.nome}
                  </option>
                ))}
              </Selecao>
              <p className="text-xs text-muted-foreground">Pós-júnior: escolha a EJ em que você esteve.</p>
            </Campo>
            <Pergunta rotulo="Cargo na EJ" nome="cargo" opcoes={CARGOS} />
            <Pergunta rotulo="Tempo de MEJ" nome="tempoMej" opcoes={TEMPOS_MEJ} />
            <Pergunta rotulo="Área de atuação" nome="areaAtuacao" opcoes={AREAS_ATUACAO} />

            {erro && (
              <p role="alert" className="text-sm text-destructive">
                {erro}
              </p>
            )}
            <Button type="submit" disabled={enviando} className="h-10 w-full">
              {enviando ? "Enviando..." : "Criar conta"}
            </Button>
            <p className="text-center text-sm">
              Já tem conta?{" "}
              <Link to="/login" className="underline">
                Entrar
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}

// Uma pergunta de múltipla escolha do cadastro, todas obrigatórias.
function Pergunta({ rotulo, nome, opcoes }: { rotulo: string; nome: string; opcoes: Opcao[] }) {
  return (
    <Campo rotulo={rotulo} id={nome}>
      <Selecao id={nome} name={nome} required defaultValue="">
        <option value="" disabled>
          Escolha uma opção
        </option>
        {opcoes.map((opcao) => (
          <option key={opcao.valor} value={opcao.valor}>
            {opcao.rotulo}
          </option>
        ))}
      </Selecao>
    </Campo>
  );
}

function mensagemDeErro(e: unknown): string {
  if (e instanceof ErroApi) {
    if (e.status === 409) return "Este e-mail já tem cadastro. Use a opção Entrar.";
    if (e.status === 400) return "Algum dado não foi aceito. Confira os campos e tente de novo.";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Algo deu errado. Tente de novo em instantes.";
}
