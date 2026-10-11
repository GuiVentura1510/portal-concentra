// Dados de quem está logado, como a API devolve em GET /api/perfil.
// Ficam aqui porque o início e a aba Perfil usam o mesmo formato.
export type Perfil = {
  id: number;
  nome: string;
  email: string;
  ejId: number;
  ejNome: string;
  cargo: string;
  tempoMej: string;
  areaAtuacao: string;
  staff: boolean;
};
