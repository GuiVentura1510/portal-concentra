import { useState } from "react";
import { cn } from "cn";

import { urlDaFoto } from "@/lib/foto";

// Iniciais do primeiro e do último nome, para quem não tem foto.
function iniciais(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return "?";
  const primeira = partes[0][0];
  const ultima = partes.length > 1 ? partes[partes.length - 1][0] : "";
  return (primeira + ultima).toUpperCase();
}

// Foto de perfil redonda. Sem foto (ou se a imagem não carregar), mostra as
// iniciais. O tamanho vem de fora, por "className" (o padrão é 2,5rem).
export function Avatar({
  nome,
  fotoChave,
  className,
}: {
  nome: string;
  // Pode faltar (undefined) se a API ainda for de uma versão sem fotos, como nos
  // minutos de uma publicação em que o site sobe antes da API.
  fotoChave?: string | null;
  className?: string;
}) {
  // Guarda a chave que falhou, e não um "sim/não": quando a pessoa troca a foto,
  // a chave muda e a imagem nova é tentada de novo.
  const [chaveComFalha, setChaveComFalha] = useState<string | null>(null);
  const mostrarFoto = !!fotoChave && fotoChave !== chaveComFalha;

  return (
    <span
      // O nome está sempre escrito ao lado, então o leitor de tela pula o avatar.
      aria-hidden="true"
      className={cn(
        "inline-flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-full bg-muted text-sm font-medium text-muted-foreground select-none",
        className,
      )}
    >
      {mostrarFoto && fotoChave ? (
        <img
          src={urlDaFoto(fotoChave)}
          alt=""
          // Só baixa as fotos que chegam perto da tela (o ranking tem até 100 linhas).
          loading="lazy"
          decoding="async"
          className="size-full object-cover"
          onError={() => setChaveComFalha(fotoChave)}
        />
      ) : (
        iniciais(nome)
      )}
    </span>
  );
}
