import { ImagePlus, X } from "lucide-react";
import type { ReactNode } from "react";
import { useEffect, useRef, useState } from "react";
import { Navigate } from "react-router";

import fitaAzul from "@/assets/ilustracoes/fitas/fita-azul.svg";
import fitaCreme from "@/assets/ilustracoes/fitas/fita-creme.svg";
import fitaLaranja from "@/assets/ilustracoes/fitas/fita-laranja.svg";
import fitaRosaCurta from "@/assets/ilustracoes/fitas/fita-rosa-curta.svg";
import fitaRosaEstreita from "@/assets/ilustracoes/fitas/fita-rosa-estreita.svg";
import fitaRoxa from "@/assets/ilustracoes/fitas/fita-roxa.svg";
import fitaVermelha from "@/assets/ilustracoes/fitas/fita-vermelha.svg";
import viajanteLaranja from "@/assets/ilustracoes/pessoas/pessoa-03.webp";
import viajanteRosa from "@/assets/ilustracoes/pessoas/pessoa-10.webp";
import { Avatar } from "@/components/Avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, ErroApi } from "@/lib/api";
import { formatarData } from "@/lib/datas";
import { reduzirFotoDeEnvio, urlDoDiario } from "@/lib/foto";
import { contar } from "@/lib/opcoes";
import type { Perfil } from "@/lib/perfil";
import { lerToken } from "@/lib/sessao";

type Membro = {
  id: number;
  nome: string;
  ejNome: string;
  fotoChave: string | null;
};
type DadosTripulacao = {
  id: number;
  nome: string;
  capitaoId: number | null;
  membros: Membro[];
};
type PosicaoTripulacao = { tripulacaoId: number; pontos: number };
// Recordação que um integrante anexou ao Diário de Bordo, direto do celular.
type FotoDiario = { id: number; chave: string; autorId: number; legenda: string | null; criadaEm: string };
type Diario = { limite: number; fotos: FotoDiario[] };
// Foto escolhida no celular, já reduzida, esperando a legenda antes do envio.
type Rascunho = { id: number; imagem: string; legenda: string };

// Legenda que cabe numa linha do rodapé da polaroide (mesmo limite da API).
const MAX_LEGENDA = 30;

type Estado =
  | { tipo: "carregando" }
  | { tipo: "sem-login" }
  | { tipo: "falha" }
  | { tipo: "staff" }
  | { tipo: "aguardando" }
  | {
      tipo: "pronta";
      meuId: number;
      minhaFotoChave: string | null;
      tripulacao: DadosTripulacao;
      // Pontos e diário podem faltar se a chamada falhar: a página continua
      // mostrando a tripulação, que é o principal.
      pontos: number | null;
      diario: Diario | null;
    };

// Títulos das seções ("Integrantes", "Diário de bordo").
const ROTULO_SECAO = "text-sm leading-none font-bold text-[#e7e8eb]/80 uppercase";

