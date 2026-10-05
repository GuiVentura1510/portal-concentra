import { useEffect, useState } from "react";

const API_URL = import.meta.env.VITE_API_URL;

export default function App() {
  const [resposta, setResposta] = useState("consultando a API...");

  useEffect(() => {
    fetch(`${API_URL}/api/ping`)
      .then((r) => r.json())
      .then((dados) => setResposta(JSON.stringify(dados)))
      .catch(() => setResposta("falha ao falar com a API"));
  }, []);

  return (
    <main style={{ fontFamily: "sans-serif", padding: 24 }}>
      <h1>Portal Concentra</h1>
      <p>Resposta da API: {resposta}</p>
    </main>
  );
}
