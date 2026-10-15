// Teste de carga COMPLETO do Portal Concentra (k6), contra a produção.
//
// Simula o evento inteiro, em fases (cerca de 21 minutos no modo "completo"):
//   0:00  cadastro      500 contas de carga (carga-0001@carga.teste ...), 20 de cada vez
//   3:00  antes         até 200 pessoas navegando antes da montagem (login de cada uma)
//   6:30  montagem      o staff aperta "Montar tripulações" (tempo medido)
//   6:40  revelacao     pico do meio-dia: 40 pessoas por segundo abrindo a tripulação
//   8:00  jogo          até 300 pessoas navegando e enviando missões (com 1 a 3 fotos)
//   8:00  staff         5 pessoas do staff na aba Envios, Tripulações e Pessoas, retirando envios
//  16:00  estresse      sobe de 300 para 500 pessoas, atualizando a cada 1 a 3 s
//
// ATENÇÃO: este teste ESCREVE na produção (contas, missões "[CARGA]", envios, fotos e a
// MONTAGEM das tripulações). Por isso:
//   - só rode ANTES de abrir o cadastro de verdade e antes da montagem real;
//   - faça um Export do banco antes (página Recovery do Render);
//   - no fim, rode o carga/limpeza.sql, que apaga tudo o que o teste criou e desfaz a
//     montagem, para a montagem real de sábado funcionar.
//
// Segurança: se mais de 10% dos pedidos falharem, o teste para sozinho (abortOnFail),
// para não martelar um servidor que já caiu.
//
// Variáveis: API_URL, EMAIL_STAFF, SENHA_STAFF (conta do staff em STAFF_EMAILS).
// Opcionais: MODO=fumaca (1,5 min, 10 contas, sem montagem) | completo (padrão) |
//            jogo (~14 min, depois de uma montagem já feita: revelação, jogo, staff,
//            logins e estresse, com contas que já entraram, como no evento);
//            PESSOAS=500 (quantas contas de carga).

import http from "k6/http";
import exec from "k6/execution";
import encoding from "k6/encoding";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";

const API = (__ENV.API_URL || "").replace(/\/+$/, "");
const EMAIL_STAFF = __ENV.EMAIL_STAFF;
const SENHA_STAFF = __ENV.SENHA_STAFF;
const MODO = __ENV.MODO || "completo";
const PESSOAS = Number(__ENV.PESSOAS || (MODO === "fumaca" ? 10 : 500));
const SENHA_CARGA = "carga-senha-123";
const DOMINIO = "carga.teste";

// Foto realista (1280x960, ~170 KB), como a que o site manda depois de reduzir.
const FOTO = encoding.b64encode(open("./foto-carga.jpg", "b"));

const tempoMontagem = new Trend("montagem_ms", true);
const enviosAceitos = new Counter("envios_aceitos");
const enviosRecusados409 = new Counter("envios_409");

// 404 (sem tripulação antes da montagem) e 409 (já enviado, vagas esgotadas, conta já
// existente numa segunda rodada) são respostas normais do portal, não erros.
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 299 }, 404, 409));

