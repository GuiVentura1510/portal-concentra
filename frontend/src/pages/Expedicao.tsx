import { ArrowRight } from "lucide-react";
import { Link, Navigate } from "react-router";

import { HistoriaDaExpedicao, PilaresDaExpedicao, RegrasDaExpedicao } from "@/components/Expedicao";
import { Logo } from "@/components/Logo";
import { BOTAO_EXPEDICAO } from "@/lib/estilos";
import { useLogado } from "@/lib/sessao";

// Página pública da expedição (/expedicao), aberta pelo link da capa: a
// história e as regras para quem ainda não entrou. Quem está logado já vê
// tudo isso no Início.
export default function Expedicao() {
  if (useLogado()) return <Navigate to="/" replace />;

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col gap-8 px-5 pt-[max(1.5rem,env(safe-area-inset-top))] pb-[max(2rem,env(safe-area-inset-bottom))]">
      <header className="flex flex-col gap-5">
        <Link to="/" className="rounded outline-none focus-visible:ring-2 focus-visible:ring-ceu" aria-label="Voltar para a capa">
          <Logo />
        </Link>
        <h1 className="text-center leading-none">
          <span className="block text-[1.75rem] font-extrabold text-creme">A Expedição da</span>
          <span className="font-titulo mt-1 block bg-gradient-to-r from-white to-lilas bg-clip-text pb-1 text-5xl font-bold text-transparent italic">
            Estrela Polar
          </span>
        </h1>
      </header>

      <HistoriaDaExpedicao />
      <PilaresDaExpedicao />
      <RegrasDaExpedicao />

      <div className="flex flex-col items-center gap-5">
        <Link
          to="/cadastro"
          className={`flex h-14 w-full items-center justify-center gap-3 text-lg ${BOTAO_EXPEDICAO}`}
        >
          Criar conta de viajante
          <ArrowRight className="size-5" aria-hidden="true" />
        </Link>
        <Link
          to="/login"
          className="rounded text-base font-semibold text-ceu underline underline-offset-4 outline-none focus-visible:ring-2 focus-visible:ring-ceu"
        >
          Já tenho conta
        </Link>
      </div>
    </main>
  );
}
