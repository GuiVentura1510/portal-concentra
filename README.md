# Portal Concentra

Portal de gamificação do Concentra DF 2026, evento da Concentro nos dias 17 e 18/10/2026. A gamificação se chama **A Expedição da Estrela Polar**: os congressistas são divididos em tripulações, cumprem missões durante o evento e somam pontos num ranking.

## Estado atual

Todas as funções previstas para o evento estão em produção desde 11/10/2026.

- [x] Backend com Postgres, Flyway e endpoint de verificação
- [x] Deploy automático do backend e do frontend
- [x] Tailwind, shadcn/ui, rotas e manifesto do PWA
- [x] Cadastro com EJ, cargo, tempo de MEJ e área, e login com JWT
- [x] Perfil do congressista
- [x] Montagem das tripulações e encaixe de quem se cadastra depois
- [x] Missões criadas pelo staff, envio pelas tripulações, fila de aprovação
- [x] Pontuação por extrato e ranking
- [x] Painel do staff: tripulações, missões, fila e pessoas
- [x] Missões individuais e exclusão de missões sem respostas
- [x] Ranking individual
- [x] Foto de perfil
- [x] Edição dos dados do perfil e troca de senha
- [x] Login e cadastro com o visual da expedição
- [x] Capa da expedição e tema com a identidade do evento
- [ ] Visual do Início de quem está logado
- [ ] Logotipo oficial na capa
- [ ] Ícones oficiais do PWA

## Como funciona

1. **Cadastro.** A pessoa abre o site, informa nome, e-mail (o mesmo da compra do ingresso), senha, EJ, cargo, tempo de MEJ e área, e já entra logada. O login vale por 10 dias no mesmo aparelho.
2. **Tripulações.** Em 15/10 o staff monta as tripulações uma única vez. São grupos de 5 a 6 pessoas, de EJs diferentes, misturando novatos e veteranos. Quem se cadastra depois da montagem entra na hora numa tripulação com vaga e sem colega da mesma EJ. Os nomes são as 88 constelações.
3. **Capitão e ponto de encontro.** A tripulação escolhe o capitão no evento e o staff registra no painel. O staff também define o ponto de encontro de cada tripulação.
4. **Missões.** O staff cria as missões no painel, com pontos, vagas e horário. Cada missão é **da tripulação** (a tripulação cumpre junta, uma vez) ou **individual** (cada pessoa cumpre a sua). Há duas formas de conclusão:
   - **Resposta pelo portal:** uma resposta em texto é enviada e o staff aprova ou recusa. Nas missões da tripulação, enquanto não há capitão qualquer membro pode enviar; depois, só o capitão. Nas individuais, cada pessoa envia a sua.
   - **Staff marca:** o staff registra a missão cumprida direto, como ao carimbar um passaporte: no cartão da tripulação ou, nas individuais, na aba Pessoas.
5. **Rankings.** Cada aprovação gera um lançamento de pontos. São dois rankings separados:
   - **Tripulações:** soma só as missões da tripulação.
   - **Individual:** soma só as missões individuais de cada pessoa. Os pontos individuais não entram no ranking das tripulações.
6. **Foto de perfil.** Na aba Perfil, cada pessoa pode enviar, trocar ou remover a própria foto. Ela aparece para a tripulação, no ranking individual e no painel do staff. Quem não envia foto aparece com as iniciais do nome.

As missões são conteúdo da organização: são criadas e editadas no painel, não no código.

## Stack

**Backend**

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA e Hibernate
- PostgreSQL 17
- Flyway para migrações
- Spring Security como resource server OAuth2, com JWT HMAC-SHA256 e sessões stateless
- Actuator para monitoramento

**Frontend**

- React 19 com TypeScript 6
- Vite 8
- Tailwind CSS 4
- shadcn/ui (base Radix, preset Nova)
- React Router
- Manifesto de PWA, sem service worker

## Estrutura

```
portal-concentra/
├── backend/        API Spring Boot e Dockerfile
├── frontend/       Aplicação React
├── compose.yaml    Postgres local para desenvolvimento
└── README.md
```

