import { useEffect, useState } from "react";
import { Link } from "react-router";

import { MinhaTripulacao } from "@/components/MinhaTripulacao";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { api, ErroApi } from "@/lib/api";
import { AREAS_ATUACAO, CARGOS, TEMPOS_MEJ, rotuloDe } from "@/lib/opcoes";
import { lerToken, sair } from "@/lib/sessao";

type Perfil = {
  id: number;
  nome: string;
  email: string;
  ejId: number;
  ejNome: string;
  cargo: string;
  tempoMej: string;
  areaAtuacao: string;
  staff: boolean;
};

type Estado = "carregando" | "visitante" | "logado" | "falha";

// Início do portal. Quem não entrou vê as opções de cadastro e login;
// quem entrou vê o próprio perfil.
export default function Inicio() {
  const [estado, setEstado] = useState<Estado>(() => (lerToken() ? "carregando" : "visitante"));
  const [perfil, setPerfil] = useState<Perfil | null>(null);

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

  function sairDaConta() {
    sair();
    setPerfil(null);
    setEstado("visitante");
  }

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
              <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm">
                <dt className="text-muted-foreground">EJ</dt>
                <dd>{perfil.ejNome}</dd>
                <dt className="text-muted-foreground">Cargo</dt>
                <dd>{rotuloDe(CARGOS, perfil.cargo)}</dd>
                <dt className="text-muted-foreground">Tempo de MEJ</dt>
                <dd>{rotuloDe(TEMPOS_MEJ, perfil.tempoMej)}</dd>
                <dt className="text-muted-foreground">Área</dt>
                <dd>{rotuloDe(AREAS_ATUACAO, perfil.areaAtuacao)}</dd>
                <dt className="text-muted-foreground">E-mail</dt>
                <dd className="break-all">{perfil.email}</dd>
              </dl>
              {perfil.staff ? (
                <>
                  <p className="text-sm">Você está no staff, por isso não entra em tripulação.</p>
                  <Button asChild className="h-10 w-full">
                    <Link to="/staff">Abrir painel do staff</Link>
                  </Button>
                </>
              ) : (
                <MinhaTripulacao meuId={perfil.id} />
              )}
              <Button variant="outline" className="h-10 w-full" onClick={sairDaConta}>
                Sair
              </Button>
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
