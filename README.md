# HidariApi

![Java](https://img.shields.io/badge/Java-25-%23ED8B00.svg?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-%236DB33F.svg?logo=spring&logoColor=white)
![Spring Shell](https://img.shields.io/badge/Spring%20Shell-CLI-blue)
![License](https://img.shields.io/badge/license-MIT-green)

Um testador de APIs direto no terminal. Parecido com o Postman, mas sem sair da linha de comando.

An API tester right in the terminal. Like Postman, but without leaving the command line.

Feito com Java 25, Spring Boot e Spring Shell.

Built with Java 25, Spring Boot and Spring Shell.

---

## Instalacao / Installation

Voce precisa ter o **Java 25** e o **Maven** instalados.

You need **Java 25** and **Maven** installed.

```bash
# clonar o projeto / clone the project
git clone <repo-url>
cd hidari-api

# compilar / build
mvn clean package

# rodar / run
java -jar target/hidari-api-1.0.0.jar

# ou, durante o desenvolvimento / or, during development
mvn spring-boot:run
```

Ao abrir, voce vai ver o prompt interativo. Digite `help` para ajuda nativa por comando (inclui `comando --help`) e `help-custom` para a ajuda traduzida.

When you open it, you'll see the interactive prompt. Type `help` for native per-command help (including `command --help`) and `help-custom` for translated help.

Voce pode trocar o idioma do CLI a qualquer momento:

You can switch CLI language at any time:

```bash
language pt
language eng
```

```
hidari-api:>
```

---

## Guia rapido / Quick start

### Fazendo seu primeiro request / Making your first request

```
hidari-api:> get https://jsonplaceholder.typicode.com/users/1

  200 OK  |  320ms  |  1.2 KB

  {
    "id": 1,
    "name": "Leanne Graham",
    "username": "Bret",
    "email": "Sincere@april.biz"
    ...
  }
```

Simples assim. Digitou `get` + a URL e pronto.

That simple. Type `get` + the URL and you're done.

---

## Todos os comandos / All commands

### Requests HTTP / HTTP Requests

Cada metodo HTTP tem seu proprio comando.

Each HTTP method has its own command.

| Comando / Command | O que faz / What it does |
|---------|-----------|
| `get <url> [--param ...] [--call N] [--parallel P] [--output file.json]` | Envia GET (simples ou em lote) / Sends GET (single or batch) |
| `post <url> --body ... [--param ...] [--call N] [--parallel P] [--output file.json]` | Envia POST com body JSON / Sends POST with JSON body |
| `put <url> --body ... [--param ...] [--call N] [--parallel P] [--output file.json]` | Envia PUT com body JSON / Sends PUT with JSON body |
| `patch <url> --body ... [--param ...] [--call N] [--parallel P] [--output file.json]` | Envia PATCH com body JSON / Sends PATCH with JSON body |
| `delete <url> [--param ...] [--call N] [--parallel P] [--output file.json]` | Envia DELETE / Sends DELETE |
| `head <url> [--call N] [--parallel P] [--output file.json]` | Envia HEAD (retorna so headers / returns headers only) |
| `options <url> [--call N] [--parallel P] [--output file.json]` | Envia OPTIONS / Sends OPTIONS |

#### Exemplos / Examples

```bash
# GET simples / simple GET
get https://api.github.com/users/octocat

# POST com body JSON / POST with JSON body
post https://jsonplaceholder.typicode.com/posts --body '{"title":"Meu post","body":"Conteudo","userId":1}'

# PUT para atualizar / PUT to update
put https://jsonplaceholder.typicode.com/posts/1 --body '{"title":"Titulo atualizado","body":"Novo conteudo","userId":1}'

# PATCH para atualizar parcialmente / PATCH to partially update
patch https://jsonplaceholder.typicode.com/posts/1 --body '{"title":"So o titulo mudou"}'

# DELETE
delete https://jsonplaceholder.typicode.com/posts/1
```

---

### Execucao em lote (--call), paralelo (--parallel) e export (--output)

Rode o mesmo request varias vezes com `--call`, controlando concorrencia com `--parallel`. Opcionalmente, salve **todas** as respostas em JSON com `--output`.

Run the same request multiple times with `--call`, controlling concurrency with `--parallel`. Optionally save **all** responses to JSON with `--output`.

```bash
# chama 10x / calls 10x
get https://api.com/health --call 10

# chama 10x com 5 em paralelo / calls 10x with 5 in parallel
get https://api.com/health --call 10 --parallel 5

# salva todas as chamadas em JSON / saves all calls to JSON
get https://api.com/health --call 10 --parallel 5 --output resultado.json

# caminho absoluto com @ / absolute path with @
get https://api.com/health --call 10 --parallel 5 --output @/home/user/resultados/batch.json
```

O resumo em tela inclui latencia `p50`, `p95`, `p99`, `min`, `max` e media.

The on-screen summary includes latency `p50`, `p95`, `p99`, `min`, `max` and average.

O JSON de `--output` inclui metadados e todas as respostas individuais com esses agregados.

The `--output` JSON includes metadata and all individual responses with those aggregates.

---

### Benchmark dedicado (bench)

Use `bench` para benchmark de carga com concorrencia e warmup.

Use `bench` for load benchmarking with concurrency and warmup.

```bash
bench https://api.com/health --calls 500 --concurrency 20 --warmup 30
bench https://api.com/users --method POST --body '{"name":"A"}' --calls 200 --concurrency 10
```

Saida inclui: sucesso/falha, RPS, avg/min/max e percentis (`p50`, `p95`, `p99`).

Output includes: success/failure, RPS, avg/min/max and percentiles (`p50`, `p95`, `p99`).

---

### Query params (--param)

Em vez de montar a URL na mao, use `--param`.

Instead of building the URL by hand, use `--param`.

```bash
# sem --param (funciona, mas fica feio) / without --param (works, but ugly)
get "https://api.com/users?page=1&limit=10&sort=name"

# com --param (mais limpo) / with --param (cleaner)
get https://api.com/users --param "page=1&limit=10&sort=name"

# funciona em qualquer metodo / works with any method
delete https://api.com/items --param "id=42"
post https://api.com/search --param "q=java" --body '{"filter":"recent"}'
```

---

### Body de arquivo (@arquivo) / Body from file (@file)

Para JSONs grandes, leia o body de um arquivo.

For large JSONs, read the body from a file.

```bash
# usa com @ no --body / use @ in --body
post https://api.com/users/batch --body @payload.json
put https://api.com/config --body @/home/user/config.json

# funciona no send tambem / works with send too
send PATCH https://api.com/settings --body @settings.json
```

---

### Request customizado (send) / Custom request (send)

Para controle total, use `send`.

For full control, use `send`.

```bash
send POST https://api.com/webhook \
  --header "X-API-Key:abc123;X-Custom:valor" \
  --body '{"event":"user.created"}' \
  --param "version=2"
```

- `--header`: formato `Chave:Valor`, separados por `;` / format `Key:Value`, separated by `;`
- `--body`: inline ou `@arquivo` / inline or `@file`
- `--param`: query params

---

### Importar cURL / Import cURL

Copiou um cURL do DevTools do browser? Cola direto.

Copied a cURL from the browser DevTools? Paste it directly.

```bash
# executa o cURL importado / executes the imported cURL
import-curl "curl -X POST https://api.com/login -H 'Content-Type: application/json' -d '{\"user\":\"admin\",\"pass\":\"123\"}'"

# apenas mostra o que seria enviado, sem executar / just shows what would be sent, without executing
import-curl "curl https://api.com/data -H 'Authorization: Bearer token123'" --dry-run

# importa e ja salva na colecao / imports and saves to a collection
import-curl "curl https://api.com/users" --save minha-api:listar-usuarios
```

O parser entende / The parser understands: `-X`, `-H`, `-d`/`--data`, `-u` (Basic Auth).

---

### Vendo a resposta / Viewing the response

Depois de qualquer request, voce tem varios comandos para inspecionar a resposta.

After any request, you have several commands to inspect the response.

```bash
# mostra tudo: request + response + headers + body / shows everything
response

# mostra apenas o body (JSON formatado bonito) / shows only the body (pretty JSON)
body

# mostra os headers da resposta / shows response headers
response-headers

# gera o comando cURL equivalente / generates the equivalent cURL command
curl

# salva o body em um arquivo / saves the body to a file
save-response /home/user/resposta.json
save-response output.json
```

---

### Headers padrao / Default headers

Headers que voce quer enviar em **todos** os requests.

Headers you want to send in **every** request.

```bash
# definir / set
set-header Content-Type application/json
set-header X-API-Key minha-chave-secreta

# ver todos / list all
headers

# atalho para Bearer token (muito usado) / shortcut for Bearer token (very common)
bearer eyJhbGciOiJIUzI1NiJ9.meu.token

# remover um / remove one
unset-header X-API-Key

# remover todos / remove all
clear-headers
```

Depois de configurar, todo request vai incluir esses headers automaticamente.

Once configured, every request will automatically include these headers.

---

### Ambientes (environments)

Ambientes permitem trocar variaveis sem mudar os requests. Ideal para alternar entre dev, staging e producao.

Environments let you switch variables without changing requests. Great for switching between dev, staging and production.

#### Criando e configurando / Creating and configuring

```bash
# criar ambientes / create environments
env-create dev
env-create prod

# definir variaveis / set variables
env-set dev base_url http://localhost:8080
env-set dev api_key chave-de-dev-123
env-set prod base_url https://api.meuapp.com
env-set prod api_key chave-de-prod-456

# ver ambientes / list environments
envs

# ver variaveis de um ambiente / show environment variables
env-show dev
```

#### Usando variaveis / Using variables

Ative um ambiente e use `{{variavel}}` nos requests.

Activate an environment and use `{{variable}}` in requests.

```bash
# ativar ambiente / activate environment
env-use dev

# agora {{base_url}} e {{api_key}} sao substituidos / now they are replaced automatically
get {{base_url}}/api/users
send GET {{base_url}}/api/config --header "X-API-Key:{{api_key}}"

# trocar para producao — mesmos requests, URLs diferentes
# switch to production — same requests, different URLs
env-use prod
get {{base_url}}/api/users

# desativar ambiente / deactivate environment
env-clear
```

#### Templates dinamicos (runtime) / Dynamic runtime templates

Voce pode usar templates em URL, headers e body:

You can use templates in URL, headers and body:

```bash
# runtime
{{$timestamp}}      # epoch ms
{{$isoTimestamp}}   # ISO-8601
{{$uuid}}           # UUID randomico
{{$cpf}}            # CPF valido randomico
{{$cnpj}}           # CNPJ valido randomico
{{$cep}}            # CEP randomico (8 digitos)
{{$phoneBr}}        # Telefone BR randomico (DDD + 9 + 8 digitos)
{{$fullNameBr}}     # Nome completo brasileiro randomico
{{$addressBr}}      # Endereco brasileiro randomico

# partes especificas BR (tambem funcionam com prefixo faker.)
{{phone_br.ddd}}
{{phone_br.number}}
{{full_name_br.first_name}}
{{full_name_br.middle_name}}
{{full_name_br.last_name}}
{{address_br.street}}
{{address_br.number}}
{{address_br.neighborhood}}
{{address_br.city}}
{{address_br.state}}
{{address_br.cep}}

# ambiente
{{base_url}}
{{env.base_url}}

# ultima resposta
{{last.status}}
{{last.header.content-type}}
{{last.body.user.id}}
```

#### Gerenciando / Managing

```bash
# remover variavel / remove variable
env-unset dev api_key

# remover ambiente inteiro / remove entire environment
env-rm staging
```

---

### Colecoes (collections)

Colecoes salvam requests para reusar depois. Como as Collections do Postman.

Collections save requests for later reuse. Like Postman Collections.

#### Fluxo basico / Basic flow

```bash
# 1. criar a colecao / create the collection
col-create minha-api

# 2. fazer um request / make a request
get https://api.com/users

# 3. salvar na colecao (usa o ultimo request) / save to collection (uses last request)
col-add minha-api listar-usuarios

# 4. fazer outro e salvar / make another and save
post https://api.com/users --body '{"name":"Joao"}'
col-add minha-api criar-usuario

# 5. ver o que tem na colecao / see what's in the collection
col-show minha-api
```

#### Executando / Running

```bash
# executar um request especifico pelo indice / run a specific request by index
col-run minha-api 1

# executar TODOS de uma vez (smoke test) / run ALL at once (smoke test)
col-run-all minha-api
```

### Aliases customizados

Crie atalhos de comando para acelerar o fluxo.

Create command shortcuts to speed up your workflow.

```bash
alias-set gh-health "get https://api.github.com/health --call 5 --parallel 2"
aliases
a gh-health
alias-run gh-health --args "--output health.json"
alias-rm gh-health
```

### Config centralizada (config + profiles)

Agora a ferramenta possui configuracao central em `config.json` (inclui `language`).

Now the tool has centralized configuration in `config.json` (including `language`).

```bash
# listar tudo
config-list

# linguagem (equivale ao comando language)
config-set language en
config-get language

# defaults de requests
config-set request.default-call 10
config-set request.default-parallel 5
config-set request.default-output @/home/user/default-batch.json

# profiles com base_url
profile-set-base-url dev http://localhost:8080
profile-use dev
profile-list

```

### Import de OpenAPI/Swagger e Postman

Importe especificacoes para gerar collections automaticamente e, no caso de OpenAPI, mocks iniciais.

Import specs to generate collections automatically and, for OpenAPI, initial mocks.

```bash
# OpenAPI/Swagger JSON -> collection + mocks
import-openapi @/home/user/openapi.json --collection pet-api --mocks true

# OpenAPI sem gerar mocks
import-openapi @/home/user/openapi.json --collection pet-api --mocks false

# Postman collection JSON -> collection
import-postman @/home/user/postman_collection.json --collection postman-api
```

O `col-run-all` mostra uma tabela com o resultado de cada request.

`col-run-all` shows a table with the result of each request.

```
  Resultado: minha-api

  #    METODO   STATUS NOME                           URL                                      TEMPO
  ---------------------------------------------------------------------------------------------------------
  + 1   GET      200    listar-usuarios                https://api.com/users                    120ms
  + 2   POST     201    criar-usuario                  https://api.com/users                    89ms

  2 passed, 0 failed  |  total: 209ms  |  2 request(s)
```

#### Gerenciando / Managing

```bash
# listar colecoes / list collections
cols

# remover request de uma colecao / remove request from a collection
col-rm-req minha-api 2

# remover colecao inteira / remove entire collection
col-rm minha-api
```

---

### Historico / History

Todo request que voce faz e salvo automaticamente no historico.

Every request you make is automatically saved to the history.

```bash
# ver os ultimos 20 requests / see the last 20 requests
history

# ver os ultimos 50 / see the last 50
history --limit 50

# re-executar um request do historico / re-execute a request from history
replay 3

# limpar historico / clear history
clear-history
```

---

### Mock Server

Crie APIs fake direto no terminal. Perfeito para:
- Testar o frontend sem backend pronto
- Simular APIs de terceiros
- Prototipar endpoints rapidamente

Create fake APIs right in the terminal. Perfect for:
- Testing the frontend without a ready backend
- Simulating third-party APIs
- Quickly prototyping endpoints

#### Iniciando / Starting

```bash
# iniciar na porta padrao (8089) / start on the default port (8089)
mock-start

# ou em outra porta / or on another port
mock-start --port 3000
```

#### Criando rotas / Creating routes

**Jeito mais rapido — CRUD completo de uma vez / Fastest way — full CRUD at once:**

```bash
# cria 5 rotas: GET (lista), GET (por id), POST, PUT, DELETE
# creates 5 routes: GET (list), GET (by id), POST, PUT, DELETE
mock-add-crud /api/users \
  --list-body '[{"id":1,"name":"Joao"},{"id":2,"name":"Maria"}]' \
  --item-body '{"id":1,"name":"Joao","email":"joao@email.com"}'
```

Isso cria / This creates:

| Metodo / Method | Path | Status |
|--------|------|--------|
| GET | /api/users | 200 |
| GET | /api/users/{id} | 200 |
| POST | /api/users | 201 |
| PUT | /api/users/{id} | 200 |
| DELETE | /api/users/{id} | 204 |

Depois e so chamar / Then just call:

```bash
# de dentro do hidari-api / from inside hidari-api
get http://localhost:8089/api/users
get http://localhost:8089/api/users/42

# ou de qualquer lugar (curl, browser, frontend, etc.)
# or from anywhere (curl, browser, frontend, etc.)
# curl http://localhost:8089/api/users
```

**Rota JSON rapida / Quick JSON route:**

```bash
mock-add-json GET /api/health --body '{"status":"ok","version":"1.0"}'
mock-add-json POST /api/login --body '{"token":"eyJhbGci...","expiresIn":3600}' --status 200
mock-add-json GET /api/products --body @produtos.json

# com timeout simulado (408 apos 2s) / with simulated timeout (408 after 2s)
mock-add-json GET /api/slow --body '{"error":"timeout"}' --delay 5000 --timeout-config 2

# rota stateful por status / stateful status sequence
mock-add-json GET /api/state --body '{"ok":true}' --scenario 500,500,200
```

**Rota completa (com headers, delay, etc.) / Full route (with headers, delay, etc.):**

```bash
# simular endpoint lento / simulate slow endpoint
mock-add POST /api/webhook --status 202 --body '{"queued":true}' --delay 2000 --desc "Webhook lento"

# simular erro / simulate error
mock-add GET /api/error --status 500 --body '{"error":"Internal Server Error"}'

# com headers customizados / with custom headers
mock-add GET /api/data --status 200 --body '{"data":[]}' --header "X-RateLimit:100;X-Request-Id:abc"

# timeout por rota (segundos) / per-route timeout (seconds)
mock-add GET /api/timeout --delay 5000 --timeout-config 2 --body '{"error":"Request Timeout"}'

# cenario stateful / stateful scenario
mock-add GET /api/order --body '{"order":"created"}' --scenario 500,200
```

**Criar mock a partir de uma resposta real / Create mock from a real response:**

```bash
# 1. faz um request real / make a real request
get https://api.github.com/users/octocat

# 2. cria uma rota mock com a mesma resposta / create a mock route with the same response
mock-from-response /api/github-user

# agora http://localhost:8089/api/github-user retorna o mesmo JSON do GitHub
# now http://localhost:8089/api/github-user returns the same JSON from GitHub
```

#### Path params dinamicos / Dynamic path params

Use `{param}` no path — qualquer valor bate.

Use `{param}` in the path — any value matches.

```bash
mock-add-json GET /api/users/{id} --body '{"id":1,"name":"Joao"}'

# todos esses vao funcionar / all of these will work:
# GET /api/users/1
# GET /api/users/42
# GET /api/users/abc
```

#### Templates dinamicos no mock / Dynamic templates in mock responses

No body/headers do mock, voce pode usar:

Inside mock body/headers, you can use:

```bash
{{param.id}}      # path param
{{query.page}}    # query param
{{$timestamp}}
{{$isoTimestamp}}
{{$uuid}}
{{$cpf}}
{{faker.uuid}}
{{faker.timestamp}}
{{faker.int}}
{{faker.bool}}
{{faker.word}}
{{faker.cpf}}
{{faker.cnpj}}
{{faker.cep}}
{{faker.phone_br}}
{{faker.full_name_br}}
{{faker.address_br}}
{{faker.phone_br.ddd}}
{{faker.phone_br.number}}
{{faker.full_name_br.first_name}}
{{faker.full_name_br.middle_name}}
{{faker.full_name_br.last_name}}
{{faker.address_br.street}}
{{faker.address_br.number}}
{{faker.address_br.neighborhood}}
{{faker.address_br.city}}
{{faker.address_br.state}}
{{faker.address_br.cep}}
```

Exemplo:

```bash
mock-add-json GET /api/users/{id} \
  --body '{"id":"{{param.id}}","cpf":"{{faker.cpf}}","cnpj":"{{faker.cnpj}}","cep":"{{faker.cep}}","phone":"{{faker.phone_br}}","name":"{{faker.full_name_br}}","address":"{{faker.address_br}}"}'
```

#### Editando rotas / Editing routes

Use `mock-edit` para alterar qualquer campo de uma rota existente. So informe o que quer mudar — o resto continua igual.

Use `mock-edit` to change any field of an existing route. Only provide what you want to change — the rest stays the same.

```bash
# primeiro, veja as rotas / first, see the routes
mock-list

# mudar so o status / change only the status
mock-edit 3 --status 404

# mudar so o body (de arquivo) / change only the body (from file)
mock-edit 3 --body @/home/user/novo-body.json

# mudar so o body (inline) / change only the body (inline)
mock-edit 1 --body '{"error":"Not Found","message":"Recurso nao encontrado"}'

# mudar status e body juntos / change status and body together
mock-edit 2 --status 500 --body '{"error":"Internal Server Error"}'

# adicionar delay / add delay
mock-edit 1 --delay 2000

# configurar timeout simulado / set simulated timeout
mock-edit 1 --timeout-config 2

# configurar/alterar cenario stateful / set/update stateful scenario
mock-edit 1 --scenario 500,200

# mudar descricao / change description
mock-edit 4 --desc "Rota de teste atualizada"

# mudar metodo ou path / change method or path
mock-edit 5 --method POST --path /api/v2/users

# mudar tudo de uma vez / change everything at once
mock-edit 1 --status 201 --body '{"created":true}' --delay 100 --header "X-Custom:valor" --desc "Criacao com delay"
```

**Flags disponiveis no mock-edit / Available flags in mock-edit:**

| Flag | O que faz / What it does |
|------|-----------|
| `--status <N>` | Altera o status code / Changes the status code |
| `--body <JSON\|@file>` | Altera o body (inline ou arquivo) / Changes the body (inline or file) |
| `--header <K:V;...>` | Adiciona/atualiza headers / Adds/updates headers |
| `--delay <ms>` | Altera o delay em ms / Changes the delay in ms |
| `--timeout-config <s>` | Simula timeout e responde 408 se o tempo ultrapassar N segundos / Simulates timeout and returns 408 if elapsed time exceeds N seconds |
| `--scenario <s1,s2,...>` | Sequencia de status stateful por chamada / Stateful status sequence per call |
| `--desc <texto>` | Altera a descricao / Changes the description |
| `--method <METHOD>` | Altera o metodo HTTP / Changes the HTTP method |
| `--path <path>` | Altera o path / Changes the path |

#### Vendo rotas e logs / Viewing routes and logs

```bash
# listar todas as rotas / list all routes
mock-list

# ver detalhes de uma rota (indice da lista) / see route details (list index)
mock-show 1

# ver requests recebidos pelo mock / see requests received by the mock
mock-logs

# ver mais logs / see more logs
mock-logs --limit 50

# limpar logs / clear logs
mock-clear-logs
```

A tabela de logs mostra hora, metodo, status, path e se bateu com alguma rota (OK) ou nao (MISS).

The logs table shows time, method, status, path and whether it matched a route (OK) or not (MISS).

#### Gerenciando / Managing

```bash
# remover uma rota pelo indice / remove a route by index
mock-rm 3

# remover todas as rotas / remove all routes
mock-clear

# parar o servidor / stop the server
mock-stop

# ver status / check status
mock-status
```

#### Persistencia / Persistence

As rotas ficam salvas no diretorio de configuracao da aplicacao. Quando voce abre o hidari-api de novo, basta dar `mock-start` e todas as rotas anteriores ja estao la.

Routes are saved in the app configuration directory. When you open hidari-api again, just run `mock-start` and all previous routes are already there.

#### CORS

O mock server ja vem com CORS habilitado. Pode chamar de qualquer frontend no browser sem problemas.

The mock server comes with CORS enabled. You can call it from any frontend in the browser without issues.

---

## Exemplos do dia a dia / Everyday examples

### Testar uma API REST completa / Test a complete REST API

```bash
# configurar ambiente / set up environment
env-create local
env-set local url http://localhost:8080
env-use local

# configurar autenticacao / set up authentication
bearer meu-token-jwt

# testar endpoints / test endpoints
get {{url}}/api/users
post {{url}}/api/users --body '{"name":"Novo Usuario","email":"novo@email.com"}'
get {{url}}/api/users/1
put {{url}}/api/users/1 --body '{"name":"Nome Atualizado"}'
delete {{url}}/api/users/1

# ver historico do que fez / see history of what you did
history
```

### Montar uma colecao de smoke test / Build a smoke test collection

```bash
# criar colecao / create collection
col-create smoke-test

# fazer requests e salvar / make requests and save
get https://api.com/health
col-add smoke-test health-check

get https://api.com/api/users
col-add smoke-test listar-usuarios

post https://api.com/api/auth/login --body '{"user":"test","pass":"test"}'
col-add smoke-test login

# rodar tudo de uma vez / run all at once
col-run-all smoke-test
```

### Prototipar frontend com mock / Prototype frontend with mock

```bash
# subir mock server / start mock server
mock-start --port 3000

# criar API fake completa / create complete fake API
mock-add-crud /api/products \
  --list-body '[{"id":1,"name":"Notebook","price":4500},{"id":2,"name":"Mouse","price":120}]' \
  --item-body '{"id":1,"name":"Notebook","price":4500,"description":"Notebook gamer"}'

mock-add-json POST /api/auth/login --body '{"token":"fake-jwt-token","user":{"id":1,"name":"Admin"}}'
mock-add-json GET /api/auth/me --body '{"id":1,"name":"Admin","role":"admin"}'

# pronto! frontend pode chamar http://localhost:3000/api/...
# done! frontend can call http://localhost:3000/api/...

# depois, ajustar uma rota sem recriar tudo / later, adjust a route without recreating everything
mock-edit 1 --body '[{"id":1,"name":"Notebook","price":3999,"onSale":true}]'
```

### Importar cURL do browser / Import cURL from the browser

```bash
# copiou do DevTools do Chrome? cola aqui / copied from Chrome DevTools? paste here
import-curl "curl 'https://api.com/data' -H 'accept: application/json' -H 'authorization: Bearer xyz123'"

# salvar na colecao ao importar / save to collection when importing
import-curl "curl -X POST https://api.com/webhook -d '{\"event\":\"test\"}'" --save webhooks:teste

# so ver o que seria enviado / just see what would be sent
import-curl "curl https://api.com/users -H 'X-Custom: valor'" --dry-run
```

### Salvar respostas / Save responses

```bash
# fazer request / make request
get https://api.com/reports/monthly

# salvar o JSON retornado / save the returned JSON
save-response relatorio-mensal.json

# salvar em caminho completo / save to full path
save-response /home/user/dados/resposta.json
```

### Clonar API real para mock / Clone real API to mock

```bash
# subir mock / start mock
mock-start

# chamar a API real e clonar cada endpoint / call the real API and clone each endpoint
get https://api.github.com/users/octocat
mock-from-response /api/users/octocat

get https://api.github.com/users/octocat/repos
mock-from-response /api/users/octocat/repos

# agora voce tem uma copia local da API / now you have a local copy of the API
get http://localhost:8089/api/users/octocat

# ajustar as respostas como quiser / adjust the responses as you wish
mock-edit 1 --body '{"login":"meu-user","name":"Meu Nome"}'
```

---

## Onde os dados ficam salvos / Where data is stored

Diretorio padrao por sistema:

Default directory per system:

- Linux/macOS: `~/.config/hidariapi/` (ou `$XDG_CONFIG_HOME/hidariapi`)
- Windows: `%APPDATA%\\hidariapi`
- Override (todos os SOs): `HIDARIAPI_CONFIG_DIR=/caminho/custom`

| Arquivo / File | Conteudo / Content |
|---------|----------|
| `collections.json` | Colecoes de requests salvos / Saved request collections |
| `history.json` | Historico dos ultimos 200 requests / History of the last 200 requests |
| `environments.json` | Ambientes e variaveis / Environments and variables |
| `mocks.json` | Rotas do mock server / Mock server routes |

Sao arquivos JSON simples — pode editar manualmente se quiser.

They are simple JSON files — you can edit them manually if you want.

---

## Referencia rapida de comandos / Quick command reference

### Requests HTTP

| Comando / Command | Descricao / Description |
|---------|-----------|
| `get <url> [--param ...] [--call N] [--parallel P] [--output file]` | GET request |
| `post <url> [--body ...] [--param ...] [--call N] [--parallel P] [--output file]` | POST request |
| `put <url> [--body ...] [--param ...] [--call N] [--parallel P] [--output file]` | PUT request |
| `patch <url> [--body ...] [--param ...] [--call N] [--parallel P] [--output file]` | PATCH request |
| `delete <url> [--param ...] [--call N] [--parallel P] [--output file]` | DELETE request |
| `head <url> [--call N] [--parallel P] [--output file]` | HEAD request |
| `options <url> [--call N] [--parallel P] [--output file]` | OPTIONS request |
| `send <METHOD> <url> [--header ...] [--body ...] [--param ...] [--call N] [--parallel P] [--output file]` | Request customizado / Custom request |
| `bench <url> [--method] [--header] [--body] [--calls] [--concurrency] [--warmup]` | Benchmark dedicado / Dedicated benchmark |
| `import-curl "<cmd>" [--save col:name] [--dry-run]` | Importar cURL / Import cURL |
| `import-openapi <file> [--collection] [--base-url] [--mocks]` | Importar OpenAPI/Swagger / Import OpenAPI/Swagger |
| `import-postman <file> [--collection]` | Importar collection Postman / Import Postman collection |
| `alias-set <name> "<command>"` | Criar/atualizar alias / Create/update alias |
| `alias-run <name> [--args "..."]` | Executar alias / Execute alias |
| `aliases / alias-rm <name>` | Listar/remover aliases / List/remove aliases |
| `config-set <key> <value> / config-get <key> / config-list` | Configuracao central / Central config |
| `profile-use <name> / profile-set-base-url <p> <url> / profile-list` | Profiles de config / Config profiles |

### Resposta / Response

| Comando / Command | Descricao / Description |
|---------|-----------|
| `response` | Resposta completa / Full response |
| `body` | Apenas o body / Body only |
| `response-headers` | Headers da resposta / Response headers |
| `curl` | Gerar cURL / Generate cURL |
| `save-response <file>` | Salvar body em arquivo / Save body to file |

### Headers

| Comando / Command | Descricao / Description |
|---------|-----------|
| `set-header <key> <value>` | Definir header padrao / Set default header |
| `unset-header <key>` | Remover header / Remove header |
| `headers` | Listar headers / List headers |
| `clear-headers` | Limpar todos / Clear all |
| `bearer <token>` | Definir Bearer token / Set Bearer token |

### Ambientes / Environments

| Comando / Command | Descricao / Description |
|---------|-----------|
| `env-create <name>` | Criar ambiente / Create environment |
| `env-set <env> <key> <value>` | Definir variavel / Set variable |
| `env-unset <env> <key>` | Remover variavel / Remove variable |
| `env-use <name>` | Ativar ambiente / Activate environment |
| `env-clear` | Desativar ambiente / Deactivate environment |
| `envs` | Listar ambientes / List environments |
| `env-show <name>` | Ver variaveis / Show variables |
| `env-rm <name>` | Remover ambiente / Remove environment |

### Colecoes / Collections

| Comando / Command | Descricao / Description |
|---------|-----------|
| `col-create <name>` | Criar colecao / Create collection |
| `col-add <col> <req-name>` | Salvar ultimo request / Save last request |
| `col-run <col> <index>` | Executar request / Run request |
| `col-run-all <col>` | Executar todos (smoke test) / Run all (smoke test) |
| `col-show <name>` | Ver requests / Show requests |
| `cols` | Listar colecoes / List collections |
| `col-rm-req <col> <index>` | Remover request / Remove request |
| `col-rm <name>` | Remover colecao / Remove collection |

### Mock Server

| Comando / Command | Descricao / Description |
|---------|-----------|
| `mock-start [--port N]` | Iniciar servidor (padrao: 8089) / Start server (default: 8089) |
| `mock-stop` | Parar servidor / Stop server |
| `mock-status` | Ver status / Show status |
| `mock-add <M> <path> [--status] [--body] [--header] [--delay] [--timeout-config] [--scenario] [--desc]` | Adicionar rota / Add route |
| `mock-add-json <M> <path> --body ... [--status] [--timeout-config] [--scenario] [--desc]` | Rota JSON rapida / Quick JSON route |
| `mock-add-crud <path> [--list-body] [--item-body]` | CRUD completo (5 rotas) / Full CRUD (5 routes) |
| `mock-from-response <path> [--method]` | Criar de resposta real / Create from real response |
| `mock-edit <index> [--status] [--body] [--header] [--delay] [--timeout-config] [--scenario] [--desc] [--method] [--path]` | Editar rota / Edit route |
| `mock-list` | Listar rotas / List routes |
| `mock-show <index>` | Ver detalhes da rota / Show route details |
| `mock-rm <index>` | Remover rota / Remove route |
| `mock-clear` | Remover todas / Remove all |
| `mock-logs [--limit N]` | Ver requests recebidos / View received requests |
| `mock-clear-logs` | Limpar logs / Clear logs |

### Historico / History

| Comando / Command | Descricao / Description |
|---------|-----------|
| `history [--limit N]` | Ver historico / Show history |
| `replay <index>` | Re-executar request / Re-execute request |
| `clear-history` | Limpar historico / Clear history |

---

## Tecnologias / Technologies

| Tecnologia / Technology | Uso / Usage |
|------------|-----|
| Java 25 | Linguagem / Language |
| Spring Boot 3.5.5 | Framework base |
| Spring Shell 3.4.0 | CLI interativo (REPL) / Interactive CLI (REPL) |
| java.net.http.HttpClient | Requests HTTP (built-in) |
| com.sun.net.httpserver | Mock server (built-in) |
| Jackson | JSON serialization |
| JLine | Cores no terminal / Terminal colors |
