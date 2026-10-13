import { useEffect, useState } from "react";
import { Navigate } from "react-router";

import { MissoesDaTripulacao } from "@/components/MissoesDaTripulacao";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { api, ErroApi } from "@/lib/api";
import type { Perfil } from "@/lib/perfil";
import { lerToken } from "@/lib/sessao";

type Estado =
  | { tipo: "carregando" }
  | { tipo: "sem-login" }
  | { tipo: "falha" }
  | { tipo: "staff" }
  | { tipo: "pronto"; temTripulacao: boolean };

// Aba Missões: as missões visíveis, com o envio das respostas. As regras
// ficam no Início, junto da história da expedição.
export default function Missoes() {
  const [estado, setEstado] = useState<Estado>(() => (lerToken() ? { tipo: "carregando" } : { tipo: "sem-login" }));

  useEffect(() => {
    if (!lerToken()) return;
    api<Perfil>("/api/perfil", { autenticado: true })
      .then(async (perfil) => {
        if (perfil.staff) {
          setEstado({ tipo: "staff" });
          return;
        }
        // Sem tripulação a API responde 404 (antes da montagem, em 15/10).
        const temTripulacao = await api("/api/tripulacao", { autenticado: true }).then(
          () => true,
          () => false,
        );
        setEstado({ tipo: "pronto", temTripulacao });
      })
      .catch((e) => {
        setEstado(e instanceof ErroApi && e.status === 401 ? { tipo: "sem-login" } : { tipo: "falha" });
      });
  }, []);

  if (estado.tipo === "sem-login") return <Navigate to="/login" replace />;

  return (
    <main className="flex min-h-svh items-start justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle>Missões</CardTitle>
          <CardDescription>As regras estão no Início, em "Como funciona a expedição".</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {estado.tipo === "carregando" && <p className="text-sm">Carregando...</p>}
          {estado.tipo === "falha" && (
            <>
              <p className="text-sm">Não foi possível carregar seus dados.</p>
              <Button className="h-10 w-full" onClick={() => window.location.reload()}>
                Tentar de novo
              </Button>
            </>
          )}
          {estado.tipo === "staff" && (
            <p className="text-center text-sm">
              Você está no staff, por isso não cumpre missões. Para criar e editar missões, use o painel do staff, na
              aba Início.
            </p>
          )}
          {estado.tipo === "pronto" && <MissoesDaTripulacao temTripulacao={estado.temTripulacao} />}
        </CardContent>
      </Card>
    </main>
  );
}
