import { useEffect, useState } from "react";
import { Link } from "react-router";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

// O endereço da API vem do ambiente para o mesmo código servir na máquina
// local e em produção (no Render, a variável VITE_API_URL).
const API_URL = import.meta.env.VITE_API_URL;

async function buscarPing(): Promise<string> {
  try {
    const r = await fetch(`${API_URL}/api/ping`);
    return JSON.stringify(await r.json());
  } catch {
    return "falha ao falar com a API";
  }
}

// Tela provisória: prova que site, API e banco estão ligados.
// Será substituída pela home de verdade no bloco de 06 a 08/10.
export default function Inicio() {
  const [resposta, setResposta] = useState("consultando a API...");

  useEffect(() => {
    buscarPing().then(setResposta);
  }, []);

  return (
    <main className="flex min-h-svh items-center justify-center p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Portal Concentra</CardTitle>
          <CardDescription>A Expedição da Estrela Polar</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-sm break-all">Resposta da API: {resposta}</p>
          <Button onClick={() => buscarPing().then(setResposta)}>
            Consultar de novo
          </Button>
          <nav className="flex justify-between text-sm underline">
            <Link to="/cadastro">Cadastro</Link>
            <Link to="/login">Login</Link>
          </nav>
        </CardContent>
      </Card>
    </main>
  );
}
