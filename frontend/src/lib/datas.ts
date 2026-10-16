// Horários do evento. A API guarda e devolve em UTC; o site mostra e recebe
// no horário de Brasília, que não tem horário de verão desde 2019 (UTC-3 fixo).
const FUSO = "America/Sao_Paulo";

// Valor do campo datetime-local ("2026-10-17T10:30", horário de Brasília)
// para o formato que a API espera ("2026-10-17T10:30:00-03:00").
export function campoParaApi(valor: string): string | null {
  return valor ? `${valor}:00-03:00` : null;
}

// Horário da API para o valor do campo datetime-local, em Brasília.
export function apiParaCampo(iso: string | null): string {
  if (!iso) return "";
  const texto = new Intl.DateTimeFormat("sv-SE", {
    timeZone: FUSO,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(new Date(iso));
  return texto.replace(" ", "T");
}

// Horário legível, em Brasília. Ex.: "sáb., 17/10, 10:30".
export function formatarHorario(iso: string): string {
  return new Intl.DateTimeFormat("pt-BR", {
    timeZone: FUSO,
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

// Data curta, em Brasília. Ex.: "17/10/26".
export function formatarData(iso: string): string {
  return new Intl.DateTimeFormat("pt-BR", {
    timeZone: FUSO,
    day: "2-digit",
    month: "2-digit",
    year: "2-digit",
  }).format(new Date(iso));
}

// Janela de uma missão em texto. Ex.: "De sáb., 17/10, 10:30 até sáb., 17/10, 11:20".
export function textoJanela(inicio: string | null, fim: string | null): string {
  if (inicio && fim) return `De ${formatarHorario(inicio)} até ${formatarHorario(fim)}`;
  if (inicio) return `A partir de ${formatarHorario(inicio)}`;
  if (fim) return `Até ${formatarHorario(fim)}`;
  return "Sem horário definido";
}
