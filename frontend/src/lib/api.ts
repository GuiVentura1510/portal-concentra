import { lerToken, sair } from "@/lib/sessao";

// O endereço da API vem do ambiente para o mesmo código servir na máquina
// local e em produção (no Render, a variável VITE_API_URL).
const API_URL = import.meta.env.VITE_API_URL;

// Erro com o código HTTP da resposta, para cada tela escolher a mensagem.
// O código 0 significa que não deu para falar com o servidor.
export class ErroApi extends Error {
  readonly status: number;

  constructor(status: number, mensagem: string) {
    super(mensagem);
    this.status = status;
  }
}

type Opcoes = {
  metodo?: "GET" | "POST" | "PUT" | "DELETE";
  corpo?: unknown;
  // Só as rotas que exigem login levam o token. Nas rotas públicas ele fica
  // de fora: um token vencido faria a API recusar até o próprio login.
  autenticado?: boolean;
};

// Chama a API enviando e recebendo JSON.
export async function api<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const cabecalhos: Record<string, string> = {};
  if (opcoes.corpo !== undefined) {
    cabecalhos["Content-Type"] = "application/json";
  }
  const token = opcoes.autenticado ? lerToken() : null;
  if (token) {
    cabecalhos["Authorization"] = `Bearer ${token}`;
  }

  let resposta: Response;
  try {
    resposta = await fetch(`${API_URL}${caminho}`, {
      method: opcoes.metodo ?? "GET",
      headers: cabecalhos,
      body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
    });
  } catch {
    throw new ErroApi(0, "Sem conexão com o servidor.");
  }

  if (resposta.status === 401 && token) {
    // Token vencido ou inválido: a sessão acabou.
    sair();
  }
  if (!resposta.ok) {
    throw new ErroApi(resposta.status, `A API respondeu ${resposta.status}.`);
  }
  return (await resposta.json()) as T;
}
