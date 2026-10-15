import { urlDaComprovacao } from "@/lib/foto";

// Miniatura da foto de um envio de missão; tocar abre a foto inteira numa aba nova.
// Usada na aba Envios do staff e na missão de quem enviou.
export function FotoDoEnvio({ chave, pequena = false }: { chave: string; pequena?: boolean }) {
  const url = urlDaComprovacao(chave);
  return (
    <a href={url} target="_blank" rel="noreferrer" className={pequena ? "block" : "block w-fit"}>
      <img
        src={url}
        alt="Foto enviada como comprovação"
        loading="lazy"
        // Pequena: quadrada, na grade de várias fotos. Tocar abre a foto inteira.
        className={
          pequena
            ? "aspect-square w-full rounded-lg border object-cover"
            : "max-h-48 max-w-full rounded-lg border object-contain"
        }
      />
    </a>
  );
}

// Todas as fotos de um envio (desde a V11, até 5). "fotoChaves" vem da API nova;
// "fotoChave" (só a primeira) cobre a API antiga durante uma publicação.
export function FotosDoEnvio({ fotoChaves, fotoChave }: { fotoChaves?: string[] | null; fotoChave?: string | null }) {
  const chaves = fotoChaves && fotoChaves.length > 0 ? fotoChaves : fotoChave ? [fotoChave] : [];
  if (chaves.length === 0) return null;
  if (chaves.length === 1) return <FotoDoEnvio chave={chaves[0]} />;
  return (
    <div className="grid grid-cols-3 gap-2">
      {chaves.map((chave) => (
        <FotoDoEnvio key={chave} chave={chave} pequena />
      ))}
    </div>
  );
}
