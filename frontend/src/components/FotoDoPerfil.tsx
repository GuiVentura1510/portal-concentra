import { useRef, useState, type ChangeEvent } from "react";

import { Avatar } from "@/components/Avatar";
import { Button } from "@/components/ui/button";
import { api, ErroApi } from "@/lib/api";
import { reduzirFoto } from "@/lib/foto";

// Foto de perfil na aba Perfil: mostra a foto atual (ou as iniciais) e deixa a
// pessoa escolher, trocar ou remover a dela. A foto é reduzida no próprio
// celular antes do envio (ver lib/foto.ts).
export function FotoDoPerfil({
  nome,
  fotoChave,
  aoMudar,
  aviso,
}: {
  nome: string;
  fotoChave: string | null;
  // Recebe a chave nova, ou null quando a foto é removida.
  aoMudar: (fotoChave: string | null) => void;
  // Frase curta dizendo onde a foto aparece.
  aviso?: string;
}) {
  const campoArquivo = useRef<HTMLInputElement>(null);
  const [ocupado, setOcupado] = useState<"enviando" | "removendo" | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  async function escolher(evento: ChangeEvent<HTMLInputElement>) {
    const arquivo = evento.target.files?.[0];
    // Limpa o campo: escolher o mesmo arquivo de novo precisa disparar o evento.
    evento.target.value = "";
    if (!arquivo) return;

    setErro(null);
    setOcupado("enviando");
    try {
      let imagem: string;
      try {
        imagem = await reduzirFoto(arquivo);
      } catch {
        setErro("Não foi possível abrir essa imagem. Tente outra foto.");
        return;
      }
      const resposta = await api<{ fotoChave: string }>("/api/perfil/foto", {
        metodo: "PUT",
        corpo: { imagem },
        autenticado: true,
      });
      aoMudar(resposta.fotoChave);
    } catch (e) {
      setErro(
        e instanceof ErroApi && e.status === 400
          ? "A foto não foi aceita. Tente outra imagem."
          : e instanceof ErroApi && e.status === 0
            ? "Sem conexão com o servidor. Confira a internet e tente de novo."
            : "Não foi possível enviar a foto. Tente de novo.",
      );
    } finally {
      setOcupado(null);
    }
  }

  async function remover() {
    setErro(null);
    setOcupado("removendo");
    try {
      await api("/api/perfil/foto", { metodo: "DELETE", autenticado: true });
      aoMudar(null);
    } catch {
      setErro("Não foi possível remover a foto. Tente de novo.");
    } finally {
      setOcupado(null);
    }
  }

  return (
    <section className="flex flex-col items-center gap-3 text-center">
      <Avatar nome={nome} fotoChave={fotoChave} className="size-24 text-3xl" />
      <div className="flex w-full max-w-xs flex-col gap-2">
        <Button
          variant="outline"
          className="h-10 w-full"
          disabled={ocupado !== null}
          onClick={() => campoArquivo.current?.click()}
        >
          {ocupado === "enviando" ? "Enviando..." : fotoChave ? "Trocar foto" : "Adicionar foto"}
        </Button>
        {fotoChave && (
          <Button variant="ghost" className="h-10 w-full" disabled={ocupado !== null} onClick={remover}>
            {ocupado === "removendo" ? "Removendo..." : "Remover foto"}
          </Button>
        )}
      </div>
      {/* Campo de arquivo escondido: quem abre a galeria ou a câmera é o botão acima. */}
      <input
        ref={campoArquivo}
        type="file"
        accept="image/*"
        className="hidden"
        aria-label="Escolher foto de perfil"
        onChange={escolher}
      />
      {aviso && <p className="text-xs text-muted-foreground">{aviso}</p>}
      {erro && (
        <p role="alert" className="text-sm text-destructive">
          {erro}
        </p>
      )}
    </section>
  );
}
