import { useEffect, useState } from "react";
import { Link } from "react-router";

import { MinhaTripulacao } from "@/components/MinhaTripulacao";
import { MissoesDaTripulacao } from "@/components/MissoesDaTripulacao";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { api, ErroApi } from "@/lib/api";
import type { Perfil } from "@/lib/perfil";
import { lerToken } from "@/lib/sessao";

type Estado = "carregando" | "visitante" | "logado" | "falha";

// Início do portal. Quem não entrou vê as opções de cadastro e login; quem
// entrou vê a tripulação e as missões. Os dados do cadastro e o botão de sair
// ficam na aba Perfil, e o ranking tem a própria aba.
export default function Inicio() {
  const [estado, setEstado] = useState<Estado>(() => (lerToken() ? "carregando" : "visitante"));
  const [perfil, setPerfil] = useState<Perfil | null>(null);
  const [temTripulacao, setTemTripulacao] = useState(false);

  useEffect(() => {
    if (!lerToken()) return;
    api<Perfil>("/api/perfil", { autenticado: true })
      .then((dados) => {
        setPerfil(dados);
        setEstado("logado");
      })
      .catch((e) => {
        // 401: o token venceu e já foi apagado; a pessoa entra de novo.
        setEstado(e instanceof ErroApi && e.status === 401 ? "visitante" : "falha");
      });
  }, []);

  return (
    <main className="flex min-h-svh items-start justify-center p-4 sm:items-center">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>A Expedição da Estrela Polar</CardTitle>
          <CardDescription>Concentra DF 2026 · 17 e 18 de outubro</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {estado === "carregando" && <p className="text-sm">Carregando...</p>}

          {estado === "visitante" && (
            <>
              <p className="text-sm">
                Cadastre-se para entrar na sua tripulação e participar das missões do evento.
              </p>
              <Button asChild className="h-10 w-full">
                <Link to="/cadastro">Fazer cadastro</Link>
              </Button>
              <Button asChild variant="outline" className="h-10 w-full">
                <Link to="/login">Já tenho conta</Link>
              </Button>
            </>
          )}

          {estado === "logado" && perfil && (
            <>
              <p className="text-lg font-medium">Olá, {perfil.nome.split(" ")[0]}!</p>
              {perfil.staff ? (
                <>
                  <p className="text-sm">Você está no staff, por isso não entra em tripulação.</p>
                  <Button asChild className="h-10 w-full">
                    <Link to="/staff">Abrir painel do staff</Link>
                  </Button>
                </>
              ) : (
                <>
                  <MinhaTripulacao meuId={perfil.id} aoCarregar={setTemTripulacao} />
                  <MissoesDaTripulacao temTripulacao={temTripulacao} />
                </>
              )}
            </>
          )}

          {estado === "falha" && (
            <>
              <p className="text-sm">Não foi possível carregar seus dados.</p>
              <Button className="h-10 w-full" onClick={() => window.location.reload()}>
                Tentar de novo
              </Button>
            </>
          )}
        </CardContent>
      </Card>
    </main>
  );
}