const COMPLETO = {
  cadastro: {
    executor: "shared-iterations", exec: "cadastrar", vus: 20, iterations: PESSOAS,
    maxDuration: "3m", tags: { fase: "cadastro" },
  },
  antes: {
    executor: "ramping-vus", exec: "navegar", startTime: "3m", startVUs: 0,
    stages: [{ duration: "2m", target: 200 }, { duration: "1m30s", target: 200 }],
    gracefulRampDown: "5s", tags: { fase: "antes" },
  },
  montagem: {
    executor: "per-vu-iterations", exec: "montar", vus: 1, iterations: 1,
    startTime: "6m30s", maxDuration: "2m", tags: { fase: "montagem" },
  },
  revelacao: {
    executor: "constant-arrival-rate", exec: "revelar", rate: 40, timeUnit: "1s",
    duration: "1m", startTime: "6m40s", preAllocatedVUs: 100, maxVUs: 400,
    tags: { fase: "revelacao" },
  },
  jogo: {
    executor: "ramping-vus", exec: "jogar", startTime: "8m", startVUs: 0,
    stages: [{ duration: "3m", target: 300 }, { duration: "4m", target: 300 }, { duration: "1m", target: 0 }],
    gracefulRampDown: "10s", tags: { fase: "jogo" },
  },
  staff: {
    executor: "constant-vus", exec: "conferir", vus: 5, duration: "8m", startTime: "8m",
    tags: { fase: "staff" },
  },
  estresse: {
    executor: "ramping-vus", exec: "estressar", startTime: "16m", startVUs: 300,
    stages: [{ duration: "2m", target: 500 }, { duration: "2m", target: 500 }, { duration: "30s", target: 0 }],
    gracefulRampDown: "5s", tags: { fase: "estresse" },
  },
};

const FUMACA = {
  cadastro: { executor: "shared-iterations", exec: "cadastrar", vus: 2, iterations: PESSOAS, tags: { fase: "cadastro" } },
  antes: { executor: "constant-vus", exec: "navegar", vus: 5, duration: "30s", startTime: "20s", tags: { fase: "antes" } },
  jogo: { executor: "constant-vus", exec: "jogar", vus: 5, duration: "40s", startTime: "50s", tags: { fase: "jogo" } },
  staff: { executor: "constant-vus", exec: "conferir", vus: 1, duration: "40s", startTime: "50s", tags: { fase: "staff" } },
};

// Depois da montagem. As pessoas já estão logadas (o login vale 10 dias), então as
// fases usam tokens obtidos no começo; os logins são medidos à parte, num ritmo fixo.
const JOGO = {
  revelacao: {
    executor: "constant-arrival-rate", exec: "revelar", rate: 40, timeUnit: "1s",
    duration: "1m", preAllocatedVUs: 100, maxVUs: 400, tags: { fase: "revelacao" },
  },
  jogo: {
    executor: "ramping-vus", exec: "jogar", startTime: "1m10s", startVUs: 0,
    stages: [{ duration: "3m", target: 300 }, { duration: "4m", target: 300 }, { duration: "1m", target: 0 }],
    gracefulRampDown: "10s", tags: { fase: "jogo" },
  },
  staff: {
    executor: "constant-vus", exec: "conferir", vus: 5, duration: "8m", startTime: "1m10s",
    tags: { fase: "staff" },
  },
  // Gente entrando ao longo do evento: 1 login por segundo durante 3 minutos.
  logins: {
    executor: "constant-arrival-rate", exec: "logar", rate: 1, timeUnit: "1s",
    duration: "3m", startTime: "3m", preAllocatedVUs: 5, maxVUs: 30, tags: { fase: "logins" },
  },
  estresse: {
    executor: "ramping-vus", exec: "estressar", startTime: "9m20s", startVUs: 300,
    stages: [{ duration: "2m", target: 500 }, { duration: "2m", target: 500 }, { duration: "30s", target: 0 }],
    gracefulRampDown: "5s", tags: { fase: "estresse" },
  },
};

export const options = {
  scenarios: MODO === "fumaca" ? FUMACA : MODO === "jogo" ? JOGO : COMPLETO,
  setupTimeout: "2m",
  teardownTimeout: "2m",
  thresholds: {
    http_req_failed: [{ threshold: "rate<0.10", abortOnFail: true, delayAbortEval: "1m" }],
    "http_req_failed{fase:jogo}": ["rate<0.01"],
    "http_req_duration{tipo:leitura}": ["p(95)<1500"],
    "http_req_duration{tipo:envio}": ["p(95)<5000"],
    "http_req_duration{tipo:cadastro}": ["p(95)<5000"],
    "http_req_duration{tipo:login}": ["p(95)<3000"],
    "http_req_duration{fase:revelacao}": ["p(95)<2000"],
    "http_req_duration{fase:estresse}": ["p(95)<3000"],
    checks: ["rate>0.98"],
  },
};

