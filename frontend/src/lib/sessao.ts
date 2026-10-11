import { useSyncExternalStore } from "react";

// Guarda o token do login no navegador, para a pessoa continuar logada
// ao fechar e abrir o site. O token vale 10 dias.
const CHAVE = "portal-concentra.token";

// Reserva para navegadores que bloqueiam o armazenamento (aba anônima de
// alguns celulares): o login dura até a aba ser fechada.
let tokenEmMemoria: string | null = null;

// Componentes que precisam reagir quando a pessoa entra ou sai (a barra de abas).
const ouvintes = new Set<() => void>();

function avisarOuvintes(): void {
  ouvintes.forEach((ouvinte) => ouvinte());
}

function assinar(ouvinte: () => void): () => void {
  ouvintes.add(ouvinte);
  return () => {
    ouvintes.delete(ouvinte);
  };
}

export function lerToken(): string | null {
  try {
    return localStorage.getItem(CHAVE) ?? tokenEmMemoria;
  } catch {
    return tokenEmMemoria;
  }
}

export function salvarToken(token: string): void {
  tokenEmMemoria = token;
  try {
    localStorage.setItem(CHAVE, token);
  } catch {
    // Sem armazenamento: fica só a cópia em memória.
  }
  avisarOuvintes();
}

export function sair(): void {
  tokenEmMemoria = null;
  try {
    localStorage.removeItem(CHAVE);
  } catch {
    // Sem armazenamento: não há nada gravado para apagar.
  }
  avisarOuvintes();
}

// Diz se há alguém logado e redesenha o componente quando isso muda, inclusive
// quando a API responde 401 e a sessão é encerrada no meio do uso.
export function useLogado(): boolean {
  return useSyncExternalStore(assinar, () => lerToken() !== null);
}
