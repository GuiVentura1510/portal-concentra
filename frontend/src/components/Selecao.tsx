import type { ComponentProps } from "react";

import { cn } from "@/lib/utils";

// Lista de opções nativa do navegador: no celular abre o seletor do próprio
// sistema, mais fácil de usar que um menu desenhado. O visual imita o Input.
export function Selecao({ className, ...props }: ComponentProps<"select">) {
  return (
    <select
      className={cn(
        "h-10 w-full rounded-lg border border-input bg-transparent px-2.5 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-50",
        className,
      )}
      {...props}
    />
  );
}