// ---------- ajudantes ----------

function json(metodo, caminho, corpo, token, tipo, nome) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  return http.request(metodo, `${API}${caminho}`, corpo === null ? null : JSON.stringify(corpo), {
    headers, tags: { tipo, nome }, timeout: "60s",
  });
}

function ler(caminho, token, nome) {
  const r = json("GET", caminho, null, token, "leitura", nome);
  check(r, { [`${nome} ok`]: (x) => x.status === 200 || x.status === 404 });
  return r;
}

function entrarComo(email, senha) {
  const r = json("POST", "/api/login", { email, senha }, null, "login", "login");
  check(r, { "login 200": (x) => x.status === 200 });
  return r.status === 200 ? r.json("token") : null;
}

const numero = (i) => String(i).padStart(4, "0");
const emailDe = (i) => `carga-${numero(i)}@${DOMINIO}`;
const sorteio = (lista) => lista[Math.floor(Math.random() * lista.length)];

// Cada pessoa virtual usa uma conta de carga fixa e guarda o token entre as rodadas.
let meuToken = null;
const jaEnviei = {};
let falhasMostradas = 0;
function minhaConta() {
  return ((exec.vu.idInTest - 1) % PESSOAS) + 1;
}
function token(dados) {
  // Modo jogo: tokens obtidos no começo (como quem já entrou no portal antes).
  if (dados && dados.tokens && dados.tokens.length > 0) {
    return dados.tokens[(exec.vu.idInTest - 1) % dados.tokens.length];
  }
  if (!meuToken) meuToken = entrarComo(emailDe(minhaConta()), SENHA_CARGA);
  return meuToken;
}

// ---------- preparação ----------

function missao(staff, corpo) {
  const r = json("POST", "/api/staff/missoes", corpo, staff, "preparo", "criar-missao");
  if (r.status !== 201 && r.status !== 200) {
    throw new Error(`Não consegui criar a missão de carga (${r.status}): ${r.body}`);
  }
  return { id: r.json("id"), maxFotos: r.json("maxFotos") || 1, comprovacao: corpo.comprovacao, corpo };
}

export function setup() {
  if (!API || !EMAIL_STAFF || !SENHA_STAFF) {
    throw new Error("Defina API_URL, EMAIL_STAFF e SENHA_STAFF antes de rodar.");
  }
  const staff = entrarComo(EMAIL_STAFF, SENHA_STAFF);
  if (!staff) throw new Error("O login do staff falhou: confira e-mail, senha e API_URL.");

  const resumo = json("GET", "/api/staff/tripulacoes/resumo", null, staff, "preparo", "resumo");
  if (resumo.status === 403) throw new Error("A conta não é do staff (ou precisa sair e entrar de novo).");
  if (MODO === "jogo" && resumo.json("tripulacoes") === 0) {
    throw new Error("O modo jogo precisa de uma montagem já feita (rode o modo completo antes).");
  }
  if (MODO === "completo" && resumo.json("tripulacoes") > 0) {
    throw new Error("Já existem tripulações: a montagem já foi feita. Rode o carga/limpeza.sql antes.");
  }

  const ejs = json("GET", "/api/ejs", null, null, "preparo", "ejs").json().map((e) => e.id);

  const base = { descricao: "Missão criada pelo teste de carga. Será apagada.", pontos: 10, vagas: null,
    inicio: null, fim: null, conclusao: "ENVIO_CAPITAO", visivel: true };
  const missoes = [
    missao(staff, { ...base, titulo: "[CARGA] Individual com fotos", alcance: "INDIVIDUAL", comprovacao: "FOTO", maxFotos: 3 }),
    missao(staff, { ...base, titulo: "[CARGA] Individual só texto", alcance: "INDIVIDUAL", comprovacao: "TEXTO" }),
    missao(staff, { ...base, titulo: "[CARGA] Tripulação texto ou foto", alcance: "TRIPULACAO", comprovacao: "TEXTO_OU_FOTO", maxFotos: 2 }),
    // Com vagas: muitas tripulações disputam as mesmas 30 vagas ao mesmo tempo.
    missao(staff, { ...base, titulo: "[CARGA] Tripulação com 30 vagas", alcance: "TRIPULACAO", comprovacao: "TEXTO", vagas: 30 }),
  ];
  console.log(`Missões de carga: ${missoes.map((m) => m.id).join(", ")} (máximo de fotos aceito pela API: ${missoes[0].maxFotos})`);

  // Modo jogo: entra com 100 contas de carga, de 5 em 5, antes de começar.
  const tokens = [];
  if (MODO === "jogo") {
    const total = Math.min(100, PESSOAS);
    for (let i = 1; i <= total; i += 5) {
      const lote = [];
      for (let k = i; k < i + 5 && k <= total; k++) {
        lote.push(["POST", `${API}/api/login`, JSON.stringify({ email: emailDe(k), senha: SENHA_CARGA }),
          { headers: { "Content-Type": "application/json" }, tags: { tipo: "preparo", nome: "login-previo" }, timeout: "60s" }]);
      }
      for (const r of http.batch(lote)) if (r.status === 200) tokens.push(r.json("token"));
    }
    console.log(`Logins prévios: ${tokens.length} de ${total}`);
    if (tokens.length === 0) throw new Error("Nenhuma conta de carga conseguiu entrar.");
  }
  return { staff, ejs, missoes, tokens };
}