O código do backend fica em `backend/src/main/java/br/org/concentro/portal/`, organizado por assunto:

| Pacote | Conteúdo |
| --- | --- |
| `infra` | Segurança, JWT, CORS e endpoint de verificação |
| `acesso` | Login, emissão do token e lista do staff (`EquipeStaff`) |
| `cadastro` | EJs, congressistas, perfil, foto de perfil e a aba Pessoas do staff |
| `tripulacao` | Tripulações, algoritmo de montagem (`MontadorDeTripulacoes`), nomes e rotas |
| `missao` | Missões, conclusões, lançamentos de pontos e ranking |

No frontend:

| Pasta | Conteúdo |
| --- | --- |
| `src/pages/` | Telas (capa, início, cadastro, login, ranking, perfil, staff) |
| `src/components/` | Componentes das telas, como os painéis do staff |
| `src/components/ui/` | Componentes do shadcn/ui |
| `src/assets/ilustracoes/` | Ilustrações do Manual de Identidade Visual em WebP (`pessoas/` e `estrelas/`) |
| `src/index.css` | Fontes, cores do tema e céu estrelado do fundo |
| `src/lib/estilos.ts` | Classes compartilhadas: botão da expedição, cartão dos formulários, campos e links |
| `src/lib/api.ts` | Chamadas à API; o token só vai nas chamadas marcadas como autenticadas |
| `src/lib/sessao.ts` | Guarda do token no aparelho |
| `src/lib/opcoes.ts` | Códigos e textos de cargo, tempo de MEJ e área |
| `src/lib/datas.ts` | Conversão entre UTC e o horário de Brasília |
| `src/lib/foto.ts` | Redução da foto de perfil no celular antes do envio e endereço da imagem |

## Produção

| Serviço | Endereço | Hospedagem |
| --- | --- | --- |
| Site | https://portal-concentra-web.onrender.com | Render, Static Site |
| API | https://portal-concentra-api.onrender.com | Render, Web Service com Docker |
| Banco | acesso interno | Render Postgres 17 |

Todo `git push` na branch `main` publica sozinho. Mudanças em `backend/` republicam a API (cerca de 1 minuto) e mudanças em `frontend/` republicam o site. Antes de testar em produção, espere a mensagem "Your service is live" no painel do Render.

## Pré-requisitos

- JDK 21
- Docker e Docker Compose
- Node 24

## Executando localmente

1. Inicie o PostgreSQL, a partir da raiz do repositório:

	```bash
	docker compose up -d db
	```

2. Inicie o backend:

	```bash
	cd backend
	./mvnw spring-boot:run
	```

	Para entrar como staff no ambiente local, informe o seu e-mail ao subir o backend:

	```bash
	STAFF_EMAILS=seu.email@exemplo.com ./mvnw spring-boot:run
	```

3. Em outro terminal, inicie o frontend:

	```bash
	cd frontend
	npm install
	npm run dev
	```

O backend sobe em `http://localhost:8080` e o frontend em `http://localhost:5173`. O Flyway executa as migrações automaticamente quando o backend inicia.

Depois de mudar o backend, reinicie-o (Ctrl+C e suba de novo). Se aparecer "Endereço já em uso", ainda há um backend antigo rodando: encerre-o com `kill $(lsof -t -i:8080)`.

Para parar o banco:

```bash
docker compose down
```

Os dados ficam no volume Docker `pgdata`. Para removê-los também, use `docker compose down -v`.

Para desfazer uma montagem de tripulações feita no banco local:

```bash
docker compose exec db psql -U portal -d portal -c "delete from lancamento; delete from conclusao; update congressista set tripulacao_id = null; delete from tripulacao; update missao set vagas_usadas = 0;"
```

## API

### Públicas

| Método e rota | Descrição |
| --- | --- |
| `GET /api/ping` | Verificação da API e da conexão com o banco |
| `GET /actuator/health` | Saúde da aplicação |
| `GET /api/ejs` | EJs em ordem alfabética, para o formulário |
| `POST /api/cadastro` | Cria a conta e devolve o token |
| `POST /api/login` | Devolve o token |
| `GET /api/fotos/{chave}` | Imagem de uma foto de perfil (JPEG), pela chave sorteada no envio |

