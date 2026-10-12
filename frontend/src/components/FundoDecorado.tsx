import type { ReactNode } from "react";

import estrelaCircundante from "@/assets/ilustracoes/estrelas/estrela-circundante.webp";
import estrelaGrande from "@/assets/ilustracoes/estrelas/estrela-grande-2.webp";
import estrelasCadentes from "@/assets/ilustracoes/estrelas/estrelas-cadentes-1.webp";
import constelacao from "@/assets/ilustracoes/estrelas/constelacao-2.webp";
import viajanteRoxo from "@/assets/ilustracoes/pessoas/pessoa-08.webp";
import viajanteRosa from "@/assets/ilustracoes/pessoas/pessoa-09.webp";

// Moldura das telas de login e cadastro: estrelas e viajantes nos cantos, atrás
// do conteúdo. A camada de enfeites é "fixed", então fica parada enquanto o
// formulário rola (o cadastro é longo). Os tamanhos usam vw com limite em px:
// no celular as figuras ficam pequenas, nos cantos, sem cobrir o cartão.
export function FundoDecorado({ children }: { children: ReactNode }) {
  return (
    <div className="relative min-h-svh overflow-x-clip">
      <div aria-hidden="true" className="pointer-events-none fixed inset-0 overflow-hidden select-none">
        <img src={estrelaGrande} alt="" className="absolute -top-[4vw] -left-[6vw] w-[26vw] max-w-56 -rotate-12" />
        <img src={estrelasCadentes} alt="" className="absolute -top-2 right-[2vw] w-[14vw] max-w-28" />
        <img
          src={estrelaCircundante}
          alt=""
          className="absolute top-[34%] -left-[4vw] w-[22vw] max-w-52 opacity-80 motion-safe:animate-cintilar"
        />
        <img src={constelacao} alt="" className="absolute top-[48%] -right-[8vw] w-[30vw] max-w-72 -rotate-[35deg] opacity-80" />
        <img
          src={viajanteRosa}
          alt=""
          className="absolute -bottom-[3vw] -left-[4vw] w-[24vw] max-w-52 motion-safe:animate-flutuar"
        />
        <img
          src={viajanteRoxo}
          alt=""
          className="absolute -right-[3vw] -bottom-[4vw] w-[22vw] max-w-48 motion-safe:animate-flutuar motion-safe:[animation-delay:-3.5s]"
        />
      </div>
      {/* "relative" põe o conteúdo acima da camada de enfeites. */}
      <div className="relative">{children}</div>
    </div>
  );
}
