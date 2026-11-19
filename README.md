# Mobilis — Global Solution 2026.2

Plataforma de comodato de equipamentos assistivos (cadeiras de rodas, muletas, andadores e afins) para ONGs e cidadãos em vulnerabilidade temporária de mobilidade.

**Tema:** CitizenTech — Tecnologia centrada nas necessidades reais do cidadão  
**Disciplina:** Microservice and Web Engineering & IT Services  
**Stack:** Java 25, Spring Boot 4.0.9, Spring Cloud, MySQL, Eureka, API Gateway, OpenFeign, Load Balancer, Circuit Breaker

## Integrantes

| Nome | RM     |
|------|--------|
|   Graziely Tavares da Silva   | 559111 |
|   Mariana Pacheco Torres   | 558956 |
|   Mayara Santos Pereira Tavares   | 559031 |

## Problema e solução

Cidadãos enfrentam filas no SUS e custos altos na locação privada de equipamentos de apoio, enquanto ONGs e igrejas mantêm inventário ocioso sem visibilidade digital. O **Mobilis** conecta instituições e solicitantes: o inventário controla disponibilidade e o comodato reserva, empresta e devolve o equipamento via comunicação síncrona entre microsserviços.

## Arquitetura

| Componente | Porta | Descrição |
|------------|-------|-----------|
| `eureka-server` | 8761 | Service discovery |
| `api-gateway` | 8082 | Roteamento + Swagger dinâmico |
| `ms-inventario` | aleatória (`0`) | Instituições e equipamentos (`db_inventario`) |
| `ms-comodato` | aleatória (`0`) | Comodatos e fluxo síncrono (`db_comodato`) |
| MySQL inventário | 3306 | Banco do `ms-inventario` |
| MySQL comodato | 3307 | Banco do `ms-comodato` |

Fluxo síncrono (OpenFeign + Load Balancer + Circuit Breaker + Fallback):

1. `POST /api/v1/comodatos` → reserva equipamento (`DISPONIVEL` → `RESERVADO`)
2. `PATCH /api/v1/comodatos/{id}/retirada` → `EMPRESTADO`
3. `PATCH /api/v1/comodatos/{id}/devolucao` → `DISPONIVEL`
4. `PATCH /api/v1/comodatos/{id}/cancelamento` → `DISPONIVEL`

## Pré-requisitos

- JDK **25**
- Maven Wrapper (incluso em cada módulo) ou Maven 3.9+
- Docker Desktop

## 1. Subir o MySQL

Na raiz do repositório:

```bash
docker compose up -d
```

Credenciais (iguais nos dois bancos):

- usuário: `mobilis`
- senha: `mobilis`
- bancos: `db_inventario` (porta 3306) e `db_comodato` (porta 3307)

## 2. Subir os serviços (ordem obrigatória)

Em terminais separados, na pasta de cada módulo:

```bash
cd eureka-server
./mvnw spring-boot:run
```

```bash
cd api-gateway
./mvnw spring-boot:run
```

```bash
cd ms-inventario
./mvnw spring-boot:run
```

```bash
cd ms-comodato
./mvnw spring-boot:run
```

No Windows PowerShell use `.\mvnw.cmd spring-boot:run`.

No IntelliJ IDEA: abra cada módulo e execute a classe `*Application`.

Aguarde o registro no Eureka: [http://localhost:8761](http://localhost:8761)

## 3. URLs úteis

- Eureka: http://localhost:8761
- Swagger (Gateway): http://localhost:8082/swagger-ui/index.html
- Inventário via Gateway: http://localhost:8082/ms-inventario/api/v1/equipamentos
- Comodato via Gateway: http://localhost:8082/ms-comodato/api/v1/comodatos

## 4. Exemplos de teste

Listar equipamentos:

```bash
curl http://localhost:8082/ms-inventario/api/v1/equipamentos
```

Criar comodato (reserva o equipamento 1):

```bash
curl -X POST http://localhost:8082/ms-comodato/api/v1/comodatos ^
  -H "Content-Type: application/json" ^
  -d "{\"nomeSolicitante\":\"Ana Costa\",\"documentoSolicitante\":\"12345678901\",\"equipamentoId\":1,\"dataReserva\":\"2026-11-15\",\"prazoDevolucao\":\"2026-12-15\",\"consentimentoLgpd\":true}"
```

Retirada / devolução / cancelamento:

```bash
curl -X PATCH http://localhost:8082/ms-comodato/api/v1/comodatos/1/retirada
curl -X PATCH http://localhost:8082/ms-comodato/api/v1/comodatos/1/devolucao
curl -X PATCH http://localhost:8082/ms-comodato/api/v1/comodatos/3/cancelamento
```

## 5. Testes automatizados

Cada microsserviço usa profile `test` com H2 (sem Docker). Unitários (`*Test`) e integração (`*IT`):

```bash
cd ms-inventario
./mvnw verify

cd ../ms-comodato
./mvnw verify
```

## Estrutura do monorepo

```text
GS2-2026/
  docker-compose.yml
  eureka-server/
  api-gateway/
  ms-inventario/
  ms-comodato/
  README.md
  entrega.txt
```
