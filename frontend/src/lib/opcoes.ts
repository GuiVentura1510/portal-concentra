// Opções das perguntas do cadastro: o código que vai para a API
// e o texto que aparece na tela.
export type Opcao = { valor: string; rotulo: string };

export const CARGOS: Opcao[] = [
  { valor: "TRAINEE", rotulo: "Trainee" },
  { valor: "ASSESSOR_CONSULTOR", rotulo: "Assessor(a) ou consultor(a)" },
  { valor: "PROJETISTA", rotulo: "Projetista" },
  { valor: "GERENTE_COORDENADOR", rotulo: "Gerente ou coordenador(a)" },
  { valor: "DIRETOR", rotulo: "Diretor(a)" },
  { valor: "PRESIDENTE", rotulo: "Presidente" },
  { valor: "POS_JUNIOR", rotulo: "Pós-júnior" },
];

// Trainee ainda não atua numa área da EJ: não responde "área de atuação"
// (a API grava a área vazia; regra no AreaPorCargo do backend).
export function cargoTemArea(cargo: string): boolean {
  return cargo !== "TRAINEE";
}

export const TEMPOS_MEJ: Opcao[] = [
  { valor: "MENOS_DE_6_MESES", rotulo: "Menos de 6 meses" },
  { valor: "DE_6_MESES_A_1_ANO", rotulo: "De 6 meses a 1 ano" },
  { valor: "DE_1_A_2_ANOS", rotulo: "De 1 a 2 anos" },
  { valor: "MAIS_DE_2_ANOS", rotulo: "Mais de 2 anos" },
];

export const AREAS_ATUACAO: Opcao[] = [
  { valor: "PRESIDENCIA", rotulo: "Presidência" },
  { valor: "PROJETOS", rotulo: "Projetos" },
  { valor: "COMERCIAL", rotulo: "Comercial/Negócios" },
  { valor: "MARKETING", rotulo: "Marketing" },
  { valor: "OPERACOES", rotulo: "Operações (Gestão de Pessoas e Financeiro)" },
];

// "1 pessoa", "2 pessoas": escolhe a forma certa da palavra pelo número.
export function contar(quantidade: number, singular: string, plural: string): string {
  return `${quantidade} ${quantidade === 1 ? singular : plural}`;
}

// Texto de um código, para mostrar no perfil.
export function rotuloDe(opcoes: Opcao[], valor: string): string {
  return opcoes.find((opcao) => opcao.valor === valor)?.rotulo ?? valor;
}
