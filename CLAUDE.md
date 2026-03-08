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
- **shell/** — `@ShellComponent` with `@ShellMethod` commands (user-facing CLI)
- **service/** — `ApiService` with HttpClient, session state, orchestration
- **model/** — Records: ApiRequest, ApiResponse, SavedRequest, Collection, Environment, HttpMethod
- **store/** — JSON persistence in ~/.config/hidariapi/ (CollectionStore, HistoryStore, EnvironmentStore)
- **util/** — JsonFormatter for pretty-printing

## Key Patterns
- JLine `AttributedStyle` for terminal colors
- Java HttpClient (built-in, no external HTTP lib)
- Environment variables: `{{key}}` syntax resolved at send time
- History is FIFO with configurable max (default 200)
- Collections persist named requests for re-execution
- Default headers applied to every request

## CLI Commands
### HTTP Requests
- `get/post/put/patch/delete/head/options <url> [--body JSON]`
- `send <METHOD> <url> [--header Key:Value;...] [--body text]`

### Response
- `response` — full last response (req + res + headers + body)
- `body` — just the body (pretty-printed if JSON)
- `response-headers` — response headers
- `curl` — cURL equivalent of last request

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
- `col-show <name>` / `cols` — show/list collections
- `col-rm-req <collection> <index>` — remove request from collection

### History
- `history [--limit N]` — show history
- `replay <index>` — re-execute from history
- `clear-history` — clear all history

## Persistence
All data stored in `~/.config/hidariapi/`:
- `collections.json` — saved request collections
- `history.json` — request history (max 200 entries)
- `environments.json` — environment variables
