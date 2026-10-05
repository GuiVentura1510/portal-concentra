# Portal Concentra

Portal de gamificação do Concentra DF 2026, evento da Concentro nos dias 17 e 18/10/2026. A gamificação se chama **A Expedição da Estrela Polar**.

## Estado atual

O caminho completo até a produção está funcionando: o site publicado chama a API, que consulta o banco. Ainda não há funcionalidades de negócio.

- [x] Backend com Postgres, Flyway e endpoint de verificação
- [x] Frontend com tela de verificação
- [x] Deploy automático do backend e do frontend
- [ ] Tailwind, shadcn/ui e manifesto do PWA
- [ ] Cadastro, login, questionário e perfil
- [ ] Tripulações
- [ ] Missões, pontuação e ranking
- [ ] Telas do staff

## Stack

**Backend**

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA e Hibernate
- PostgreSQL 17
- Flyway para migrações
- Spring Security com sessões stateless
- Actuator para monitoramento

**Frontend**

- React com TypeScript
- Vite

## Estrutura

```
portal-concentra/
├── backend/        API Spring Boot e Dockerfile
├── frontend/       Aplicação React
├── compose.yaml    Postgres local para desenvolvimento
└── README.md
```

## Produção

| Serviço | Endereço | Hospedagem |
| --- | --- | --- |
| Site | https://portal-concentra-web.onrender.com | Render, Static Site |
| API | https://portal-concentra-api.onrender.com | Render, Web Service com Docker |
| Banco | acesso interno | Render Postgres 17 |

Todo `git push` na branch `main` publica sozinho. Mudanças em `backend/` republicam a API e mudanças em `frontend/` republicam o site.

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

3. Em outro terminal, inicie o frontend:

	```bash
	cd frontend
	npm install
	npm run dev
	```

O backend sobe em `http://localhost:8080` e o frontend em `http://localhost:5173`. O Flyway executa as migrações automaticamente quando o backend inicia.

Para parar o banco:

```bash
docker compose down
```

Os dados ficam no volume Docker `pgdata`. Para removê-los também, use `docker compose down -v`.

## Endpoints disponíveis

Os endpoints abaixo não exigem autenticação:

```bash
curl http://localhost:8080/api/ping
curl http://localhost:8080/actuator/health
```

Uma resposta esperada para o ping é:

```json
{"status":"ok","banco":"portal"}
```

O endpoint `/api/ping` consulta o banco de dados atual, portanto também funciona como verificação da conexão com o PostgreSQL. As demais rotas exigem autenticação.

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

### Frontend

| Variável | Onde é definida | Descrição |
| --- | --- | --- |
| `VITE_API_URL` | `frontend/.env.development` em desenvolvimento; painel do Render em produção | Endereço da API, sem barra no final |
| `NODE_VERSION` | Painel do Render | Versão do Node usada no build de produção |

O valor de `VITE_API_URL` é embutido no site durante o build. Para alterá-lo em produção, é preciso publicar o site de novo.

## Migrações de banco

O banco é configurado para validação do schema (`ddl-auto: validate`), então toda mudança de estrutura é uma migração Flyway em `backend/src/main/resources/db/migration`.

- Cada mudança é um arquivo novo: `V2__descricao.sql`, `V3__descricao.sql` e assim por diante, com dois sublinhados depois do número.
- Arquivo já aplicado nunca é editado. Uma correção é sempre uma migração nova.
- As migrações rodam sozinhas na inicialização, inclusive em produção.

## Regras do projeto

- Vagas limitadas por missão são reservadas com operação atômica no banco, nunca com leitura seguida de escrita.
- Existe restrição única por tripulação e missão, para que um reenvio não pontue duas vezes.
- A pontuação é um extrato de lançamentos. O total é sempre a soma do extrato, nunca um campo editável.
- As janelas de horário das missões são validadas no servidor. Os instantes são guardados em UTC e exibidos no horário de Brasília.
- O ranking fica em cache em memória por 10 a 15 segundos.

## Testes

Com o banco local no ar, execute os testes do backend com:

```bash
cd backend
./mvnw test
```