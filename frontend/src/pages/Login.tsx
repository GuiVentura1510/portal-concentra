import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router";

import { Campo } from "@/components/Campo";
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
    <main className="flex min-h-svh items-start justify-center p-4 sm:items-center">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Entrar</CardTitle>
          <CardDescription>A Expedição da Estrela Polar</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={enviar} className="flex flex-col gap-4">
            {aviso && <p className="text-sm">{aviso}</p>}
            <Campo rotulo="E-mail" id="email">
              <Input id="email" name="email" type="email" required autoComplete="email" className="h-10" />
            </Campo>
            <Campo rotulo="Senha" id="senha">
              <Input
                id="senha"
                name="senha"
                type={mostrarSenha ? "text" : "password"}
                required
                maxLength={72}
                autoComplete="current-password"
                className="h-10"
              />
            </Campo>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={mostrarSenha} onChange={(e) => setMostrarSenha(e.target.checked)} />
              Mostrar senha
            </label>

            {erro && (
              <p role="alert" className="text-sm text-destructive">
                {erro}
              </p>
            )}
            <Button type="submit" disabled={enviando} className="h-10 w-full">
              {enviando ? "Entrando..." : "Entrar"}
            </Button>
            <p className="text-center text-sm">
              Ainda não tem conta?{" "}
              <Link to="/cadastro" className="underline">
                Cadastre-se
              </Link>
            </p>
            <p className="text-center text-xs text-muted-foreground">
              Esqueceu a senha? Procure a organização do evento.
            </p>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}

function mensagemDeErro(e: unknown): string {
  if (e instanceof ErroApi) {
    if (e.status === 401 || e.status === 400) return "E-mail ou senha incorretos.";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Algo deu errado. Tente de novo em instantes.";
}