### Com login

| Método e rota | Descrição |
| --- | --- |
| `GET /api/perfil` | Dados da pessoa, incluindo se é do staff, a chave da foto (`fotoChave`) e se EJ e tempo de MEJ ainda podem mudar (`podeMudarEjETempo`) |
| `PUT /api/perfil` | Edita os próprios dados: nome, cargo e área sempre; EJ e tempo de MEJ só antes da montagem das tripulações (409 depois). O e-mail não muda |
| `PUT /api/perfil/senha` | Troca a própria senha: `{"senhaAtual", "novaSenha"}`. 400 se a senha atual estiver errada ou a nova tiver menos de 8 caracteres |
| `PUT /api/perfil/foto` | Envia ou troca a própria foto: `{"imagem": "<JPEG em base64>"}`. Devolve a chave nova |
| `DELETE /api/perfil/foto` | Remove a própria foto |
| `GET /api/tripulacao` | Tripulação da pessoa (404 antes da montagem) |
| `GET /api/missoes` | Missões visíveis, com a situação da tripulação em cada uma |
| `POST /api/missoes/{id}/resposta` | Envio da resposta de uma missão |
| `GET /api/ranking` | Tripulações por pontos (só missões da tripulação) |
| `GET /api/ranking/individual` | Pessoas por pontos (só missões individuais): as 100 primeiras posições, a posição de quem pediu e o total |

### Só staff (`/api/staff/**`)

| Método e rota | Descrição |
| --- | --- |
| `GET /api/staff/tripulacoes/resumo` | Totais de congressistas e tripulações |
| `GET /api/staff/tripulacoes` | Todas as tripulações com membros |
| `POST /api/staff/tripulacoes/montar` | Monta as tripulações (uma única vez; 409 se já montadas) |
| `PUT /api/staff/tripulacoes/{id}/capitao` | Registra o capitão |
| `PUT /api/staff/tripulacoes/{id}/ponto-encontro` | Define o ponto de encontro |
| `GET` e `POST /api/staff/missoes` | Lista e cria missões |
| `PUT /api/staff/missoes/{id}` | Edita uma missão (o alcance só muda enquanto não há respostas) |
| `DELETE /api/staff/missoes/{id}` | Exclui uma missão sem respostas nem pontos (409 se houver) |
| `POST /api/staff/missoes/{id}/marcar` | Registra a missão cumprida: `{"tripulacaoId": ...}` nas da tripulação, `{"congressistaId": ...}` nas individuais |
| `GET /api/staff/conclusoes?status=PENDENTE` | Fila de respostas |
| `POST /api/staff/conclusoes/{id}/aprovar` | Aprova e lança os pontos |
| `POST /api/staff/conclusoes/{id}/recusar` | Recusa, com motivo |
| `GET /api/staff/congressistas?busca=` | Busca por nome, e-mail ou EJ |
| `POST /api/staff/congressistas/{id}/senha` | Gera uma senha temporária |
| `DELETE /api/staff/congressistas/{id}` | Remove um cadastro sem histórico |
| `DELETE /api/staff/congressistas/{id}/foto` | Remove a foto de perfil de alguém |
| `POST /api/staff/congressistas/cruzamento` | Aponta cadastros fora da lista de inscritos |

Uma rota nova só fica pública se for liberada em `SecurityConfig`. A rota `/error` precisa continuar liberada: sem ela, todo erro vira 403.

Exemplo de verificação:

```bash
curl https://portal-concentra-api.onrender.com/api/ping
```

```json
{"status":"ok","banco":"portal"}
```

## Rotas do site

