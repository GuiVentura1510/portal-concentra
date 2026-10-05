# Portal Concentra

Portal de gamificação do Concentra DF 2026.

## Estado atual

O projeto contém o backend inicial da aplicação, desenvolvido com:

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA e Hibernate
- PostgreSQL 17
- Flyway para migrações
- Spring Security com sessões stateless
- Actuator para monitoramento

O banco é configurado para validação do schema (`ddl-auto: validate`). A migração inicial está em `backend/src/main/resources/db/migration/V1__baseline.sql`.

## Pré-requisitos

- JDK 21
- Docker e Docker Compose

## Executando localmente

1. Inicie o PostgreSQL:

	```bash
	docker compose up -d db
	```

2. Inicie o backend:

	```bash
	cd backend
	./mvnw spring-boot:run
	```

Por padrão, a aplicação inicia em `http://localhost:8080`. O Flyway executa as migrações automaticamente na inicialização.

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

As propriedades podem ser sobrescritas por variáveis de ambiente:

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/portal` | URL JDBC do PostgreSQL |
| `DATABASE_USERNAME` | `portal` | Usuário do banco |
| `DATABASE_PASSWORD` | `portal` | Senha do banco |
| `PORT` | `8080` | Porta HTTP da aplicação |

## Testes

Execute os testes do backend com:

```bash
cd backend
./mvnw test
```