// Aba Tripulação, no visual do Figma (Concentra 26 › Tripulação · Diário de Bordo):
// pontos e nome da tripulação, os integrantes e o Diário de Bordo, com as fotos que
// os integrantes anexam do celular como recordação, em polaroides presas por fita.
// O ponto de encontro e o capitão continuam no Início e na aba Perfil. Os pontos
// individuais não aparecem aqui (ficam no ranking individual), para não virar
// disputa interna.
export default function Tripulacao() {
  const [estado, setEstado] = useState<Estado>(() => (lerToken() ? { tipo: "carregando" } : { tipo: "sem-login" }));

  useEffect(() => {
    if (!lerToken()) return;
    api<Perfil>("/api/perfil", { autenticado: true })
      .then(async (perfil) => {
        if (perfil.staff) {
          setEstado({ tipo: "staff" });
          return;
        }
        let tripulacao: DadosTripulacao;
        try {
          tripulacao = await api<DadosTripulacao>("/api/tripulacao", { autenticado: true });
        } catch (e) {
          // 404 até a montagem, em 15/10.
          setEstado(e instanceof ErroApi && e.status === 404 ? { tipo: "aguardando" } : { tipo: "falha" });
          return;
        }
        const [ranking, diario] = await Promise.allSettled([
          api<PosicaoTripulacao[]>("/api/ranking", { autenticado: true }),
          api<Diario>("/api/tripulacao/diario", { autenticado: true }),
        ]);
        setEstado({
          tipo: "pronta",
          meuId: perfil.id,
          minhaFotoChave: perfil.fotoChave,
          tripulacao,
          // O ranking das tripulações lista todas, inclusive as que ainda têm 0 ponto.
          pontos:
            ranking.status === "fulfilled"
              ? (ranking.value.find((p) => p.tripulacaoId === tripulacao.id)?.pontos ?? null)
              : null,
          diario: diario.status === "fulfilled" ? diario.value : null,
        });
      })
      .catch((e) => {
        setEstado(e instanceof ErroApi && e.status === 401 ? { tipo: "sem-login" } : { tipo: "falha" });
      });
  }, []);

  if (estado.tipo === "sem-login") return <Navigate to="/login" replace />;

  return (
    // "overflow-x-clip": os viajantes e as fitas passam da borda de propósito, como no Figma.
    <main className="relative mx-auto flex w-full max-w-md flex-col overflow-x-clip px-8 pt-[max(2.5rem,env(safe-area-inset-top))] pb-12">
      <header className="flex flex-col">
        {estado.tipo === "pronta" && estado.pontos !== null && (
          <p className="text-right font-titulo text-[22px] leading-none font-bold tracking-[0.08em] text-[#e7e8eb]/85 uppercase">
            {estado.pontos} <span aria-hidden="true">pts</span>
            <span className="sr-only">pontos</span>
          </p>
        )}
        <p className="mt-5 text-[11px] leading-none font-bold tracking-[0.08em] text-ouro uppercase">Tripulação</p>
        <h1 className="mt-1 font-titulo text-4xl leading-[1.1] font-black tracking-[0.08em] break-words text-creme italic">
          {estado.tipo === "pronta" ? estado.tripulacao.nome : "Sua tripulação"}
        </h1>
      </header>

      {estado.tipo === "carregando" && <p className="mt-8 text-center text-sm text-muted-foreground">Carregando...</p>}

      {estado.tipo === "falha" && (
        <Cartao>
          <p className="text-center text-sm">Não foi possível carregar sua tripulação.</p>
          <Button className="h-10 w-full" onClick={() => window.location.reload()}>
            Tentar de novo
          </Button>
        </Cartao>
      )}

      {estado.tipo === "staff" && (
        <Cartao>
          <p className="text-center text-sm">
            Contas do staff não entram em tripulação. As tripulações ficam no painel, na aba Início.
          </p>
        </Cartao>
      )}

      {estado.tipo === "aguardando" && (
        <Cartao>
          <p className="text-center text-sm">
            Sua tripulação será revelada no dia <strong>15 de outubro</strong>.
          </p>
        </Cartao>
      )}

      {estado.tipo === "pronta" && (
        <>
          <Integrantes meuId={estado.meuId} minhaFotoChave={estado.minhaFotoChave} tripulacao={estado.tripulacao} />
          <DiarioDeBordo meuId={estado.meuId} diarioInicial={estado.diario} />
        </>
      )}
    </main>
  );
}

// Primeiro e último nome, como no desenho ("Nome Sobrenome"): cabe na coluna estreita.
function nomeCurto(nome: string): string {
  const partes = nome.trim().split(/\s+/);
  return partes.length > 1 ? `${partes[0]} ${partes[partes.length - 1]}` : nome.trim();
}

// Cartão branco: o data-slot faz valerem as cores de superfície clara do index.css.
function Cartao({ children, className = "mt-8" }: { children: ReactNode; className?: string }) {
  return (
    <div data-slot="card" className={`flex flex-col gap-4 rounded-[2rem] bg-white p-5 text-[#1d2252] ${className}`}>
      {children}
    </div>
  );
}