// ---------- fases ----------

const CARGOS = ["TRAINEE", "ASSESSOR_CONSULTOR", "ASSESSOR_CONSULTOR", "PROJETISTA", "GERENTE_COORDENADOR", "DIRETOR", "PRESIDENTE", "POS_JUNIOR"];
const TEMPOS = ["MENOS_DE_6_MESES", "DE_6_MESES_A_1_ANO", "DE_1_A_2_ANOS", "MAIS_DE_2_ANOS"];
const AREAS = ["PRESIDENCIA", "PROJETOS", "COMERCIAL", "MARKETING", "OPERACOES"];

export function cadastrar(dados) {
  const i = exec.scenario.iterationInTest + 1;
  const cargo = sorteio(CARGOS);
  const r = json("POST", "/api/cadastro", {
    nome: `Carga ${numero(i)}`, email: emailDe(i), senha: SENHA_CARGA, ejId: sorteio(dados.ejs),
    cargo, tempoMej: sorteio(TEMPOS), areaAtuacao: cargo === "TRAINEE" ? null : sorteio(AREAS),
  }, null, "cadastro", "cadastro");
  check(r, { "cadastro 201 (ou 409 já existe)": (x) => x.status === 201 || x.status === 409 });
}

// Uma pessoa olhando o portal: as chamadas que o celular faz ao abrir as abas.
function telas(t) {
  ler("/api/perfil", t, "perfil");
  ler("/api/missoes", t, "missoes");
  ler("/api/tripulacao", t, "tripulacao");
  if (Math.random() < 0.5) ler("/api/ranking", t, "ranking");
  else ler("/api/ranking/individual", t, "ranking-individual");
}

export function navegar(dados) {
  const t = token(dados);
  if (!t) { sleep(5); return; }
  telas(t);
  sleep(3 + Math.random() * 5);
}

export function montar(dados) {
  const inicio = Date.now();
  const r = json("POST", "/api/staff/tripulacoes/montar", null, dados.staff, "montagem", "montar");
  tempoMontagem.add(Date.now() - inicio);
  check(r, { "montagem 200": (x) => x.status === 200 });
  console.log(`Montagem: status ${r.status} em ${Date.now() - inicio} ms: ${r.body}`);
}

// Meio-dia de sábado: todo mundo abre o portal para ver a tripulação.
export function revelar(dados) {
  const t = token(dados);
  if (!t) return;
  ler("/api/tripulacao", t, "tripulacao");
  ler("/api/perfil", t, "perfil");
}

