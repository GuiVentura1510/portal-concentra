import estrela from "@/assets/ilustracoes/estrelas/estrela-grande-2.webp";
import viajanteLaranja from "@/assets/ilustracoes/pessoas/pessoa-03.webp";
import viajanteRosa from "@/assets/ilustracoes/pessoas/pessoa-10.webp";

// Ilustração da tela de boas-vindas: dois viajantes ligados por uma trajetória
// tracejada até a Estrela Polar.
//
// Como a cena se adapta ao celular: ela ocupa a altura que sobra na tela, que
// muda de aparelho para aparelho. Por isso tudo é posicionado em porcentagem
// da caixa da cena, e a trajetória é um SVG com as mesmas coordenadas (0 a 100),
// esticado para a caixa inteira. Assim a linha sempre passa pelos viajantes,
// seja a cena mais alta ou mais baixa. Cada figura tem a altura em porcentagem
// da cena e uma largura máxima em porcentagem da largura; "object-contain"
// mantém a proporção do desenho quando a largura máxima é que manda.
export function CenaExpedicao({ viajantes }: { viajantes: number }) {
  return (
    <div
      // "aria-hidden": a cena é decorativa; o leitor de tela pula direto
      // para o texto e os botões.
      aria-hidden="true"
      className="relative min-h-44 w-full flex-1 select-none"
    >
      <svg
        className="absolute inset-0 size-full overflow-visible"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
      >
        {/* "non-scaling-stroke": a espessura e os traços não esticam junto
            com a cena. */}
        <path
          d="M 19 100 C 18 90, 22 78, 34 74 C 42 70, 55 70, 62 69 C 74 66, 86 56, 89 42 C 91 31, 89 21, 79 12"
          fill="none"
          stroke="#dfe7f5"
          strokeOpacity="0.85"
          strokeWidth="2"
          strokeDasharray="10 8"
          strokeLinecap="round"
          vectorEffect="non-scaling-stroke"
        />
      </svg>

      {/* Brilho atrás da Estrela Polar. */}
      <div className="absolute top-[11%] left-[77%] aspect-square h-[40%] -translate-x-1/2 -translate-y-1/2 rounded-full bg-[radial-gradient(circle,rgb(247_242_226/0.3),transparent_62%)]" />
      <img
        src={estrela}
        alt=""
        className="absolute top-[11%] left-[77%] h-[24%] max-w-[26%] -translate-x-1/2 -translate-y-1/2 object-contain"
      />

      <p className="font-manuscrita absolute top-[30%] left-[50%] -translate-x-1/2 -translate-y-1/2 -rotate-3 text-xl leading-tight whitespace-nowrap text-rosa">
        {viajantes} viajantes,
        <br />
        <span className="pl-8">um só norte</span>
      </p>

      <Brilho className="top-[33%] left-[85%] size-4 text-[#5b8cff]" />
      <Brilho className="top-[52%] left-[11%] size-5 text-white" />
      <Brilho className="top-[55%] left-[53%] size-4 text-white" />
      <Brilho className="top-[95%] left-[63%] size-3 text-white" />

      {/* O viajante laranja passa da borda direita de propósito, como no
          mockup. A tela corta o que sobra (overflow-x-clip na página). */}
      <img
        src={viajanteLaranja}
        alt=""
        className="absolute top-[47%] left-[90%] h-[19%] max-w-[44%] -translate-x-1/2 -translate-y-1/2 rotate-[28deg] object-contain motion-safe:animate-flutuar"
      />
      <img
        src={viajanteRosa}
        alt=""
        className="absolute top-[74%] left-[32%] h-[26%] max-w-[40%] -translate-x-1/2 -translate-y-1/2 object-contain motion-safe:animate-flutuar motion-safe:[animation-delay:-3.5s]"
      />
    </div>
  );
}

// Estrelinha de quatro pontas que pisca devagar.
function Brilho({ className }: { className: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      className={`absolute -translate-x-1/2 -translate-y-1/2 drop-shadow-[0_0_4px_currentColor] motion-safe:animate-cintilar ${className}`}
    >
      <path d="M12 0 C13 8 16 11 24 12 C16 13 13 16 12 24 C11 16 8 13 0 12 C8 11 11 8 12 0 Z" fill="currentColor" />
    </svg>
  );
}
