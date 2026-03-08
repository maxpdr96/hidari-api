# HidariApi — API Tester CLI

## Project Overview
Spring Shell CLI for testing HTTP APIs from the terminal (Postman-like).
Built with Java 25, Spring Boot 3.5.5, Spring Shell 3.4.0, and java.net.http.HttpClient.

## Build & Run
```bash
mvn clean package           # Build
mvn spring-boot:run         # Run (dev)
java -jar target/hidari-api-1.0.0.jar  # Run (prod)
mvn compile                 # Compile only
mvn test                    # Run tests
```

## Architecture
- **shell/** — `@ShellComponent` classes: `ApiCommands` (HTTP/collections/history) + `MockCommands` (mock server)
- **service/** — `ApiService` (HttpClient + session), `MockServerService` (embedded HTTP server)
- **model/** — Records: ApiRequest, ApiResponse, SavedRequest, Collection, Environment, HttpMethod, MockRoute
- **store/** — JSON persistence in ~/.config/hidariapi/ (CollectionStore, HistoryStore, EnvironmentStore, MockStore)
- **util/** — JsonFormatter, CurlParser

## Key Patterns
- JLine `AttributedStyle` for terminal colors
- Java HttpClient (built-in, no external HTTP lib)
- Mock server uses `com.sun.net.httpserver.HttpServer` (built-in, virtual threads)
- Environment variables: `{{key}}` syntax resolved at send time
- Body from file: `--body @path/to/file.json` syntax
- Query params: `--param "key=value&key2=value2"` appended to URL
- History is FIFO with configurable max (default 200)
- Collections persist named requests for re-execution
- Default headers applied to every request
- Mock routes support path params: `/api/users/{id}` matches `/api/users/123`

## CLI Commands
### HTTP Requests
- `get/post/put/patch/delete <url> [--body JSON|@file] [--param key=value&...]`
- `head/options <url>`
- `send <METHOD> <url> [--header Key:Value;...] [--body text|@file] [--param ...]`
- `import-curl "<curl command>" [--save col:name] [--dry-run]`

### Response
- `response` — full last response (req + res + headers + body)
- `body` — just the body (pretty-printed if JSON)
- `response-headers` — response headers
- `curl` — cURL equivalent of last request
- `save-response <file>` — save body to file

### Headers
- `set-header <key> <value>` / `unset-header <key>` / `headers` / `clear-headers`
- `bearer <token>` — shortcut for Authorization: Bearer

### Environments (variable substitution)
- `env-create/env-rm <name>` — create/remove environment
- `env-set <env> <key> <value>` / `env-unset <env> <key>` — manage variables
- `env-use <name>` / `env-clear` — activate/deactivate
- `envs` / `env-show <name>` — list/show details

### Collections (saved requests)
- `col-create/col-rm <name>` — create/remove collection
- `col-add <collection> <request-name>` — save last request to collection
- `col-run <collection> <index>` — execute saved request
- `col-run-all <collection>` — execute all requests (smoke test)
- `col-show <name>` / `cols` — show/list collections
- `col-rm-req <collection> <index>` — remove request from collection

### Mock Server
- `mock-start [--port N]` — start mock server (default: 8089)
- `mock-stop` — stop mock server
- `mock-status` — show server status
- `mock-add <METHOD> <path> [--status N] [--body JSON|@file] [--header K:V;...] [--delay ms] [--desc text]`
- `mock-add-json <METHOD> <path> --body JSON|@file [--status N] [--desc text]` — quick JSON route
- `mock-add-crud <basePath> [--list-body JSON] [--item-body JSON]` — generates 5 CRUD routes
- `mock-from-response <path> [--method M]` — create route from last real response
- `mock-list` / `mock-show <index>` — list/show routes
- `mock-rm <index>` / `mock-clear` — remove routes
- `mock-logs [--limit N]` / `mock-clear-logs` — view received requests

### History
- `history [--limit N]` — show history
- `replay <index>` — re-execute from history
- `clear-history` — clear all history

## Persistence
All data stored in `~/.config/hidariapi/`:
- `collections.json` — saved request collections
- `history.json` — request history (max 200 entries)
- `environments.json` — environment variables
- `mocks.json` — mock server routes (persist between sessions)
