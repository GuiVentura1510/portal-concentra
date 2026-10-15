import { BrowserRouter, Navigate, Route, Routes } from "react-router";

import { ComAbas } from "@/components/Abas";
import { useLogado } from "@/lib/sessao";
import BoasVindas from "@/pages/BoasVindas";
import Cadastro from "@/pages/Cadastro";
import Expedicao from "@/pages/Expedicao";
import Inicio from "@/pages/Inicio";
import Login from "@/pages/Login";
import Missoes from "@/pages/Missoes";
import Perfil from "@/pages/Perfil";
import Ranking from "@/pages/Ranking";
import Tripulacao from "@/pages/Tripulacao";

// Mapa de rotas do portal: cada endereço aponta para uma tela.
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Telas com a barra de abas embaixo (a barra só aparece com login). */}
        <Route element={<ComAbas />}>
          <Route path="/" element={<Entrada />} />
          <Route path="/missoes" element={<Missoes />} />
          <Route path="/tripulacao" element={<Tripulacao />} />
          <Route path="/ranking" element={<Ranking />} />
          <Route path="/perfil" element={<Perfil />} />
        </Route>
        <Route path="/expedicao" element={<Expedicao />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/login" element={<Login />} />
        {/* Endereço desconhecido volta ao início, para ninguém cair em
            uma tela em branco por causa de link digitado errado. O antigo
            "/staff" também cai aqui: o painel do staff agora é o próprio
            início das contas do staff. */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

// Endereço "/": quem não entrou vê a capa da expedição; quem entrou vê o
// Início (a página da expedição, ou o painel, no caso do staff). Ao sair da
// conta, a troca é automática, porque useLogado avisa quando o token some.
function Entrada() {
  return useLogado() ? <Inicio /> : <BoasVindas />;
}