| Endereço | Tela |
| --- | --- |
| `/` | Início. Visitante vê a capa da expedição, com "Entrar na expedição" e "Criar conta de viajante"; congressista vê a tripulação e as missões; a conta do staff vê o painel do staff |
| `/cadastro` | Cadastro ("Criar conta"), com login automático no fim |
| `/login` | Login. "Esqueci minha senha" explica que o staff gera uma senha temporária |
| `/ranking` | Rankings das tripulações e individual, em abas, destacando a tripulação e a posição da pessoa |
| `/perfil` | Perfil: foto, dados do cadastro com "Editar dados", "Trocar senha", tripulação, missões cumpridas e o botão de sair. Sem login, leva ao `/login` |

Quem está logado vê, no rodapé de `/`, `/ranking` e `/perfil`, a barra de abas Início, Ranking e Perfil.

O painel do staff não tem endereço próprio: é o início das contas do staff.

Um endereço desconhecido volta para `/` (inclusive o antigo `/staff`). Em produção, o Render reescreve `/*` para `/index.html`, para que as rotas funcionem ao abrir o link direto.

### Painel do staff

| Aba | O que faz |
| --- | --- |
| Tripulações | Resumo, botão de montar (confirmação em dois toques, some depois da montagem), busca, capitão, ponto de encontro e "Registrar missão cumprida" |
| Missões | Cria, edita e exclui missões: título, descrição, pontos, vagas, início e fim no horário de Brasília, para quem (tripulação ou individual), forma de conclusão e visibilidade. Só exclui missão sem respostas; as outras saem do ar desmarcando "visível" |
| Fila | Respostas enviadas pelas tripulações, com aprovar ou recusar |
| Pessoas | Busca, registro de missão individual cumprida, senha temporária, remoção de foto, remoção de cadastro e cruzamento com a lista de inscritos |

O site pode ser instalado na tela inicial do celular pelo manifesto em `frontend/public/manifest.webmanifest`. Não há service worker, de propósito: assim o celular nunca serve uma versão antiga quando houver correção durante o evento. Os ícones atuais são provisórios; para trocar, substitua os três PNG em `frontend/public/`.

## Identidade visual

O visual segue o mockup da capa e o Manual de Identidade Visual do Concentra 26.

- **Fontes** (pacotes `@fontsource-variable`, sem arquivos na pasta): Nunito é a fonte de todo o site; Lora itálica fica na classe `font-titulo` ("Estrela Polar" e os números da capa); Caveat fica na classe `font-manuscrita` ("um só norte"). As três foram escolhidas pelo mockup, porque o Manual não informa as fontes. Para trocar, mude o `@import` e a variável correspondente no `src/index.css`.
- **Duas superfícies:** a página é azul-noite com estrelas e os cartões são brancos. O `:root` do `src/index.css` tem as cores da página, e o bloco `[data-slot="card"]` troca as mesmas variáveis dentro de qualquer cartão. Assim botões, campos e textos do shadcn ficam legíveis nos dois lugares sem mudar cada componente.
- **Cores da marca**, disponíveis como classes (`text-creme`, `bg-noite` etc.): `noite` #0a0e3d, `creme` #f7f2e2, `lilas` #c3cbff, `rosa` #f7a8d8, `ceu` #a4ccfa, `oceano` #51a8c8. Os contrastes de texto foram conferidos (mínimo de 4,5:1).
- **Capa** (`src/pages/BoasVindas.tsx`): aparece em `/` para quem não está logado; a escolha entre capa e início fica no `App.tsx`. A ilustração (`src/components/CenaExpedicao.tsx`) ocupa a altura que sobra na tela, e tudo nela é posicionado em porcentagem, para caber em celulares de 360×640 a 412×915 sem rolagem. Os números do cartão (983 tripulantes, 3 missões, 1 magna final) são fixos, por decisão da organização, e ficam na lista `NUMEROS` no topo do arquivo.
- **Logotipo:** provisório, em texto. O arquivo do Manual não tem transparência nem o "26". Fica em `src/components/Logo.tsx`, usado pela capa, pelo login e pelo cadastro; quando chegar o oficial, a troca é só nesse arquivo.
- **Login e cadastro:** cartão branco centralizado sobre a moldura `src/components/FundoDecorado.tsx` (estrelas e viajantes nos cantos, numa camada fixa que não rola com o formulário).
- **Ilustrações:** as 22 do Manual estão em `src/assets/ilustracoes/`, em WebP (3,8 MB nos PNG originais, cerca de 0,6 MB em WebP). A capa usa a `pessoa-03`, a `pessoa-10` e a `estrela-grande-2`.

