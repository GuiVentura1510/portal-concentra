import { API_URL } from "@/lib/api";

// Lado da foto de perfil, em pixels. A maior exibição tem 80 px; 256 px sobram
// para telas de alta densidade e mantêm cada foto em torno de 20 KB.
const LADO = 256;
const PREFIXO = "data:image/jpeg;base64,";

// Endereço da imagem a partir da chave que a API devolve. A chave muda a cada
// foto nova, então o navegador pode guardar a imagem sem perguntar de novo.
export function urlDaFoto(chave: string): string {
  return `${API_URL}/api/fotos/${chave}`;
}

function carregar(url: string): Promise<HTMLImageElement> {
  return new Promise((resolver, rejeitar) => {
    const imagem = new Image();
    imagem.onload = () => resolver(imagem);
    imagem.onerror = () => rejeitar(new Error("O navegador não conseguiu abrir a imagem."));
    imagem.src = url;
  });
}

// Prepara a foto escolhida no celular para o envio: recorta o quadrado do meio,
// reduz para 256 px e converte para JPEG. Devolve o arquivo em base64, que é
// como a API recebe. Assim uma foto de câmera, com vários MB, vira uns 20 KB
// antes de sair do aparelho.
export async function reduzirFoto(arquivo: File): Promise<string> {
  const url = URL.createObjectURL(arquivo);
  try {
    const imagem = await carregar(url);
    const largura = imagem.naturalWidth;
    const altura = imagem.naturalHeight;
    const lado = Math.min(largura, altura);
    if (lado === 0) throw new Error("A imagem está vazia.");

    const tela = document.createElement("canvas");
    tela.width = LADO;
    tela.height = LADO;
    const contexto = tela.getContext("2d");
    if (!contexto) throw new Error("O navegador não permite preparar a imagem.");
    // Fundo branco: o JPEG não tem transparência, e sem isto um PNG vazado sairia preto.
    contexto.fillStyle = "#ffffff";
    contexto.fillRect(0, 0, LADO, LADO);
    contexto.imageSmoothingQuality = "high";
    contexto.drawImage(imagem, (largura - lado) / 2, (altura - lado) / 2, lado, lado, 0, 0, LADO, LADO);

    const dados = tela.toDataURL("image/jpeg", 0.85);
    if (!dados.startsWith(PREFIXO)) throw new Error("O navegador não gerou um JPEG.");
    return dados.slice(PREFIXO.length);
  } finally {
    URL.revokeObjectURL(url);
  }
}
