// Guarda o token do login no navegador, para a pessoa continuar logada
// ao fechar e abrir o site. O token vale 10 dias.
const CHAVE = "portal-concentra.token";

// Reserva para navegadores que bloqueiam o armazenamento (aba anônima de
// alguns celulares): o login dura até a aba ser fechada.
let tokenEmMemoria: string | null = null;

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
}

export function sair(): void {
  tokenEmMemoria = null;
  try {
    localStorage.removeItem(CHAVE);
  } catch {
    // Sem armazenamento: não há nada gravado para apagar.
  }
}
