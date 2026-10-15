import {
  BookOpen,
  Crown,
  Gift,
  ListChecks,
  ShieldCheck,
  Trophy,
  Users,
  Zap,
  type LucideIcon,
} from "lucide-react";

import type { ReactNode } from "react";

import estrelaPolar from "@/assets/ilustracoes/estrelas/estrela-polar.webp";

// História e regras da Expedição da Estrela Polar, com base no guia
// "Experiência do Congressista". Aparecem no Início de quem está logado e na
// página pública /expedicao. O texto é fixo: para mudar, é só editar aqui.

// Números das regras, separados para facilitar a troca.
const MISSOES_INDIVIDUAIS_POR_DIA = 8;
const PREMIADAS_TRIPULACOES = 3;
const PREMIADOS_NO_INDIVIDUAL = 10;

const PILARES = [
  { nome: "A União Faz a Nação", texto: "A força é coletiva. Ninguém chega ao norte sozinho." },
  {
    nome: "Grandes Lideranças, Grandes Responsabilidades",
    texto: "Coragem para construir no caminho, mesmo sem ter todas as respostas.",
  },
  {
    nome: "Quando um Capítulo Termina, Outro Começa",
    texto: "Transição e legado. Quem chega aprende com quem já trilhou a rota.",
  },
];

const REGRAS: { titulo: string; texto: string; Icone: LucideIcon }[] = [
  {
    titulo: "Sua tripulação",
    texto:
      "De 5 a 6 pessoas de EJs diferentes, misturando quem está chegando com quem já tem estrada. Cada tripulação tem o nome de uma constelação e um ponto de encontro. A sua é revelada no sábado, 17/10, ao meio-dia; quem se cadastrar depois entra direto numa tripulação.",
    Icone: Users,
  },
  {
    titulo: "O capitão",
    texto:
      "A tripulação escolhe o capitão no evento, e o staff registra. Ele envia as respostas das missões da tripulação. Até ter capitão, qualquer membro pode enviar.",
    Icone: Crown,
  },
  {
    titulo: "As missões",
    texto: `Algumas são da tripulação, cumpridas em grupo. Outras são individuais: ${MISSOES_INDIVIDUAIS_POR_DIA} por dia. Cada uma tem horário e fica na aba Missões. A resposta vai pelo portal (texto, foto ou os dois, conforme a missão) ou é registrada por um Guardião da Rota.`,
    Icone: ListChecks,
  },
  {
    titulo: "Vale na hora",
    texto:
      "A resposta enviada já soma pontos. Os Guardiões conferem depois e podem retirar um envio que não for válido. Nesse caso, você vê o motivo e pode enviar de novo enquanto a missão estiver aberta.",
    Icone: Zap,
  },
  {
    titulo: "Pontos e rankings",
    texto:
      "As missões da tripulação somam para a tripulação, e as individuais somam só para você. São dois rankings, na aba Ranking: o das tripulações e o individual.",
    Icone: Trophy,
  },
  {
    titulo: "Passaporte e adesivos",
    texto:
      "O Passaporte do Navegador é o bloco de notas que você recebe no início do evento. Cada missão cumprida rende um adesivo, e juntos eles formam a Estrela Polar no verso do crachá.",
    Icone: BookOpen,
  },
  {
    titulo: "Guardiões da Rota",
    texto: "É o staff. Procure um deles para tirar dúvidas, carimbar o passaporte ou registrar uma missão.",
    Icone: ShieldCheck,
  },
  {
    titulo: "Prêmios",
    texto: `As ${PREMIADAS_TRIPULACOES} primeiras tripulações a completar todas as missões ganham um prêmio de reconhecimento na Magna Final (domingo, 17h15). Nas missões individuais, os ${PREMIADOS_NO_INDIVIDUAL} primeiros do ranking individual ganham um brinde.`,
    Icone: Gift,
  },
];

// Título das seções, no fundo azul-noite.
function TituloSecao({ children }: { children: ReactNode }) {
  return <h2 className="font-titulo text-center text-2xl font-bold text-creme italic">{children}</h2>;
}

export function HistoriaDaExpedicao() {
  return (
    <section aria-labelledby="titulo-historia" className="flex flex-col items-center gap-4 text-center">
      <img
        src={estrelaPolar}
        alt=""
        aria-hidden="true"
        className="h-24 w-auto select-none motion-safe:animate-cintilar"
      />
      <h2 id="titulo-historia" className="font-titulo text-2xl font-bold text-creme italic">
        A história
      </h2>
      <div className="flex flex-col gap-3 text-base leading-relaxed text-creme/90">
        <p>
          Enquanto as outras estrelas giram e o céu parece mudar, a Estrela Polar fica fixa. É ela que aponta o norte
          para quem navega.
        </p>
        <p>
          Somos 983 viajantes e, juntos, formamos a tripulação que mantém o rumo do MEJ DF. O Concentra 2026 não é um
          evento para assistir de longe: é uma expedição, e cada um de nós tem um papel nela.
        </p>
      </div>
      <p className="font-manuscrita text-2xl text-lilas">Onde se criam grandes histórias.</p>
    </section>
  );
}

export function PilaresDaExpedicao() {
  return (
    <section aria-labelledby="titulo-pilares" className="flex flex-col gap-3">
      <TituloSecao>
        <span id="titulo-pilares">Os pilares</span>
      </TituloSecao>
      <ol className="flex flex-col gap-2">
        {PILARES.map((pilar, i) => (
          <li
            key={pilar.nome}
            className="flex gap-3 rounded-2xl border border-lilas/25 bg-white/5 px-4 py-3"
          >
            <span className="font-titulo text-2xl leading-none font-bold text-ceu italic" aria-hidden="true">
              {i + 1}
            </span>
            <span>
              <span className="block font-bold text-creme">{pilar.nome}</span>
              <span className="block text-sm text-creme/85">{pilar.texto}</span>
            </span>
          </li>
        ))}
      </ol>
    </section>
  );
}

export function RegrasDaExpedicao() {
  return (
    <section aria-labelledby="titulo-regras" className="flex flex-col gap-3">
      <TituloSecao>
        <span id="titulo-regras">Como funciona a expedição</span>
      </TituloSecao>
      {/* data-slot="card": cores de superfície clara do index.css. */}
      <div data-slot="card" className="rounded-[2rem] border-2 border-[#b6a7e9] bg-white px-5 py-5 text-[#1d2252] shadow-[4px_4px_0_#a9b8ec]">
        <ul className="flex flex-col gap-4">
          {REGRAS.map(({ titulo, texto, Icone }) => (
            <li key={titulo} className="flex gap-3">
              <span className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-full bg-[#eceefd] text-[#2f5fd0]">
                <Icone className="size-5" aria-hidden="true" />
              </span>
              <span>
                <span className="block font-bold">{titulo}</span>
                <span className="block text-sm leading-relaxed text-[#40457a]">{texto}</span>
              </span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
