import { Link } from "react-router";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

// Tela provisória para rotas que já existem no mapa, mas ainda não têm
// conteúdo. Some quando a última tela de verdade for criada.
export default function EmBreve({ titulo }: { titulo: string }) {
  return (
    <main className="flex min-h-svh items-center justify-center p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>{titulo}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-sm">Esta tela ainda está em construção.</p>
          <Link to="/" className="text-sm underline">
            Voltar ao início
          </Link>
        </CardContent>
      </Card>
    </main>
  );
}
