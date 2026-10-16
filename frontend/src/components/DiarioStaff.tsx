import { useCallback, useEffect, useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { api, ErroApi } from "@/lib/api";
import { formatarHorario } from "@/lib/datas";
import { urlDoDiario } from "@/lib/foto";

type FotoDiario = {
  id: number;
  chave: string;
  autorNome: string;
  tripulacaoNome: string;
  legenda: string | null;
  criadaEm: string;
};

function buscarFotos() {
  return api<FotoDiario[]>("/api/staff/diario", { autenticado: true });
}

// Aba Diário do staff: as últimas fotos que os integrantes anexaram ao Diário de
// Bordo das tripulações, da mais nova para a mais antiga. Não valem pontos; o staff
// só tira as que não forem adequadas.
export function DiarioStaff() {
  const [fotos, setFotos] = useState<FotoDiario[] | null>(null);
  const [falha, setFalha] = useState(false);
  const [busca, setBusca] = useState("");

  const atualizar = useCallback(() => {
    setFalha(false);
    buscarFotos().then(setFotos, () => setFalha(true));
  }, []);

  useEffect(() => {
    buscarFotos().then(setFotos, () => setFalha(true));
  }, []);

  function remover(id: number) {
    setFotos((lista) => (lista ?? []).filter((f) => f.id !== id));
  }

  const termo = busca.trim().toLowerCase();
  const visiveis =
    fotos?.filter(
      (f) => termo === "" || [f.tripulacaoNome, f.autorNome].some((t) => t.toLowerCase().includes(termo)),
    ) ?? null;

  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        Fotos que os congressistas anexaram ao Diário de Bordo da tripulação. Não valem pontos. Tire aqui as que não
        forem adequadas.
      </p>
      <div className="grid grid-cols-[1fr_auto] gap-2">
        <Input
          value={busca}
          onChange={(e) => setBusca(e.target.value)}
          placeholder="Buscar tripulação ou pessoa"
          className="h-10"
        />
        <Button variant="outline" className="h-10" onClick={atualizar}>
          Atualizar
        </Button>
      </div>
      {falha && <p className="text-sm">Não foi possível carregar as fotos.</p>}
      {fotos === null && !falha && <p className="text-sm">Carregando...</p>}
      {fotos?.length === 0 && <p className="text-sm">Nenhuma foto anexada ainda.</p>}
      {fotos !== null && fotos.length > 0 && visiveis?.length === 0 && (
        <p className="text-sm">Nenhuma foto encontrada para essa busca.</p>
      )}
      {visiveis?.map((f) => <ItemFoto key={f.id} foto={f} aoTirar={() => remover(f.id)} />)}
    </div>
  );
}

function ItemFoto({ foto, aoTirar }: { foto: FotoDiario; aoTirar: () => void }) {
  // Primeiro toque pergunta; o segundo confirma.
  const [confirmando, setConfirmando] = useState(false);
  const [tirando, setTirando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const url = urlDoDiario(foto.chave);

  async function tirar() {
    setTirando(true);
    setErro(null);
    try {
      await api(`/api/staff/diario/${foto.id}`, { metodo: "DELETE", autenticado: true });
      aoTirar();
    } catch (e) {
      // 404: quem anexou (ou outra pessoa do staff) já tirou a foto.
      if (e instanceof ErroApi && e.status === 404) {
        aoTirar();
        return;
      }
      setErro("Não foi possível tirar a foto. Tente de novo.");
    } finally {
      setTirando(false);
    }
  }

  return (
    <article className="grid grid-cols-[6rem_1fr] gap-3 rounded-lg border p-3">
      <a href={url} target="_blank" rel="noreferrer" className="block">
        <img
          src={url}
          alt={`Foto anexada por ${foto.autorNome}`}
          loading="lazy"
          className="aspect-square w-full rounded-lg border object-cover"
        />
      </a>
      <div className="flex min-w-0 flex-col gap-2">
        <div>
          <p className="font-semibold break-words">{foto.tripulacaoNome}</p>
          <p className="text-xs break-words text-muted-foreground">
            {foto.autorNome} · {formatarHorario(foto.criadaEm)}
          </p>
          {/* A legenda é escrita pela pessoa: o staff também confere o texto. */}
          {foto.legenda && <p className="mt-1 text-sm break-words">“{foto.legenda}”</p>}
        </div>
        {!confirmando ? (
          <Button variant="outline" className="h-10" onClick={() => setConfirmando(true)}>
            Tirar do diário
          </Button>
        ) : (
          <div className="grid grid-cols-2 gap-2">
            <Button variant="destructive" className="h-10" disabled={tirando} onClick={tirar}>
              {tirando ? "Tirando..." : "Confirmar"}
            </Button>
            <Button variant="outline" className="h-10" disabled={tirando} onClick={() => setConfirmando(false)}>
              Voltar
            </Button>
          </div>
        )}
        {erro && (
          <p role="alert" className="text-sm text-destructive">
            {erro}
          </p>
        )}
      </div>
    </article>
  );
}