## Configuração

### Backend

As propriedades podem ser sobrescritas por variáveis de ambiente. Em produção, elas são definidas no painel do Render, no serviço da API.

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/portal` | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` | `portal` | Usuário do banco |
| `DATABASE_PASSWORD` | `portal` | Senha do banco |
| `PORT` | `8080` | Porta HTTP da aplicação |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Endereços de sites autorizados a chamar a API, separados por vírgula e sem barra no final |
| `JWT_SECRET` | segredo de desenvolvimento | Chave que assina os tokens. Em produção é obrigatória: a API não sobe no Render com o segredo de desenvolvimento |
| `STAFF_EMAILS` | vazio | E-mails do staff, separados por vírgula |

Sobre o staff:

- Cada pessoa do staff cria a conta pelo próprio site e depois tem o e-mail incluído em `STAFF_EMAILS`.
- A mudança vale depois que a pessoa sai e entra de novo.
- Contas do staff não entram em tripulação. Quem vai jogar como congressista não pode estar na lista.

### Frontend

| Variável | Onde é definida | Descrição |
| --- | --- | --- |
| `VITE_API_URL` | `frontend/.env.development` em desenvolvimento; painel do Render em produção | Endereço da API, sem barra no final |
| `NODE_VERSION` | Painel do Render | Versão do Node usada no build de produção |

O valor de `VITE_API_URL` é embutido no site durante o build. Para alterá-lo em produção, é preciso publicar o site de novo.

## Migrações de banco

O banco é configurado para validação do schema (`ddl-auto: validate`), então toda mudança de estrutura é uma migração Flyway em `backend/src/main/resources/db/migration`.

- Cada mudança é um arquivo novo, com dois sublinhados depois do número: `V7__descricao.sql`.
- Arquivo já aplicado nunca é editado. Uma correção é sempre uma migração nova.
- As migrações rodam sozinhas na inicialização, inclusive em produção.
- Uma EJ nova entra por migração nova, nunca editando a `V2`.

| Migração | Conteúdo |
| --- | --- |
| `V1__baseline.sql` | Confirma que o Flyway executa no banco |
| `V2__tabela_de_cadastro.sql` | Tabelas `ej` e `congressista` e as 54 EJs |
| `V3__cargo_do_congressista.sql` | Coluna `cargo` |
| `V4__tripulacoes.sql` | Tabela `tripulacao` e `congressista.tripulacao_id` |
| `V5__missoes.sql` | Tabela `missao` |
| `V6__conclusoes_e_pontos.sql` | `missao.vagas_usadas`, tabelas `conclusao` e `lancamento` |
| `V7__missoes_individuais.sql` | `missao.alcance` e a pessoa em `conclusao` e `lancamento` |
| `V8__foto_de_perfil.sql` | Tabela `foto_perfil` e `congressista.foto_chave` |
| `V9__cargos_trainee_e_projetista.sql` | Cargos `TRAINEE` e `PROJETISTA`; área de atuação vazia só para trainee |

Nos logs da API, a linha `now at version v9` (escrita depois de migrar) confirma que todas foram aplicadas. A linha `Current version of schema` é escrita antes e mostra a versão anterior.

As classes anotadas com `@Entity` precisam bater com as tabelas: se houver diferença, o backend não inicia.

## Regras do projeto

