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
  // null para trainee, que não escolhe área.
  areaAtuacao: string | null;
  staff: boolean;
  // Chave da foto de perfil (ver lib/foto.ts); null para quem não enviou foto.
  fotoChave: string | null;
  // Falso depois da montagem das tripulações: EJ e tempo de MEJ ficam travados.
  podeMudarEjETempo: boolean;
};
