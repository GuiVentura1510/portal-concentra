import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router";

import { Campo } from "@/components/Campo";
import { FundoDecorado } from "@/components/FundoDecorado";
import { Logo } from "@/components/Logo";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { BOTAO_EXPEDICAO, CAMPO, CARTAO_FORMULARIO, LINK_CARTAO } from "@/lib/estilos";
import { salvarToken } from "@/lib/sessao";

type RespostaLogin = { token: string; nome: string };

// Tela de login: troca e-mail e senha pelo token e volta para o início.
export default function Login() {
  const navegar = useNavigate();
  const local = useLocation();
  // Aviso enviado por outra tela, como o cadastro que não conseguiu entrar sozinho.
  const aviso = (local.state as { aviso?: string } | null)?.aviso;
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [verAjudaSenha, setVerAjudaSenha] = useState(false);

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const dados = new FormData(evento.currentTarget);
    setErro(null);
    setEnviando(true);
    try {
      const login = await api<RespostaLogin>("/api/login", {
        metodo: "POST",
        corpo: {
          email: String(dados.get("email") ?? "").trim(),
          senha: String(dados.get("senha") ?? ""),
        },
      });
      salvarToken(login.token);
      navegar("/", { replace: true });
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <FundoDecorado>
      {/* Centralizado na vertical também no celular; com o teclado aberto ou
          em tela baixa, o conteúdo rola normalmente. */}
      <main className="flex min-h-svh flex-col items-center justify-center gap-8 px-5 py-10">
        <Logo />
        {/* data-slot="card": usa as cores de superfície clara do index.css. */}
        <div data-slot="card" className={CARTAO_FORMULARIO}>
          <h1 className="text-center text-4xl font-extrabold tracking-wide text-noite">ENTRAR</h1>
          <form onSubmit={enviar} className="mt-6 flex flex-col gap-4">
            {aviso && <p className="text-sm">{aviso}</p>}
            <Campo rotulo="E-mail" id="email">
              <Input id="email" name="email" type="email" required autoComplete="email" className={CAMPO} />
            </Campo>
            <Campo rotulo="Senha" id="senha">
              <Input
                id="senha"
                name="senha"
                type={mostrarSenha ? "text" : "password"}
                required
                maxLength={72}
                autoComplete="current-password"
                className={CAMPO}
              />
            </Campo>
            <div className="-mt-1 flex items-center justify-between gap-2 text-sm">
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={mostrarSenha} onChange={(e) => setMostrarSenha(e.target.checked)} />
                Mostrar senha
              </label>
              {/* Não há troca de senha pelo site: o staff gera uma senha
                  temporária na aba Pessoas. O link só explica isso. */}
              <button
                type="button"
                className="text-[#2b7aa1] underline underline-offset-2"
                aria-expanded={verAjudaSenha}
                onClick={() => setVerAjudaSenha((v) => !v)}
              >
                Esqueci minha senha
              </button>
            </div>
            {verAjudaSenha && (
              <p className="rounded-xl bg-[#eef6fc] px-3 py-2 text-sm">
                Procure a organização do evento: o staff gera uma senha temporária para você entrar.
              </p>
            )}

            {erro && (
              <p role="alert" className="text-sm text-destructive">
                {erro}
              </p>
            )}
            <button type="submit" disabled={enviando} className={`mt-2 h-12 w-full text-lg disabled:opacity-60 ${BOTAO_EXPEDICAO}`}>
              {enviando ? "Entrando..." : "Entrar"}
            </button>
            <p className="text-center text-sm">
              Ainda não tem conta?{" "}
              <Link to="/cadastro" className={LINK_CARTAO}>
                Cadastre-se
              </Link>
            </p>
          </form>
        </div>
      </main>
    </FundoDecorado>
  );
}

function mensagemDeErro(e: unknown): string {
  if (e instanceof ErroApi) {
    if (e.status === 401 || e.status === 400) return "E-mail ou senha incorretos.";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Algo deu errado. Tente de novo em instantes.";
}
