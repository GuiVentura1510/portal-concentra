import { BrowserRouter, Navigate, Route, Routes } from "react-router";

import Cadastro from "@/pages/Cadastro";
import Inicio from "@/pages/Inicio";
import Login from "@/pages/Login";
import Staff from "@/pages/Staff";

// Mapa de rotas do portal: cada endereço aponta para uma tela.
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route path="/login" element={<Login />} />
        <Route path="/staff" element={<Staff />} />
        {/* Endereço desconhecido volta ao início, para ninguém cair em
            uma tela em branco por causa de link digitado errado. */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
