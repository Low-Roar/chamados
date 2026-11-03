# Sistema de Chamados – API REST em camadas

Spring Boot 3 + Spring Data JPA + Bean Validation + H2 em memória (recriado a cada reinício).
Arquitetura: Controller → Service → Repository → Entity (+ DTOs e Exceptions).

## Executar
    mvn spring-boot:run      # ou ./mvnw spring-boot:run
    mvn test                 # roda todos os testes (Repository, Controller e integração)

H2 Console: http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:chamadosdb`, usuário `sa`, senha em branco.

## Rotas

| Recurso | Método | Rota | Sucesso |
|---|---|---|---|
| Clientes | POST / GET / GET{id} / PUT{id} / DELETE{id} | `/clientes` | 201 / 200 / 200 / 200 / 204 |
| Clientes | GET | `/clientes/{id}/chamados` | 200 (paginado) |
| Categorias | POST / GET / GET{id} / PUT{id} / DELETE{id} | `/categorias` | 201 / 200 / 200 / 200 / 204 |
| Técnicos | POST / GET / GET{id} / PUT{id} / DELETE{id} | `/tecnicos` | 201 / 200 / 200 / 200 / 204 |
| Chamados | POST / GET / GET{id} / PUT{id} / DELETE{id} | `/chamados` | 201 / 200 (paginado) / 200 (com comentários) / 200 / 204 |
| Chamados | PATCH | `/chamados/{id}/atribuir` | 200 |
| Chamados | PATCH | `/chamados/{id}/status` | 200 |
| Chamados | POST | `/chamados/{id}/comentarios` | 201 |

Erros: **400** validação/JSON/parâmetro inválido · **404** não encontrado · **409** regra de negócio.

## Regras de negócio
- Cliente: e-mail único. Categoria: nome único (sem diferenciar maiúsculas). Técnico: e-mail único.
- Cliente, categoria e técnico com chamados vinculados não podem ser excluídos (409).
- Chamado nasce `ABERTO` e `dataAbertura` é preenchida automaticamente.
- Chamado `FECHADO` não pode ser alterado nem receber técnico (409); ao fechar, `dataFechamento` é preenchida.
- Transições de status válidas:
  - `ABERTO` → `EM_ATENDIMENTO` | `FECHADO`
  - `EM_ATENDIMENTO` → `AGUARDANDO_CLIENTE` | `FECHADO`
  - `AGUARDANDO_CLIENTE` → `EM_ATENDIMENTO` | `FECHADO`
  - `FECHADO` → nenhuma
- Enums: `Prioridade` (BAIXA, MEDIA, ALTA, CRITICA), `StatusChamado`, `Perfil` (ADMIN, TECNICO).

## Exemplos de requisições (curl)

    # categoria e técnico
    curl -i -X POST http://localhost:8080/categorias -H "Content-Type: application/json" \
      -d '{"nome":"Rede","descricao":"Problemas de rede"}'
    curl -i -X POST http://localhost:8080/tecnicos -H "Content-Type: application/json" \
      -d '{"nome":"Carlos","email":"carlos@empresa.com","perfil":"TECNICO"}'

    # abrir chamado (precisa de um cliente, categoria e técnico já cadastrados)
    curl -i -X POST http://localhost:8080/chamados -H "Content-Type: application/json" \
      -d '{"titulo":"Sem internet","descricao":"A rede caiu","prioridade":"ALTA","clienteId":1,"categoriaId":1}'

    # atribuir técnico e mudar status
    curl -i -X PATCH http://localhost:8080/chamados/1/atribuir -H "Content-Type: application/json" -d '{"tecnicoId":1}'
    curl -i -X PATCH http://localhost:8080/chamados/1/status   -H "Content-Type: application/json" -d '{"status":"EM_ATENDIMENTO"}'

    # comentar e ver o detalhe (com comentários)
    curl -i -X POST http://localhost:8080/chamados/1/comentarios -H "Content-Type: application/json" \
      -d '{"texto":"Verificando o roteador","autor":"Carlos"}'
    curl -i http://localhost:8080/chamados/1

## Filtros, paginação e ordenação (GET /chamados)

Parâmetros opcionais e combináveis: `status`, `prioridade`, `clienteId`, `page` (começa em 0), `size` (padrão 10), `sort`.
Padrão de ordenação: `dataAbertura` decrescente.

    curl -i "http://localhost:8080/chamados?status=ABERTO"
    curl -i "http://localhost:8080/chamados?prioridade=CRITICA&status=EM_ATENDIMENTO"
    curl -i "http://localhost:8080/chamados?clienteId=1&prioridade=ALTA"
    curl -i "http://localhost:8080/chamados?page=0&size=5&sort=dataAbertura,desc"
    curl -i "http://localhost:8080/chamados?page=1&size=2&sort=titulo,asc"
    curl -i "http://localhost:8080/clientes/1/chamados?page=0&size=10"

Resposta paginada:

    { "conteudo": [ ... ], "pagina": 0, "tamanho": 10, "totalElementos": 2, "totalPaginas": 1 }
