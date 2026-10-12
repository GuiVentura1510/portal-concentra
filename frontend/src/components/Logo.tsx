// Provisório até chegar o logotipo oficial com fundo transparente (o arquivo
// do Manual não tem o "26" nem transparência). Quando chegar, trocar o texto
// por <img src={logo} alt="Concentra 26" className="mx-auto h-5" />.
export function Logo() {
  return (
    <p className="text-center text-lg leading-none font-semibold tracking-tight text-creme">
      Concentra<span className="font-extrabold">26</span>
    </p>
  );
}
