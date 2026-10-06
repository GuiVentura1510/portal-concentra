import { BrowserRouter, Navigate, Route, Routes } from "react-router";

import EmBreve from "@/pages/EmBreve";
import Inicio from "@/pages/Inicio";

// Mapa de rotas do portal: cada endereço aponta para uma tela.
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path="/cadastro" element={<EmBreve titulo="Cadastro" />} />
        <Route path="/login" element={<EmBreve titulo="Login" />} />
        {/* Endereço desconhecido volta ao início, para ninguém cair em
            uma tela em branco por causa de link digitado errado. */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
