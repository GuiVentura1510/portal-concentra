import { ArrowRight } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router";

import { HistoriaDaExpedicao, PilaresDaExpedicao, RegrasDaExpedicao } from "@/components/Expedicao";
import { Logo } from "@/components/Logo";
import { MinhaTripulacao } from "@/components/MinhaTripulacao";
import { Button } from "@/components/ui/button";
import { api, ErroApi } from "@/lib/api";
import { BOTAO_EXPEDICAO } from "@/lib/estilos";
import type { Perfil } from "@/lib/perfil";
import { lerToken } from "@/lib/sessao";
import Staff from "@/pages/Staff";

type Estado = "carregando" | "sem-login" | "logado" | "falha";

// Início de quem está logado (o visitante vê a capa, ver App.tsx).
// - Congressista: a página da expedição, com a saudação, o quadro da
//   tripulação, a história, os pilares e as regras. As missões têm aba própria.
// - Staff: o próprio painel do staff.
export default function Inicio() {
  const [estado, setEstado] = useState<Estado>(() => (lerToken() ? "carregando" : "sem-login"));
  const [perfil, setPerfil] = useState<Perfil | null>(null);

  useEffect(() => {
    if (!lerToken()) return;
    api<Perfil>("/api/perfil", { autenticado: true })
      .then((dados) => {
        setPerfil(dados);
        setEstado("logado");
      })
      .catch((e) => {
        // 401: o token venceu e já foi apagado; o App troca o Início pela capa.
        setEstado(e instanceof ErroApi && e.status === 401 ? "sem-login" : "falha");
      });
  }, []);

  // Conta do staff: o início já é o painel. O staff não entra em tripulação
  // nem cumpre missões, então a página do congressista não teria o que mostrar.
  if (estado === "logado" && perfil?.staff) return <Staff />;
  if (estado === "sem-login") return null;

  return (
    <main className="mx-auto flex w-full max-w-md flex-col gap-8 px-5 pt-[max(1.5rem,env(safe-area-inset-top))] pb-8">
      <header className="flex flex-col gap-5">
        <Logo />
        <h1 className="text-center leading-none">
          <span className="block text-2xl font-extrabold text-creme">
            {perfil ? `Olá, ${perfil.nome.split(" ")[0]}!` : "Olá, viajante!"}
          </span>
          <span className="mt-2 block text-base font-semibold text-creme/80">
            Bem-vindo à Expedição da Estrela Polar
          </span>
        </h1>
      </header>

      {estado === "carregando" && <p className="text-center text-sm text-muted-foreground">Carregando...</p>}

      {estado === "falha" && (
        <div className="flex flex-col gap-3 text-center">
          <p className="text-sm">Não foi possível carregar seus dados.</p>
          <Button className="h-10 w-full" onClick={() => window.location.reload()}>
            Tentar de novo
          </Button>
        </div>
      )}

      {estado === "logado" && perfil && (
        <>
          {/* Cartão branco: o quadro da tripulação usa as cores de superfície clara. */}
          <div data-slot="card" className="flex flex-col gap-4 rounded-[2rem] bg-white p-4 text-[#1d2252]">
            <MinhaTripulacao meuId={perfil.id} minhaFotoChave={perfil.fotoChave} />
            <Link to="/missoes" className={`flex h-12 items-center justify-center gap-2 ${BOTAO_EXPEDICAO}`}>
              Ver as missões
              <ArrowRight className="size-5" aria-hidden="true" />
            </Link>
          </div>

          <HistoriaDaExpedicao />
          <PilaresDaExpedicao />
          <RegrasDaExpedicao />
        </>
      )}
    </main>
  );
}