function Integrantes({
  meuId,
  minhaFotoChave,
  tripulacao,
}: {
  meuId: number;
  minhaFotoChave: string | null;
  tripulacao: DadosTripulacao;
}) {
  // Capitão primeiro; depois quem está logado; depois os outros, por nome.
  const membros = [...tripulacao.membros].sort((a, b) => {
    const peso = (m: Membro) => (m.id === tripulacao.capitaoId ? 0 : m.id === meuId ? 1 : 2);
    return peso(a) - peso(b) || a.nome.localeCompare(b.nome, "pt-BR");
  });

  return (
    <section aria-labelledby="titulo-integrantes" className="mt-7">
      <h2 id="titulo-integrantes" className={ROTULO_SECAO}>
        Integrantes
      </h2>
      <div className="relative mt-5">
        {/* Viajante rosa à esquerda da segunda fileira, que tem só 2 pessoas
            numa tripulação de 5. Fica atrás das fotos quando a fileira é cheia. */}
        <img
          src={viajanteRosa}
          alt=""
          aria-hidden="true"
          className="pointer-events-none absolute bottom-[67px] -left-[47px] w-[91px] max-w-none select-none motion-safe:animate-flutuar"
        />
        {/* Três por fileira; a última, incompleta, fica centralizada. */}
        <ul className="relative -mx-2 flex flex-wrap justify-center gap-5 text-center font-bold uppercase">
          {membros.map((membro) => (
            <li key={membro.id} className="flex w-[calc((100%-2.5rem)/3)] flex-col items-center gap-5">
              <Avatar
                nome={membro.nome}
                fotoChave={membro.id === meuId ? minhaFotoChave : membro.fotoChave}
                className="size-22 text-2xl"
              />
              <div className="w-full leading-tight">
                <p className="text-[10px] break-words text-white">{nomeCurto(membro.nome)}</p>
                <p className="text-[8px] break-words text-white/70">{membro.ejNome}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

// Pose de cada polaroide, tirada do Figma e repetida a cada 6 fotos: o giro (com
// a leve inclinação do desenho), um pequeno desvio do centro da coluna, para as
// fotos não ficarem alinhadas demais, e as fitas que a prendem. Cada fita é
// posicionada pelo centro, em px a partir do centro da polaroide.
type Pose = { desvio: string; giro: string; fitas: { src: string; lugar: string }[] };
const POSES: Pose[] = [
  {
    desvio: "-translate-x-[4px] translate-y-[1px]",
    giro: "rotate-[-6.47deg] skew-x-[-2.02deg]",
    fitas: [{ src: fitaLaranja, lugar: "top-[calc(50%-34px)] left-[calc(50%-60px)] rotate-[-7.06deg]" }],
  },
  {
    desvio: "-translate-y-[1px]",
    giro: "rotate-[9.17deg] skew-x-[-2.02deg]",
    fitas: [{ src: fitaRoxa, lugar: "top-[calc(50%-55px)] left-[calc(50%+8px)] rotate-[-143deg]" }],
  },
  {
    desvio: "-translate-x-[3px] translate-y-[6px]",
    giro: "rotate-[-3.88deg] skew-x-[-2.02deg]",
    fitas: [
      { src: fitaRosaCurta, lugar: "top-[calc(50%-55px)] left-[calc(50%+58px)] rotate-[58.67deg]" },
      { src: fitaRosaEstreita, lugar: "top-[calc(50%+52px)] left-[calc(50%-63px)] rotate-[-99.02deg]" },
    ],
  },
  {
    desvio: "-translate-x-[7px] -translate-y-[6px]",
    giro: "rotate-[3.17deg] skew-x-[-2.02deg]",
    fitas: [{ src: fitaVermelha, lugar: "top-[calc(50%-43px)] left-[calc(50%+63px)] rotate-[68.61deg]" }],
  },
  {
    desvio: "translate-x-[8px] translate-y-[5px]",
    // As duas últimas são espelhadas no Figma: a inclinação vai para o outro lado.
    giro: "rotate-[7.61deg] skew-x-[2.02deg]",
    fitas: [{ src: fitaCreme, lugar: "top-[calc(50%-50px)] left-[calc(50%-58px)] -scale-y-100 rotate-[111.39deg]" }],
  },
  {
    desvio: "translate-x-[10px] -translate-y-[5px]",
    giro: "rotate-[-2.83deg] skew-x-[2.02deg]",
    fitas: [{ src: fitaAzul, lugar: "top-[calc(50%-54px)] left-[calc(50%-8px)] -scale-y-100 rotate-[132.11deg]" }],
  },
];

// Diário de Bordo: só as recordações que a tripulação anexa do celular, na ordem
// em que foram anexadas, cada uma com a legenda que a pessoa escreveu. Não têm
// relação com as missões nem valem pontos (as fotos enviadas como prova das
// missões ficam na aba Missões).
function DiarioDeBordo({ meuId, diarioInicial }: { meuId: number; diarioInicial: Diario | null }) {
  const [fotos, setFotos] = useState<FotoDiario[]>(diarioInicial?.fotos ?? []);
  const [rascunhos, setRascunhos] = useState<Rascunho[]>([]);
  const [preparando, setPreparando] = useState(false);
  // Durante o envio: qual foto está subindo, de quantas.
  const [enviando, setEnviando] = useState<{ atual: number; total: number } | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const proximoRascunho = useRef(0);

  const limite = diarioInicial?.limite ?? 0;
  // As fotos esperando a legenda já ocupam lugar no diário.
  const restantes = Math.max(0, limite - fotos.length - rascunhos.length);

  // Aceita várias de uma vez (galeria). Cada foto é reduzida no celular e fica
  // esperando a legenda, até o limite do diário.
  async function escolher(arquivos: FileList | null) {
    if (!arquivos || arquivos.length === 0) return;
    const escolhidos = Array.from(arquivos).slice(0, restantes);
    setErro(
      arquivos.length > escolhidos.length ? `Cabem só mais ${contar(restantes, "foto", "fotos")} no diário.` : null,
    );
    setPreparando(true);
    try {
      for (const arquivo of escolhidos) {
        const imagem = await reduzirFotoDeEnvio(arquivo);
        const id = proximoRascunho.current++;
        setRascunhos((atuais) => [...atuais, { id, imagem, legenda: "" }]);
      }
    } catch {
      setErro("Não foi possível usar uma das fotos. Tente outra.");
    } finally {
      setPreparando(false);
    }
  }

  // Envia as fotos novas uma por uma. Se uma falhar, as que faltam continuam no
  // cartão, com as legendas, para tentar de novo.
  async function anexar() {
    setErro(null);
    const fila = rascunhos;
    try {
      for (let i = 0; i < fila.length; i++) {
        setEnviando({ atual: i + 1, total: fila.length });
        const foto = await api<FotoDiario>("/api/tripulacao/diario", {
          metodo: "POST",
          corpo: { imagem: fila[i].imagem, legenda: fila[i].legenda.trim() },
          autenticado: true,
        });
        setFotos((atuais) => [...atuais, foto]);
        setRascunhos((atuais) => atuais.filter((r) => r.id !== fila[i].id));
      }
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(null);
    }
  }

  async function tirar(id: number) {
    setErro(null);
    try {
      await api(`/api/tripulacao/diario/${id}`, { metodo: "DELETE", autenticado: true });
      setFotos((atuais) => atuais.filter((f) => f.id !== id));
    } catch (e) {
      // 404: o staff já tinha tirado a foto.
      if (e instanceof ErroApi && e.status === 404) {
        setFotos((atuais) => atuais.filter((f) => f.id !== id));
      } else {
        setErro("Não foi possível tirar a foto. Tente de novo.");
      }
    }
  }

  return (
    <section aria-labelledby="titulo-diario" className="relative mt-10">
      <h2 id="titulo-diario" className={ROTULO_SECAO}>
        Diário de bordo
      </h2>
      {/* Viajante laranja entrando pela borda direita, acima das polaroides. */}
      <img
        src={viajanteLaranja}
        alt=""
        aria-hidden="true"
        className="pointer-events-none absolute -top-[34px] -right-[42px] z-10 w-[105px] max-w-none rotate-[10.17deg] select-none motion-safe:animate-flutuar motion-safe:[animation-delay:-3.5s]"
      />

      {diarioInicial === null ? (
        <p className="mt-4 text-sm">Não foi possível carregar o diário de bordo.</p>
      ) : (
        <>
          {fotos.length === 0 && rascunhos.length === 0 && (
            <p className="mt-4 text-sm text-creme/80">
              Anexe fotos do celular para guardar as recordações da tripulação.
            </p>
          )}
          <ul className="mt-6 grid grid-cols-2 justify-items-center gap-x-5 gap-y-[51px]">
            {fotos.map((foto, i) => (
              <Polaroide
                key={foto.id}
                foto={foto}
                pose={POSES[i % POSES.length]}
                aoTirar={foto.autorId === meuId ? () => tirar(foto.id) : null}
              />
            ))}
            {restantes > 0 && (
              <PolaroideEmBranco
                pose={POSES[fotos.length % POSES.length]}
                ocupado={preparando || enviando !== null}
                preparando={preparando}
                aoEscolher={escolher}
              />
            )}
          </ul>

          {rascunhos.length > 0 && (
            <Cartao className="mt-12">
              <p className="font-bold">
                {contar(rascunhos.length, "foto nova", "fotos novas")} para o diário
              </p>
              <ul className="flex flex-col gap-4">
                {rascunhos.map((rascunho, i) => (
                  <li key={rascunho.id} className="grid grid-cols-[4rem_1fr_auto] items-end gap-3">
                    <img
                      src={`data:image/jpeg;base64,${rascunho.imagem}`}
                      alt={`Foto nova ${i + 1}`}
                      className="size-16 rounded-lg border object-cover"
                    />
                    <div className="min-w-0">
                      <Label htmlFor={`legenda-${rascunho.id}`} className="text-xs">
                        Legenda (opcional)
                      </Label>
                      <Input
                        id={`legenda-${rascunho.id}`}
                        value={rascunho.legenda}
                        maxLength={MAX_LEGENDA}
                        placeholder="Ex.: Primeiro dia da expedição"
                        disabled={enviando !== null}
                        onChange={(e) =>
                          setRascunhos((atuais) =>
                            atuais.map((r) => (r.id === rascunho.id ? { ...r, legenda: e.target.value } : r)),
                          )
                        }
                        className="mt-1 h-11 text-base"
                      />
                    </div>
                    <button
                      type="button"
                      aria-label={`Não anexar a foto nova ${i + 1}`}
                      disabled={enviando !== null}
                      onClick={() => setRascunhos((atuais) => atuais.filter((r) => r.id !== rascunho.id))}
                      className="flex size-11 items-center justify-center rounded-full text-muted-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                    >
                      <X className="size-5" aria-hidden="true" />
                    </button>
                  </li>
                ))}
              </ul>
              <Button className="h-11" disabled={enviando !== null || preparando} onClick={anexar}>
                {enviando ? `Anexando ${enviando.atual} de ${enviando.total}...` : "Anexar ao diário"}
              </Button>
            </Cartao>
          )}

          {erro && (
            <p role="alert" className="mt-6 text-center text-sm text-destructive">
              {erro}
            </p>
          )}
          <p className="mt-8 text-center text-xs text-creme/70">
            {fotos.length >= limite
              ? `O diário chegou a ${limite} fotos. Tire uma das suas para anexar outra.`
              : `${contar(fotos.length, "foto", "fotos")} de ${limite}. São recordações: não valem pontos.`}
          </p>
        </>
      )}
    </section>
  );
}

function mensagemDeErro(e: unknown): string {
  if (e instanceof ErroApi) {
    if (e.status === 409) return "O diário chegou ao limite de fotos.";
    if (e.status === 400) return "A foto não foi aceita. Tente outra imagem.";
    if (e.status === 0) return "Sem conexão com o servidor. Confira a internet e tente de novo.";
  }
  return "Não foi possível anexar a foto. Tente de novo.";
}

// Foto numa moldura de polaroide, com a legenda e a data no rodapé.
// Tocar abre a foto inteira numa aba nova.
function Polaroide({
  foto,
  pose,
  aoTirar,
}: {
  foto: FotoDiario;
  pose: Pose;
  aoTirar: (() => Promise<void>) | null;
}) {
  const url = urlDoDiario(foto.chave);
  return (
    <li className={`relative h-[110px] w-[141px] ${pose.desvio}`}>
      <div className={`relative size-full ${pose.giro}`}>
        <a
          href={url}
          target="_blank"
          rel="noreferrer"
          className="flex size-full flex-col bg-white px-[5.6px] pt-[6.6px] outline-none focus-visible:ring-4 focus-visible:ring-ceu/60"
        >
          <img
            src={url}
            alt={foto.legenda ? `Foto do diário: ${foto.legenda}` : "Foto do diário de bordo"}
            loading="lazy"
            decoding="async"
            // O cinza do Figma aparece enquanto a foto carrega.
            className="h-[81px] w-full shrink-0 bg-[#c3c3c3] object-cover"
          />
          {/* Legenda em 7px e data em 6px (no Figma, 4px), em cinza sem transparência,
              para ler. A legenda fica como a pessoa escreveu: em maiúsculas, 30 letras
              não cabem. */}
          <span className="flex min-w-0 flex-1 flex-col items-end justify-center gap-[3px] leading-none font-bold text-[#5a6190]">
            {foto.legenda && <span className="max-w-full truncate text-[7px]">{foto.legenda}</span>}
            <span className="text-[6px]">{formatarData(foto.criadaEm)}</span>
          </span>
        </a>
        {aoTirar && <BotaoTirar aoTirar={aoTirar} />}
      </div>
      {pose.fitas.map((fita) => (
        <img
          key={fita.src}
          src={fita.src}
          alt=""
          aria-hidden="true"
          className={`pointer-events-none absolute max-w-none -translate-x-1/2 -translate-y-1/2 select-none ${fita.lugar}`}
        />
      ))}
    </li>
  );
}

// "×" nas fotos que a pessoa anexou. O primeiro toque pergunta; o segundo tira.
function BotaoTirar({ aoTirar }: { aoTirar: () => Promise<void> }) {
  const [confirmando, setConfirmando] = useState(false);
  const [tirando, setTirando] = useState(false);
  return (
    <button
      type="button"
      disabled={tirando}
      aria-label={confirmando ? "Confirmar: tirar esta foto do diário" : "Tirar esta foto do diário"}
      onBlur={() => setConfirmando(false)}
      onClick={async () => {
        if (!confirmando) {
          setConfirmando(true);
          return;
        }
        setTirando(true);
        // Deu certo: a polaroide some. Deu errado: o botão volta ao "×" e o erro aparece embaixo.
        await aoTirar();
        setTirando(false);
        setConfirmando(false);
      }}
      className={
        "absolute top-2 right-2 flex h-7 min-w-7 items-center justify-center rounded-full text-white outline-none focus-visible:ring-4 focus-visible:ring-ceu/60 " +
        (confirmando ? "bg-[#c62828] px-2.5 text-xs font-bold" : "bg-noite/80")
      }
    >
      {confirmando ? (tirando ? "Tirando..." : "Tirar") : <X className="size-4" aria-hidden="true" />}
    </button>
  );
}

// Polaroide em branco no fim do diário: tocar abre a câmera ou a galeria do celular.
function PolaroideEmBranco({
  pose,
  ocupado,
  preparando,
  aoEscolher,
}: {
  pose: Pose;
  ocupado: boolean;
  preparando: boolean;
  aoEscolher: (arquivos: FileList | null) => void;
}) {
  return (
    <li className={`relative h-[110px] w-[141px] ${pose.desvio}`}>
      <label
        className={`flex size-full cursor-pointer flex-col bg-white px-[5.6px] pt-[6.6px] focus-within:ring-4 focus-within:ring-ceu/60 ${pose.giro}`}
      >
        <span className="flex h-[81px] w-full shrink-0 items-center justify-center border-2 border-dashed border-[#c3c3c3] bg-[#f1f2fb] text-[#5a6190]">
          {preparando ? (
            <span className="text-[10px] font-bold" role="status">
              Preparando...
            </span>
          ) : (
            <ImagePlus className="size-7" aria-hidden="true" />
          )}
        </span>
        <span className="flex flex-1 items-center justify-end text-[8px] leading-none font-bold text-[#5a6190] uppercase">
          Anexar fotos
        </span>
        <input
          type="file"
          accept="image/*"
          multiple
          className="sr-only"
          disabled={ocupado}
          onChange={(e) => {
            void aoEscolher(e.target.files);
            // Limpa o campo: escolher a mesma foto de novo precisa disparar o evento.
            e.target.value = "";
          }}
        />
      </label>
    </li>
  );
}
