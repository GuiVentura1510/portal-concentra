import { urlDaComprovacao } from "@/lib/foto";

// Miniatura da foto de um envio de missão; tocar abre a foto inteira numa aba nova.
// Usada na aba Envios do staff e na missão de quem enviou.
export function FotoDoEnvio({ chave }: { chave: string }) {
  const url = urlDaComprovacao(chave);
  return (
    <a href={url} target="_blank" rel="noreferrer" className="block w-fit">
      <img
        src={url}
        alt="Foto enviada como comprovação"
        loading="lazy"
        className="max-h-48 max-w-full rounded-lg border object-contain"
      />
    </a>
  );
}
