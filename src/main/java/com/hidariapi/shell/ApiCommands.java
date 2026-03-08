package com.hidariapi.shell;

import com.hidariapi.model.*;
import com.hidariapi.model.Collection;
import com.hidariapi.model.Language;
import com.hidariapi.shell.completion.CollectionNameValueProvider;
import com.hidariapi.shell.completion.EnvironmentNameValueProvider;
import com.hidariapi.service.ApiService;
import com.hidariapi.service.LanguageService;
import com.hidariapi.util.CurlParser;
import com.hidariapi.util.JsonFormatter;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.util.*;

/**
 * Interactive commands for HidariApi — API tester in the terminal.
 */
@ShellComponent
public class ApiCommands {

    private static final AttributedStyle BOLD = AttributedStyle.DEFAULT.bold();
    private static final AttributedStyle DIM = AttributedStyle.DEFAULT.faint();
    private static final AttributedStyle CYAN = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
    private static final AttributedStyle GREEN = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    private static final AttributedStyle YELLOW = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle RED = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
    private static final AttributedStyle MAGENTA = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
    private static final AttributedStyle BOLD_CYAN = BOLD.foreground(AttributedStyle.CYAN);
    private static final AttributedStyle BOLD_GREEN = BOLD.foreground(AttributedStyle.GREEN);
    private static final AttributedStyle BOLD_RED = BOLD.foreground(AttributedStyle.RED);
    private static final AttributedStyle BOLD_YELLOW = BOLD.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle WHITE = AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);

    private final ApiService service;
    private final JsonFormatter jsonFormatter;
    private final CurlParser curlParser;
    private final LanguageService lang;

    public ApiCommands(ApiService service, JsonFormatter jsonFormatter, CurlParser curlParser, LanguageService lang) {
        this.service = service;
        this.jsonFormatter = jsonFormatter;
        this.curlParser = curlParser;
        this.lang = lang;
    }

    // Helper to delegate to LanguageService
    private String t(String pt, String en) {
        return lang.t(pt, en);
    }

    // ========== LANGUAGE ===================================================

    @ShellMethod(key = "language", value = "Switch display language (pt/en)")
    public String language(@ShellOption(help = "Language: pt or en") String lang) {
        var language = Language.fromString(lang);
        this.lang.setCurrent(language);
        return styled(GREEN, t("Idioma alterado para: " + language, "Language changed to: " + language));
    }

    @ShellMethod(key = "help", value = "Show translated help")
    public String help() {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Hidari API - " + t("Ajuda", "Help") + "\n\n"));
        sb.append(styled(DIM, "  " + t("Idioma atual", "Current language") + ": "))
                .append(styled(GREEN, lang.getCurrent().name().toLowerCase()))
                .append("\n");
        sb.append(styled(DIM, "  " + t("Dica", "Tip") + ": "))
                .append(t("use 'language pt' ou 'language eng' para trocar.", "use 'language pt' or 'language eng' to switch."))
                .append("\n");

        appendHelpSection(sb, t("Requisicoes HTTP", "HTTP Requests"));
        appendHelpItem(sb, "get <url> [--param k=v&a=b]", t("Envia GET.", "Sends GET."));
        appendHelpItem(sb, "post <url> --body '{...}' [--param ...]", t("Envia POST JSON.", "Sends JSON POST."));
        appendHelpItem(sb, "put <url> --body '{...}' [--param ...]", t("Envia PUT JSON.", "Sends JSON PUT."));
        appendHelpItem(sb, "patch <url> --body '{...}' [--param ...]", t("Envia PATCH JSON.", "Sends JSON PATCH."));
        appendHelpItem(sb, "delete <url> [--param ...]", t("Envia DELETE.", "Sends DELETE."));
        appendHelpItem(sb, "head <url>", t("Envia HEAD.", "Sends HEAD."));
        appendHelpItem(sb, "options <url>", t("Envia OPTIONS.", "Sends OPTIONS."));
        appendHelpItem(sb, "send <method> <url> [--header ...] [--body ...]", t("Request customizado.", "Custom request."));

        appendHelpSection(sb, t("Resposta", "Response"));
        appendHelpItem(sb, "response", t("Mostra a resposta completa.", "Shows full response."));
        appendHelpItem(sb, "body", t("Mostra apenas o body.", "Shows only the body."));
        appendHelpItem(sb, "response-headers", t("Mostra headers da resposta.", "Shows response headers."));
        appendHelpItem(sb, "curl", t("Gera cURL do ultimo request.", "Generates cURL from last request."));
        appendHelpItem(sb, "import-curl \"curl ...\" [--dry-run]", t("Importa e executa cURL.", "Imports and executes cURL."));
        appendHelpItem(sb, "save-response <arquivo>", t("Salva body em arquivo.", "Saves body to file."));

        appendHelpSection(sb, t("Headers Padrao", "Default Headers"));
        appendHelpItem(sb, "set-header <key> <value>", t("Define header padrao.", "Sets default header."));
        appendHelpItem(sb, "unset-header <key>", t("Remove header padrao.", "Removes default header."));
        appendHelpItem(sb, "headers", t("Lista headers padrao.", "Lists default headers."));
        appendHelpItem(sb, "clear-headers", t("Limpa headers padrao.", "Clears default headers."));
        appendHelpItem(sb, "bearer <token>", t("Define Authorization Bearer.", "Sets Authorization Bearer."));

        appendHelpSection(sb, t("Ambientes", "Environments"));
        appendHelpItem(sb, "env-create <nome>", t("Cria ambiente.", "Creates environment."));
        appendHelpItem(sb, "env-set <env> <key> <value>", t("Define variavel.", "Sets variable."));
        appendHelpItem(sb, "env-unset <env> <key>", t("Remove variavel.", "Removes variable."));
        appendHelpItem(sb, "env-use <nome>", t("Ativa ambiente.", "Activates environment."));
        appendHelpItem(sb, "env-clear", t("Desativa ambiente.", "Deactivates environment."));
        appendHelpItem(sb, "envs / env-show <nome> / env-rm <nome>", t("Lista, mostra e remove ambientes.", "Lists, shows and removes environments."));

        appendHelpSection(sb, t("Colecoes", "Collections"));
        appendHelpItem(sb, "col-create <nome>", t("Cria colecao.", "Creates collection."));
        appendHelpItem(sb, "col-add <colecao> <nomeRequest>", t("Salva ultimo request.", "Saves last request."));
        appendHelpItem(sb, "col-show <colecao> / cols", t("Mostra/lista colecoes.", "Shows/lists collections."));
        appendHelpItem(sb, "col-run <colecao> <indice>", t("Executa request salvo.", "Runs saved request."));
        appendHelpItem(sb, "col-run-all <colecao>", t("Executa todos os requests.", "Runs all requests."));
        appendHelpItem(sb, "col-rm-req <colecao> <indice> / col-rm <nome>", t("Remove request/colecao.", "Removes request/collection."));

        appendHelpSection(sb, t("Historico", "History"));
        appendHelpItem(sb, "history [--limit N]", t("Mostra historico.", "Shows history."));
        appendHelpItem(sb, "replay <indice>", t("Reexecuta request do historico.", "Replays history request."));
        appendHelpItem(sb, "clear-history", t("Limpa historico.", "Clears history."));

        appendHelpSection(sb, t("Templates", "Templates"));
        appendHelpItem(sb, "{{$timestamp}} / {{$isoTimestamp}} / {{$uuid}} / {{$cpf}}", t("Variaveis dinamicas em URL/header/body.", "Dynamic variables in URL/header/body."));
        appendHelpItem(sb, "{{chave}} ou {{env.chave}}", t("Variavel do ambiente ativo.", "Variable from active environment."));
        appendHelpItem(sb, "{{last.status}} / {{last.header.content-type}}", t("Usa dados da ultima resposta.", "Uses data from last response."));
        appendHelpItem(sb, "{{last.body.user.id}}", t("Extrai campo JSON da ultima resposta.", "Extracts JSON field from last response."));

        appendHelpSection(sb, t("Mock Server", "Mock Server"));
        appendHelpItem(sb, "mock-start [--port 8089] / mock-stop / mock-status", t("Controla servidor mock.", "Controls mock server."));
        appendHelpItem(sb, "mock-add <method> <path> [--status --body --header --delay --desc]", t("Adiciona rota.", "Adds route."));
        appendHelpItem(sb, "mock-add-json <method> <path> --body '{...}'", t("Atalho para rota JSON.", "Shortcut for JSON route."));
        appendHelpItem(sb, "mock-add-crud <basePath>", t("Cria CRUD completo.", "Creates full CRUD."));
        appendHelpItem(sb, "mock-from-response <path> [--method GET]", t("Cria rota da ultima resposta.", "Creates route from last response."));
        appendHelpItem(sb, "mock-list / mock-show <i> / mock-edit <i> ...", t("Lista, mostra e edita rotas.", "Lists, shows and edits routes."));
        appendHelpItem(sb, "mock-rm <i> / mock-clear", t("Remove rota(s).", "Removes route(s)."));
        appendHelpItem(sb, "mock-logs [--limit N] / mock-clear-logs", t("Mostra/limpa logs do mock.", "Shows/clears mock logs."));
        appendHelpItem(sb, "{{param.id}} / {{query.page}} / {{faker.uuid}} / {{faker.cpf}}", t("Templates dinamicos para body/headers mock.", "Dynamic templates for mock body/headers."));

        sb.append("\n");
        return sb.toString();
    }

    // ========== HTTP REQUESTS ==============================================

    @ShellMethod(key = "get", value = "Envia requisicao GET")
    public String get(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        return executeRequest(ApiRequest.of(null, HttpMethod.GET, appendParams(url, params)));
    }

    @ShellMethod(key = "post", value = "Envia requisicao POST com body JSON")
    public String post(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.POST, appendParams(url, params))
                .withHeader("Content-Type", "application/json");
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, t("Erro ao ler body do arquivo.", "Error reading body from file."));
        return executeRequest(req);
    }

    @ShellMethod(key = "put", value = "Envia requisicao PUT com body JSON")
    public String put(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.PUT, appendParams(url, params))
                .withHeader("Content-Type", "application/json");
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, t("Erro ao ler body do arquivo.", "Error reading body from file."));
        return executeRequest(req);
    }

    @ShellMethod(key = "patch", value = "Envia requisicao PATCH com body JSON")
    public String patch(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body JSON (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.PATCH, appendParams(url, params))
                .withHeader("Content-Type", "application/json");
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, t("Erro ao ler body do arquivo.", "Error reading body from file."));
        return executeRequest(req);
    }

    @ShellMethod(key = "delete", value = "Envia requisicao DELETE")
    public String delete(
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        return executeRequest(ApiRequest.of(null, HttpMethod.DELETE, appendParams(url, params)));
    }

    @ShellMethod(key = "head", value = "Envia requisicao HEAD (retorna apenas headers)")
    public String head(@ShellOption(help = "URL") String url) {
        return executeRequest(ApiRequest.of(null, HttpMethod.HEAD, url));
    }

    @ShellMethod(key = "options", value = "Envia requisicao OPTIONS")
    public String options(@ShellOption(help = "URL") String url) {
        return executeRequest(ApiRequest.of(null, HttpMethod.OPTIONS, url));
    }

    @ShellMethod(key = "send", value = "Envia request customizado (metodo, url, headers, body)")
    public String send(
            @ShellOption(help = "Metodo HTTP (GET, POST, PUT, etc.)") String method,
            @ShellOption(help = "URL") String url,
            @ShellOption(value = "--header", defaultValue = ShellOption.NULL,
                    help = "Header (formato Key:Value, multiplos separados por ;)") String headers,
            @ShellOption(value = "--body", defaultValue = ShellOption.NULL,
                    help = "Body (use @arquivo.json para ler de arquivo)") String body,
            @ShellOption(value = "--param", defaultValue = ShellOption.NULL,
                    help = "Query params (formato key=value, multiplos separados por &)") String params) {
        var req = ApiRequest.of(null, HttpMethod.fromString(method), appendParams(url, params));
        if (headers != null) {
            for (var h : headers.split(";")) {
                var parts = h.split(":", 2);
                if (parts.length == 2) {
                    req = req.withHeader(parts[0].trim(), parts[1].trim());
                }
            }
        }
        req = resolveAndSetBody(req, body);
        if (req == null) return styled(RED, t("Erro ao ler body do arquivo.", "Error reading body from file."));
        return executeRequest(req);
    }

    // ========== RESPONSE ===================================================

    @ShellMethod(key = "response", value = "Mostra a ultima resposta completa")
    public String response() {
        var res = service.getLastResponse();
        if (res == null) return styled(YELLOW, t("Nenhuma resposta ainda. Envie um request primeiro.", "No response yet. Send a request first."));
        return formatFullResponse(service.getLastRequest(), res);
    }

    @ShellMethod(key = "body", value = "Mostra apenas o body da ultima resposta")
    public String body() {
        var res = service.getLastResponse();
        if (res == null) return styled(YELLOW, t("Nenhuma resposta ainda.", "No response yet."));
        if (res.body().isBlank()) return styled(DIM, t("(body vazio)", "(empty body)"));
        if (res.isJson()) return jsonFormatter.prettify(res.body());
        return res.body();
    }

    @ShellMethod(key = "response-headers", value = "Mostra headers da ultima resposta")
    public String responseHeaders() {
        var res = service.getLastResponse();
        if (res == null) return styled(YELLOW, t("Nenhuma resposta ainda.", "No response yet."));
        return formatHeaders(t("Headers da Resposta", "Response Headers"), res.headers());
    }

    @ShellMethod(key = "curl", value = "Gera comando cURL do ultimo request")
    public String curl() {
        var req = service.getLastRequest();
        if (req == null) return styled(YELLOW, t("Nenhum request ainda.", "No request yet."));
        return styled(CYAN, req.toCurl());
    }

    @ShellMethod(key = "import-curl", value = "Importa um comando cURL e executa como request")
    public String importCurl(
            @ShellOption(help = "Comando cURL completo (entre aspas)") String curlCommand,
            @ShellOption(value = "--save", defaultValue = ShellOption.NULL,
                    help = "Salvar na colecao (formato colecao:nome)") String save,
            @ShellOption(value = "--dry-run", defaultValue = "false",
                    help = "Apenas parseia sem executar") boolean dryRun) {
        try {
            var req = curlParser.parse(curlCommand);

            if (dryRun) {
                var sb = new StringBuilder();
                sb.append(styled(BOLD_CYAN, "\n  " + t("Request parseado do cURL", "Parsed request from cURL") + "\n\n"));
                sb.append(styled(BOLD, "  " + req.method() + " ")).append(req.url()).append("\n");
                if (!req.headers().isEmpty()) {
                    for (var h : req.headers().entrySet()) {
                        sb.append(styled(DIM, "  " + h.getKey() + ": ")).append(h.getValue()).append("\n");
                    }
                }
                if (req.body() != null && !req.body().isBlank()) {
                    sb.append(styled(DIM, "\n  Body:\n"));
                    var bodyText = jsonFormatter.isValidJson(req.body())
                            ? jsonFormatter.prettify(req.body()) : req.body();
                    for (var line : bodyText.split("\n")) {
                        sb.append("  ").append(line).append("\n");
                    }
                }
                return sb.toString();
            }

            // Save to collection if requested
            if (save != null) {
                var parts = save.split(":", 2);
                if (parts.length == 2) {
                    var col = service.getCollection(parts[0]);
                    if (col.isPresent()) {
                        service.saveCollection(col.get().withRequest(req.withName(parts[1])));
                        System.out.println(styled(GREEN,
                                t("Salvo como '" + parts[1] + "' na colecao '" + parts[0] + "'",
                                  "Saved as '" + parts[1] + "' in collection '" + parts[0] + "'")));
                    }
                }
            }

            return executeRequest(req);
        } catch (Exception e) {
            return styled(RED, t("Erro ao parsear cURL: ", "Error parsing cURL: ") + e.getMessage());
        }
    }

    @ShellMethod(key = "save-response", value = "Salva o body da ultima resposta em um arquivo")
    public String saveResponse(@ShellOption(help = "Caminho do arquivo") String filePath) {
        try {
            service.saveResponseToFile(filePath);
            var res = service.getLastResponse();
            return styled(GREEN, t("Resposta salva em: ", "Response saved to: ") + filePath + " (" + res.sizeText() + ")");
        } catch (IOException e) {
            return styled(RED, t("Erro ao salvar: ", "Error saving: ") + e.getMessage());
        }
    }

    // ========== DEFAULT HEADERS ============================================

    @ShellMethod(key = "set-header", value = "Define header padrao para todos os requests")
    public String setHeader(
            @ShellOption(help = "Nome do header") String key,
            @ShellOption(help = "Valor do header") String value) {
        service.setDefaultHeader(key, value);
        return styled(GREEN, t("Header definido: ", "Header set: ") + key + ": " + value);
    }

    @ShellMethod(key = "unset-header", value = "Remove header padrao")
    public String unsetHeader(@ShellOption(help = "Nome do header") String key) {
        service.removeDefaultHeader(key);
        return styled(GREEN, t("Header removido: ", "Header removed: ") + key);
    }

    @ShellMethod(key = "headers", value = "Lista headers padrao configurados")
    public String headers() {
        var hdrs = service.getDefaultHeaders();
        if (hdrs.isEmpty()) return styled(DIM, t("Nenhum header padrao configurado.", "No default headers configured."));
        return formatHeaders(t("Headers Padrao", "Default Headers"), hdrs);
    }

    @ShellMethod(key = "clear-headers", value = "Remove todos os headers padrao")
    public String clearHeaders() {
        service.clearDefaultHeaders();
        return styled(GREEN, t("Todos os headers padrao removidos.", "All default headers removed."));
    }

    @ShellMethod(key = "bearer", value = "Define Authorization: Bearer <token>")
    public String bearer(@ShellOption(help = "Token") String token) {
        service.setDefaultHeader("Authorization", "Bearer " + token);
        return styled(GREEN, t("Bearer token configurado.", "Bearer token set."));
    }

    // ========== ENVIRONMENTS ===============================================

    @ShellMethod(key = "env-create", value = "Cria um novo ambiente")
    public String envCreate(@ShellOption(help = "Nome do ambiente (ex: dev, prod)") String name) {
        service.saveEnvironment(Environment.empty(name));
        return styled(GREEN, t("Ambiente criado: ", "Environment created: ") + name);
    }

    @ShellMethod(key = "env-set", value = "Define variavel no ambiente")
    public String envSet(
            @ShellOption(help = "Nome do ambiente", valueProvider = EnvironmentNameValueProvider.class) String envName,
            @ShellOption(help = "Nome da variavel") String key,
            @ShellOption(help = "Valor") String value) {
        var env = service.getEnvironment(envName);
        if (env.isEmpty()) return styled(RED, t("Ambiente nao encontrado: ", "Environment not found: ") + envName);
        service.saveEnvironment(env.get().withVariable(key, value));
        return styled(GREEN, t("Variavel definida: ", "Variable set: ") + key + " = " + value + " [" + envName + "]");
    }

    @ShellMethod(key = "env-unset", value = "Remove variavel de um ambiente")
    public String envUnset(
            @ShellOption(help = "Nome do ambiente", valueProvider = EnvironmentNameValueProvider.class) String envName,
            @ShellOption(help = "Nome da variavel") String key) {
        var env = service.getEnvironment(envName);
        if (env.isEmpty()) return styled(RED, t("Ambiente nao encontrado: ", "Environment not found: ") + envName);
        service.saveEnvironment(env.get().withoutVariable(key));
        return styled(GREEN, t("Variavel removida: ", "Variable removed: ") + key + " [" + envName + "]");
    }

    @ShellMethod(key = "env-use", value = "Ativa um ambiente (variaveis {{key}} serao substituidas)")
    public String envUse(@ShellOption(help = "Nome do ambiente", valueProvider = EnvironmentNameValueProvider.class) String name) {
        var env = service.getEnvironment(name);
        if (env.isEmpty()) return styled(RED, t("Ambiente nao encontrado: ", "Environment not found: ") + name);
        service.setActiveEnvironment(name);
        return styled(GREEN, t("Ambiente ativo: ", "Active environment: ") + name);
    }

    @ShellMethod(key = "env-clear", value = "Desativa o ambiente atual")
    public String envClear() {
        service.clearActiveEnvironment();
        return styled(GREEN, t("Ambiente desativado.", "Environment deactivated."));
    }

    @ShellMethod(key = "envs", value = "Lista ambientes disponiveis")
    public String envs() {
        var envs = service.listEnvironments();
        if (envs.isEmpty()) return styled(DIM, t("Nenhum ambiente criado. Use 'env-create <nome>' para criar.", "No environments created. Use 'env-create <name>' to create one."));
        return formatEnvironments(envs);
    }

    @ShellMethod(key = "env-show", value = "Mostra variaveis de um ambiente")
    public String envShow(@ShellOption(help = "Nome do ambiente", valueProvider = EnvironmentNameValueProvider.class) String name) {
        var env = service.getEnvironment(name);
        if (env.isEmpty()) return styled(RED, t("Ambiente nao encontrado: ", "Environment not found: ") + name);
        return formatEnvironmentDetail(env.get());
    }

    @ShellMethod(key = "env-rm", value = "Remove um ambiente")
    public String envRm(@ShellOption(help = "Nome do ambiente", valueProvider = EnvironmentNameValueProvider.class) String name) {
        if (service.removeEnvironment(name)) {
            return styled(GREEN, t("Ambiente removido: ", "Environment removed: ") + name);
        }
        return styled(RED, t("Ambiente nao encontrado: ", "Environment not found: ") + name);
    }

    // ========== COLLECTIONS ================================================

    @ShellMethod(key = "col-create", value = "Cria uma nova colecao")
    public String colCreate(@ShellOption(help = "Nome da colecao") String name) {
        service.saveCollection(Collection.empty(name));
        return styled(GREEN, t("Colecao criada: ", "Collection created: ") + name);
    }

    @ShellMethod(key = "col-add", value = "Salva o ultimo request em uma colecao")
    public String colAdd(
            @ShellOption(help = "Nome da colecao", valueProvider = CollectionNameValueProvider.class) String colName,
            @ShellOption(help = "Nome para o request") String reqName) {
        var req = service.getLastRequest();
        if (req == null) return styled(RED, t("Nenhum request para salvar. Envie um request primeiro.", "No request to save. Send a request first."));
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, t("Colecao nao encontrada: ", "Collection not found: ") + colName);
        service.saveCollection(col.get().withRequest(req.withName(reqName)));
        return styled(GREEN, t("Request '" + reqName + "' salvo na colecao '" + colName + "'",
                                "Request '" + reqName + "' saved in collection '" + colName + "'"));
    }

    @ShellMethod(key = "col-run", value = "Executa um request de uma colecao")
    public String colRun(
            @ShellOption(help = "Nome da colecao", valueProvider = CollectionNameValueProvider.class) String colName,
            @ShellOption(help = "Indice do request (1-based)") int index) {
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, t("Colecao nao encontrada: ", "Collection not found: ") + colName);
        var requests = col.get().requests();
        if (index < 1 || index > requests.size()) {
            return styled(RED, t("Indice invalido. Colecao tem " + requests.size() + " request(s).",
                                  "Invalid index. Collection has " + requests.size() + " request(s)."));
        }
        return executeRequest(requests.get(index - 1));
    }

    @ShellMethod(key = "col-show", value = "Mostra requests de uma colecao")
    public String colShow(@ShellOption(help = "Nome da colecao", valueProvider = CollectionNameValueProvider.class) String name) {
        var col = service.getCollection(name);
        if (col.isEmpty()) return styled(RED, t("Colecao nao encontrada: ", "Collection not found: ") + name);
        return formatCollectionDetail(col.get());
    }

    @ShellMethod(key = "col-rm-req", value = "Remove um request de uma colecao")
    public String colRmReq(
            @ShellOption(help = "Nome da colecao", valueProvider = CollectionNameValueProvider.class) String colName,
            @ShellOption(help = "Indice do request (1-based)") int index) {
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, t("Colecao nao encontrada: ", "Collection not found: ") + colName);
        if (index < 1 || index > col.get().requests().size()) {
            return styled(RED, t("Indice invalido.", "Invalid index."));
        }
        service.saveCollection(col.get().withoutRequest(index - 1));
        return styled(GREEN, t("Request removido da colecao '" + colName + "'",
                                "Request removed from collection '" + colName + "'"));
    }

    @ShellMethod(key = "cols", value = "Lista colecoes")
    public String cols() {
        var cols = service.listCollections();
        if (cols.isEmpty()) return styled(DIM, t("Nenhuma colecao. Use 'col-create <nome>' para criar.", "No collections. Use 'col-create <name>' to create one."));
        return formatCollections(cols);
    }

    @ShellMethod(key = "col-run-all", value = "Executa todos os requests de uma colecao (smoke test)")
    public String colRunAll(@ShellOption(help = "Nome da colecao", valueProvider = CollectionNameValueProvider.class) String colName) {
        var col = service.getCollection(colName);
        if (col.isEmpty()) return styled(RED, t("Colecao nao encontrada: ", "Collection not found: ") + colName);
        if (col.get().requests().isEmpty()) return styled(YELLOW, t("Colecao vazia.", "Empty collection."));

        System.out.println(styled(CYAN, t("Executando " + col.get().requests().size()
                + " request(s) da colecao '" + colName + "'...",
                "Running " + col.get().requests().size()
                + " request(s) from collection '" + colName + "'...")));
        System.out.println();

        var results = service.runCollection(colName);
        return formatRunAllResults(colName, results);
    }

    @ShellMethod(key = "col-rm", value = "Remove uma colecao")
    public String colRm(@ShellOption(help = "Nome da colecao", valueProvider = CollectionNameValueProvider.class) String name) {
        if (service.removeCollection(name)) {
            return styled(GREEN, t("Colecao removida: ", "Collection removed: ") + name);
        }
        return styled(RED, t("Colecao nao encontrada: ", "Collection not found: ") + name);
    }

    // ========== HISTORY ====================================================

    @ShellMethod(key = "history", value = "Mostra historico de requests")
    public String history(
            @ShellOption(value = "--limit", defaultValue = "20", help = "Limite de entradas") int limit) {
        var entries = service.getHistory(limit);
        if (entries.isEmpty()) return styled(DIM, t("Historico vazio.", "History is empty."));
        return formatHistory(entries);
    }

    @ShellMethod(key = "replay", value = "Re-executa um request do historico")
    public String replay(@ShellOption(help = "Indice do historico (1-based)") int index) {
        var entry = service.getHistoryEntry(index);
        if (entry == null) return styled(RED, t("Indice invalido.", "Invalid index."));
        return executeRequest(entry.request());
    }

    @ShellMethod(key = "clear-history", value = "Limpa o historico de requests")
    public String clearHistory() {
        service.clearHistory();
        return styled(GREEN, t("Historico limpo.", "History cleared."));
    }

    // ========== FORMATTING =================================================

    private String executeRequest(ApiRequest request) {
        try {
            System.out.println(styled(DIM, request.method() + " " + request.url() + "..."));
            var response = service.execute(request);
            return formatResponse(request, response);
        } catch (Exception e) {
            return styled(RED, t("Erro: ", "Error: ") + e.getMessage());
        }
    }

    private String formatResponse(ApiRequest req, ApiResponse res) {
        var sb = new StringBuilder();

        // Status line
        var statusStyle = res.isSuccess() ? BOLD_GREEN : (res.statusCode() < 500 ? BOLD_YELLOW : BOLD_RED);
        sb.append("\n  ");
        sb.append(styled(statusStyle, res.statusText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.durationText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.sizeText()));
        sb.append("\n");

        // Body preview (first 80 lines)
        if (!res.body().isBlank()) {
            sb.append("\n");
            var bodyText = res.isJson() ? jsonFormatter.prettify(res.body()) : res.body();
            var lines = bodyText.split("\n");
            int limit = Math.min(lines.length, 80);
            for (int i = 0; i < limit; i++) {
                sb.append("  ").append(lines[i]).append("\n");
            }
            if (lines.length > 80) {
                sb.append(styled(DIM, "  ... (" + (lines.length - 80) + " " + t("linhas omitidas. Use 'body' para ver tudo", "lines omitted. Use 'body' to see all") + ")\n"));
            }
        }

        return sb.toString();
    }

    private String formatFullResponse(ApiRequest req, ApiResponse res) {
        var sb = new StringBuilder();

        sb.append(styled(BOLD_CYAN, "\n  " + t("Requisicao", "Request") + "\n\n"));
        if (req != null) {
            sb.append(styled(BOLD, "  " + req.method() + " ")).append(styled(WHITE, req.url())).append("\n");
            if (!req.headers().isEmpty()) {
                for (var h : req.headers().entrySet()) {
                    sb.append(styled(DIM, "  " + h.getKey() + ": ")).append(h.getValue()).append("\n");
                }
            }
            if (req.body() != null && !req.body().isBlank()) {
                sb.append(styled(DIM, "\n  " + t("Body", "Body") + ":\n"));
                sb.append("  ").append(req.body()).append("\n");
            }
        }

        sb.append(styled(BOLD_CYAN, "\n  " + t("Resposta", "Response") + "\n\n"));
        var statusStyle = res.isSuccess() ? BOLD_GREEN : (res.statusCode() < 500 ? BOLD_YELLOW : BOLD_RED);
        sb.append("  ").append(styled(statusStyle, res.statusText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.durationText()));
        sb.append(styled(DIM, "  |  "));
        sb.append(styled(CYAN, res.sizeText()));
        sb.append("\n\n");

        // Response headers
        for (var h : res.headers().entrySet()) {
            sb.append(styled(DIM, "  " + h.getKey() + ": ")).append(h.getValue()).append("\n");
        }

        // Body
        if (!res.body().isBlank()) {
            sb.append(styled(BOLD_CYAN, "\n  " + t("Body", "Body") + "\n\n"));
            var bodyText = res.isJson() ? jsonFormatter.prettify(res.body()) : res.body();
            for (var line : bodyText.split("\n")) {
                sb.append("  ").append(line).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatHeaders(String title, Map<String, String> headers) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + title + "\n\n"));
        for (var h : headers.entrySet()) {
            sb.append(styled(GREEN, "  " + h.getKey())).append(styled(DIM, ": ")).append(h.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String formatEnvironments(List<Environment> envs) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Ambientes", "Environments") + "\n\n"));

        var active = service.getActiveEnvironment();

        for (int i = 0; i < envs.size(); i++) {
            var env = envs.get(i);
            var isActive = active != null && active.name().equals(env.name());
            var marker = isActive ? styled(GREEN, " * ") : "   ";
            sb.append(marker);
            sb.append(styled(CYAN, String.format("%-3d ", i + 1)));
            sb.append(styled(isActive ? BOLD_GREEN : BOLD, env.name()));
            sb.append(styled(DIM, " (" + env.variables().size() + " " + t("variavel(is)", "variable(s)") + ")"));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatEnvironmentDetail(Environment env) {
        var sb = new StringBuilder();
        var active = service.getActiveEnvironment();
        var isActive = active != null && active.name().equals(env.name());

        sb.append(styled(BOLD_CYAN, "\n  " + t("Ambiente: ", "Environment: ") + env.name()));
        if (isActive) sb.append(styled(GREEN, " " + t("(ativo)", "(active)")));
        sb.append("\n\n");

        if (env.variables().isEmpty()) {
            sb.append(styled(DIM, "  " + t("Nenhuma variavel definida.", "No variables defined.") + "\n"));
        } else {
            for (var v : env.variables().entrySet()) {
                sb.append(styled(YELLOW, "  {{" + v.getKey() + "}}"));
                sb.append(styled(DIM, " = "));
                sb.append(v.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatCollections(List<Collection> cols) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Colecoes", "Collections") + "\n\n"));

        for (int i = 0; i < cols.size(); i++) {
            var col = cols.get(i);
            sb.append(styled(CYAN, String.format("  %-3d ", i + 1)));
            sb.append(styled(BOLD, col.name()));
            sb.append(styled(DIM, " (" + col.requests().size() + " request(s))"));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatCollectionDetail(Collection col) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Colecao: ", "Collection: ") + col.name() + "\n\n"));

        if (col.requests().isEmpty()) {
            sb.append(styled(DIM, "  " + t("Nenhum request salvo.", "No saved requests.") + "\n"));
            return sb.toString();
        }

        sb.append(styled(DIM, String.format("  %-4s %-8s %-30s %s%n", "#", t("METODO", "METHOD"), t("NOME", "NAME"), "URL")));
        sb.append(styled(DIM, "  " + "-".repeat(90) + "\n"));

        for (int i = 0; i < col.requests().size(); i++) {
            var req = col.requests().get(i);
            var methodStyle = switch (req.method()) {
                case GET -> GREEN;
                case POST -> YELLOW;
                case PUT -> CYAN;
                case PATCH -> MAGENTA;
                case DELETE -> RED;
                default -> DIM;
            };
            sb.append(styled(CYAN, String.format("  %-4d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", req.method().name())));
            sb.append(styled(BOLD, String.format("%-30s ", truncate(req.name() != null ? req.name() : "-", 29))));
            sb.append(styled(DIM, truncate(req.url(), 50)));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatHistory(List<SavedRequest> entries) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Historico", "History") + "\n\n"));

        sb.append(styled(DIM, String.format("  %-4s %-8s %-6s %-45s %s%n",
                "#", t("METODO", "METHOD"), "STATUS", "URL", t("TEMPO", "TIME"))));
        sb.append(styled(DIM, "  " + "-".repeat(90) + "\n"));

        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var req = entry.request();

            var methodStyle = switch (req.method()) {
                case GET -> GREEN;
                case POST -> YELLOW;
                case PUT -> CYAN;
                case PATCH -> MAGENTA;
                case DELETE -> RED;
                default -> DIM;
            };

            var statusStyle = entry.statusCode() == 0 ? RED
                    : entry.statusCode() < 300 ? GREEN
                    : entry.statusCode() < 500 ? YELLOW : RED;

            sb.append(styled(CYAN, String.format("  %-4d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", req.method().name())));
            sb.append(styled(statusStyle, String.format("%-6s ", entry.statusCode() == 0 ? "ERR" : String.valueOf(entry.statusCode()))));
            sb.append(styled(DIM, String.format("%-45s ", truncate(req.url(), 44))));
            sb.append(styled(DIM, entry.duration() + "ms"));
            sb.append("\n");
        }

        sb.append("\n");
        sb.append(styled(DIM, "  Total: " + service.historySize() + " " + t("registro(s)", "record(s)") + "\n"));
        return sb.toString();
    }

    // ========== HELPERS ====================================================

    private String styled(AttributedStyle style, String text) {
        return new AttributedStringBuilder()
                .style(style)
                .append(text)
                .toAttributedString()
                .toAnsi();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 1) + "~" : text;
    }

    private void appendHelpSection(StringBuilder sb, String title) {
        sb.append(styled(BOLD_CYAN, "\n  " + title + "\n"));
    }

    private void appendHelpItem(StringBuilder sb, String command, String description) {
        sb.append(styled(CYAN, "  " + String.format("%-52s", command)));
        sb.append(styled(DIM, " - " + description + "\n"));
    }

    /** Appends query params to URL. Params in format "key=value&key2=value2". */
    private String appendParams(String url, String params) {
        if (params == null || params.isBlank()) return url;
        var separator = url.contains("?") ? "&" : "?";
        return url + separator + params;
    }

    /** Resolves body (inline or @file) and returns request with body set. Returns null on error. */
    private ApiRequest resolveAndSetBody(ApiRequest req, String body) {
        if (body == null) return req;
        try {
            var resolved = service.resolveBody(body);
            return req.withBody(resolved);
        } catch (IOException e) {
            System.err.println(t("Erro ao ler body: ", "Error reading body: ") + e.getMessage());
            return null;
        }
    }

    private String formatRunAllResults(String colName, List<ApiService.RunResult> results) {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  " + t("Resultado: ", "Result: ") + colName + "\n\n"));

        sb.append(styled(DIM, String.format("  %-4s %-8s %-6s %-30s %-40s %s%n",
                "#", t("METODO", "METHOD"), "STATUS", t("NOME", "NAME"), "URL", t("TEMPO", "TIME"))));
        sb.append(styled(DIM, "  " + "-".repeat(105) + "\n"));

        int passed = 0, failed = 0;
        long totalTime = 0;

        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            boolean success = r.error() == null && r.statusCode() >= 200 && r.statusCode() < 400;
            if (success) passed++;
            else failed++;
            totalTime += r.durationMs();

            var methodStyle = switch (r.method()) {
                case GET -> GREEN;
                case POST -> YELLOW;
                case PUT -> CYAN;
                case PATCH -> MAGENTA;
                case DELETE -> RED;
                default -> DIM;
            };

            var statusStyle = r.error() != null ? BOLD_RED
                    : r.statusCode() < 300 ? GREEN
                    : r.statusCode() < 500 ? YELLOW : RED;

            var statusText = r.error() != null ? "ERR" : String.valueOf(r.statusCode());

            sb.append(styled(success ? GREEN : RED, "  " + (success ? "+" : "x") + " "));
            sb.append(styled(CYAN, String.format("%-3d ", i + 1)));
            sb.append(styled(methodStyle, String.format("%-8s ", r.method().name())));
            sb.append(styled(statusStyle, String.format("%-6s ", statusText)));
            sb.append(styled(BOLD, String.format("%-30s ", truncate(r.name() != null ? r.name() : "-", 29))));
            sb.append(styled(DIM, String.format("%-40s ", truncate(r.url(), 39))));
            sb.append(styled(DIM, r.durationMs() + "ms"));
            sb.append("\n");

            if (r.error() != null) {
                sb.append(styled(RED, "         " + r.error())).append("\n");
            }
        }

        sb.append("\n");
        var summaryStyle = failed == 0 ? BOLD_GREEN : BOLD_YELLOW;
        sb.append(styled(summaryStyle, String.format("  %d %s, %d %s",
                passed, t("passou", "passed"),
                failed, t("falhou", "failed"))));
        sb.append(styled(DIM, String.format("  |  %s: %dms  |  %d request(s)",
                t("total", "total"), totalTime, results.size())));
        sb.append("\n");

        return sb.toString();
    }
}
