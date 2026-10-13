import { ArrowRight } from "lucide-react";
import { Link } from "react-router";

import { CenaExpedicao } from "@/components/CenaExpedicao";
import { Logo } from "@/components/Logo";
import { BOTAO_EXPEDICAO } from "@/lib/estilos";

// Números do cartão. São fixos por decisão da organização (não vêm da API):
// para mudar, é só trocar aqui.
const NUMEROS = [
  { valor: 983, rotulo: "Tripulantes" },
  { valor: 3, rotulo: "Missões" },
  { valor: 1, rotulo: "Magna final" },
];

// Tela de quem ainda não entrou: a capa da expedição, com o caminho para o
// login e para o cadastro. Quem está logado vê o Início (ver App.tsx).
export default function BoasVindas() {
  return (
    // "overflow-x-clip" corta o viajante que sai pela borda direita sem criar
    // rolagem lateral. A tela inteira cabe na altura do celular: a cena é a
    // parte que encolhe ou cresce (flex-1), o resto tem altura própria.
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col overflow-x-clip px-6 pt-[max(1.5rem,env(safe-area-inset-top))] pb-[max(1.5rem,env(safe-area-inset-bottom))]">
      <Logo />

      <h1 className="mt-6 leading-none">
        <span className="block text-[1.75rem] font-extrabold text-creme">A Expedição da</span>
        <span className="font-titulo mt-1 block bg-gradient-to-r from-white to-lilas bg-clip-text pb-1 text-5xl font-bold text-transparent italic">
          Estrela Polar
        </span>
      </h1>

      <CenaExpedicao viajantes={NUMEROS[0].valor} />

      <dl className="relative grid grid-cols-3 rounded-[2rem] border-2 border-[#b6a7e9] bg-white py-3 shadow-[4px_4px_0_#a9b8ec]">
        {NUMEROS.map(({ valor, rotulo }, i) => (
          <div
            key={rotulo}
            // Divisória entre as colunas, menos antes da primeira.
            className={`flex flex-col-reverse items-center gap-0.5 ${i > 0 ? "border-l-2 border-[#d5dbf5]" : ""}`}
          >
            <dt className="text-[0.6875rem] font-bold tracking-[0.12em] text-[#6a70a8] uppercase">{rotulo}</dt>
            <dd className="font-titulo text-xl font-bold text-[#1d2252] italic">{valor}</dd>
          </div>
        ))}
      </dl>

      <Link
        to="/login"
        className={`mt-5 flex h-14 items-center justify-center gap-3 text-lg ${BOTAO_EXPEDICAO}`}
      >
        Entrar na expedição
        <ArrowRight className="size-5" aria-hidden="true" />
      </Link>

      <Link
        to="/cadastro"
        className="mx-auto mt-5 rounded text-base font-semibold text-ceu underline underline-offset-4 outline-none focus-visible:ring-2 focus-visible:ring-ceu"
      >
        Criar conta de viajante
      </Link>

      <Link
        to="/expedicao"
        className="mx-auto mt-3 rounded text-sm font-semibold text-creme/85 underline underline-offset-4 outline-none focus-visible:ring-2 focus-visible:ring-ceu"
      >
        Conheça a expedição
      </Link>
    </main>
  );
}
