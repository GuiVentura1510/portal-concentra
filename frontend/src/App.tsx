import { BrowserRouter, Navigate, Route, Routes } from "react-router";

import { ComAbas } from "@/components/Abas";
import Cadastro from "@/pages/Cadastro";
import Inicio from "@/pages/Inicio";
import Login from "@/pages/Login";
import Perfil from "@/pages/Perfil";
import Ranking from "@/pages/Ranking";

// Mapa de rotas do portal: cada endereço aponta para uma tela.
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Telas com a barra de abas embaixo (a barra só aparece com login). */}
        <Route element={<ComAbas />}>
          <Route path="/" element={<Inicio />} />
          <Route path="/ranking" element={<Ranking />} />
          <Route path="/perfil" element={<Perfil />} />
        </Route>
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