function enviar(t, m) {
  const corpo = { texto: null, foto: null, fotos: [] };
  if (m.comprovacao === "TEXTO") {
    corpo.texto = `Resposta do teste de carga ${minhaConta()}`;
  } else {
    const n = 1 + Math.floor(Math.random() * m.maxFotos);
    corpo.foto = FOTO;
    for (let k = 1; k < n; k++) corpo.fotos.push(FOTO);
    if (Math.random() < 0.5) corpo.texto = "Com foto";
  }
  const r = json("POST", `/api/missoes/${m.id}/resposta`, corpo, t, "envio", `envio-${m.comprovacao}`);
  check(r, { "envio 201 ou 409": (x) => x.status === 201 || x.status === 409 });
  if (r.status === 201) enviosAceitos.add(1);
  if (r.status === 409) enviosRecusados409.add(1);
  // Mostra o motivo das falhas (no máximo 10 por pessoa virtual), para entender o erro.
  if (r.status !== 201 && r.status !== 409 && falhasMostradas++ < 10) {
    console.warn(`Envio recusado na missão "${m.corpo.titulo}": ${r.status} ${r.body}`);
  }
}

// Durante o evento: navega e, de vez em quando, cumpre uma missão de carga.
export function jogar(dados) {
  const t = token(dados);
  if (!t) { sleep(5); return; }
  telas(t);
  if (Math.random() < 0.2) {
    const pendentes = dados.missoes.filter((m) => !jaEnviei[m.id]);
    if (pendentes.length > 0) {
      const m = sorteio(pendentes);
      jaEnviei[m.id] = true;
      enviar(t, m);
      ler("/api/missoes", t, "missoes");
    }
  }
  sleep(3 + Math.random() * 5);
}

// Staff conferindo: Envios (com as fotos), Tripulações e Pessoas; às vezes retira um envio.
export function conferir(dados) {
  const s = dados.staff;
  const envios = ler("/api/staff/conclusoes", s, "staff-envios");
  ler("/api/staff/tripulacoes", s, "staff-tripulacoes");
  ler(`/api/staff/congressistas?busca=carga`, s, "staff-pessoas");
  if (envios.status === 200 && Math.random() < 0.3) {
    const lista = envios.json().filter((e) => (e.missaoTitulo || "").startsWith("[CARGA]"));
    if (lista.length > 0) {
      const alvo = sorteio(lista);
      const r = json("POST", `/api/staff/conclusoes/${alvo.id}/retirar`, { motivo: "Teste de carga" }, s, "envio", "retirar");
      check(r, { "retirada 200 ou 409": (x) => x.status === 200 || x.status === 409 });
      // Abre as fotos do envio, como o staff faz ao conferir.
      for (const chave of alvo.fotoChaves || []) {
        http.get(`${API}/api/comprovacoes/${chave}`, { tags: { tipo: "foto", nome: "foto-envio" } });
      }
    }
  }
  sleep(5 + Math.random() * 5);
}

// Além do esperado: mais gente e atualizando mais rápido, para achar o limite.
export function estressar(dados) {
  const t = token(dados);
  if (!t) { sleep(2); return; }
  telas(t);
  sleep(1 + Math.random() * 2);
}

// Um login de uma conta de carga qualquer (mede o custo do login sob carga).
export function logar() {
  entrarComo(emailDe(1 + Math.floor(Math.random() * PESSOAS)), SENHA_CARGA);
}

// No fim, esconde as missões de carga (a limpeza.sql apaga de vez).
export function teardown(dados) {
  for (const m of dados.missoes) {
    json("PUT", `/api/staff/missoes/${m.id}`, { ...m.corpo, visivel: false }, dados.staff, "preparo", "esconder-missao");
  }
  console.log("Fim. Agora rode o carga/limpeza.sql para apagar as contas, envios e a montagem do teste.");
}
