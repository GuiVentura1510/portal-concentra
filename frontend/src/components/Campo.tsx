import type { ReactNode } from "react";

import { Label } from "@/components/ui/label";

// Rótulo e campo empilhados, com o espaçamento padrão dos formulários.
export function Campo({ rotulo, id, children }: { rotulo: string; id: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{rotulo}</Label>
      {children}
    </div>
  );
}