- **Vagas.** Vagas limitadas por missão são reservadas com um único `update` condicional no banco, nunca com leitura seguida de escrita. Se nenhuma linha muda, a missão está esgotada.
- **Uma conclusão por tripulação (ou pessoa) e missão.** Índices únicos parciais impedem duas conclusões pendentes ou aprovadas da mesma tripulação na mesma missão (`ux_conclusao_ativa`) e da mesma pessoa na mesma missão individual (`ux_conclusao_individual_ativa`). Depois de uma recusa, dá para enviar de novo.
- **Exclusão.** Uma missão só é excluída se ninguém respondeu nem pontuou; assim, nenhum ponto some do ranking.
- **Extrato de pontos.** A pontuação é um extrato de lançamentos, com no máximo um lançamento por conclusão. O total é sempre a soma do extrato, nunca um campo editável. Os pontos são copiados da missão no momento da aprovação: mudar os pontos da missão depois não altera o que já foi lançado.
- **Horários.** As janelas das missões são validadas no servidor. Os instantes são guardados em UTC e exibidos e digitados no horário de Brasília (UTC-3).
- **Rankings.** Os dois ficam em cache em memória por 15 segundos (`app.ranking.cache`). Empates dividem a posição. Um lançamento com a pessoa preenchida é de missão individual e conta só no ranking individual.
- **Tripulações.** A montagem e o encaixe de quem chega depois travam as tripulações (`select ... for update`), para que cadastros simultâneos não ocupem a mesma vaga. Uma EJ só se repete numa tripulação quando tem mais gente do que o número de tripulações.
- **Fotos de perfil.** O site recorta e reduz a foto para 256 px, em JPEG, antes de enviar; a API só aceita JPEG de até 100 KB. A imagem fica no banco (o disco do Render não é permanente), na tabela `foto_perfil`, uma por pessoa. A leitura é pública, porque a tag `<img>` não envia o token; quem protege a foto é a chave sorteada a cada envio, que forma o endereço. Como uma foto nova ganha outra chave, o navegador guarda a imagem por um ano, e a API mantém em memória as fotos já lidas.
- **Segurança.** A API responde com objetos próprios e nunca devolve senha nem hash. Senhas usam BCrypt, com no máximo 72 bytes.

## Operação do evento

- **Até 14/10:** cruzar os cadastros com a lista de inscritos na aba Pessoas e remover contas de teste e cadastros indevidos. O cruzamento só aponta: confira antes de remover, porque a pessoa pode ter usado outro e-mail.
- **15/10:** montar as tripulações em produção. A montagem roda **uma única vez**; não aperte o botão antes da hora.
- **Missões:** a organização cria as missões no painel. Uma missão sem respostas pode ser excluída; com respostas, só escondida.
- **Senha esquecida:** na aba Pessoas, "Nova senha" gera uma senha temporária para passar à pessoa. Depois de entrar, a pessoa pode trocá-la na aba Perfil, em "Trocar senha".
- **Remoção de cadastro:** só é possível para quem não enviou nem avaliou missão, não lançou pontos e não é capitão.
- **Foto inadequada:** na aba Pessoas, "Remover foto" tira a foto de alguém. A pessoa pode enviar outra depois.
- **Capitão:** depois de registrado, só ele envia as respostas das missões da tripulação. As individuais, cada um envia a sua.

## Testes

Com o banco local no ar, execute os testes do backend com:

```bash
cd backend
./mvnw test
```

São 75 testes, que sobem a aplicação inteira contra o banco local e, por isso, também aplicam as migrações pendentes. Os testes que gravam no banco usam `@Transactional` e são desfeitos no fim. Os relatórios ficam em `backend/target/surefire-reports/`.

O frontend não tem testes automatizados. A verificação dele é o lint e o build:

```bash
cd frontend
npm run lint
npm run build
```

## Fluxo de trabalho

- A `main` é só produção. Cada item tem uma branch curta, como `feat/aba-pessoas`.
- O merge é feito com `git merge --ff-only`, depois de `./mvnw test` (backend) ou de lint e build (frontend) passarem.
- Commits pequenos, com prefixo `feat:`, `fix:`, `chore:` ou `docs:`.
- A pasta `.vscode/` fica fora dos commits.
- O `npm audit` acusa vulnerabilidades na cadeia `braces` → `shadcn`. É ferramenta de build e não entra no site publicado. Não rode `npm audit fix --force`.
