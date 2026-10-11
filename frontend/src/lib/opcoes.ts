// Opções das perguntas do cadastro: o código que vai para a API
// e o texto que aparece na tela.
export type Opcao = { valor: string; rotulo: string };

export const CARGOS: Opcao[] = [
  { valor: "ASSESSOR_CONSULTOR", rotulo: "Assessor(a) ou consultor(a)" },
  { valor: "GERENTE_COORDENADOR", rotulo: "Gerente ou coordenador(a)" },
  { valor: "DIRETOR", rotulo: "Diretor(a)" },
  { valor: "PRESIDENTE", rotulo: "Presidente" },
  { valor: "POS_JUNIOR", rotulo: "Pós-júnior" },
];

export const TEMPOS_MEJ: Opcao[] = [
  { valor: "MENOS_DE_6_MESES", rotulo: "Menos de 6 meses" },
  { valor: "DE_6_MESES_A_1_ANO", rotulo: "De 6 meses a 1 ano" },
  { valor: "DE_1_A_2_ANOS", rotulo: "De 1 a 2 anos" },
  { valor: "MAIS_DE_2_ANOS", rotulo: "Mais de 2 anos" },
];

export const AREAS_ATUACAO: Opcao[] = [
  { valor: "PRESIDENCIA", rotulo: "Presidência" },
  { valor: "PROJETOS", rotulo: "Projetos" },
  { valor: "COMERCIAL", rotulo: "Comercial" },
  { valor: "MARKETING", rotulo: "Marketing" },
  { valor: "OPERACOES", rotulo: "Operações (Gestão de Pessoas e Financeiro)" },
];

// Texto de um código, para mostrar no perfil.
export function rotuloDe(opcoes: Opcao[], valor: string): string {
  return opcoes.find((opcao) => opcao.valor === valor)?.rotulo ?? valor;
}
