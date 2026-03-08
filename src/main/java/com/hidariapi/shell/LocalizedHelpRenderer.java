package com.hidariapi.shell;

import com.hidariapi.service.LanguageService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the translated help text shown by the custom help command.
 */
@Component
public class LocalizedHelpRenderer extends LocalizedSupport {

    private final List<HelpSection> sections;

    public LocalizedHelpRenderer(LanguageService lang) {
        super(lang);
        this.sections = buildSections();
    }

    public String render() {
        var sb = new StringBuilder();
        sb.append(styled(BOLD_CYAN, "\n  Hidari API - " + t("Ajuda", "Help") + "\n\n"));
        sb.append(styled(DIM, "  " + t("Idioma atual", "Current language") + ": "))
                .append(styled(GREEN, lang.getCurrent().name().toLowerCase()))
                .append("\n");
        sb.append(styled(DIM, "  " + t("Dica", "Tip") + ": "))
                .append(t("use 'language pt' ou 'language eng' para trocar.", "use 'language pt' or 'language eng' to switch."))
                .append("\n");

        for (var section : sections) {
            sb.append(styled(BOLD_CYAN, "\n  " + t(section.titlePt(), section.titleEn()) + "\n"));
            for (var item : section.items()) {
                sb.append(styled(CYAN, "  " + String.format("%-52s", item.command())));
                sb.append(styled(DIM, " - " + t(item.descPt(), item.descEn()) + "\n"));
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private List<HelpSection> buildSections() {
        return List.of(
                section("Requisicoes HTTP", "HTTP Requests",
                        item("get <url> [--param k=v&a=b]", "Envia GET.", "Sends GET."),
                        item("post <url> --body '{...}' [--param ...]", "Envia POST JSON.", "Sends JSON POST."),
                        item("put <url> --body '{...}' [--param ...]", "Envia PUT JSON.", "Sends JSON PUT."),
                        item("patch <url> --body '{...}' [--param ...]", "Envia PATCH JSON.", "Sends JSON PATCH."),
                        item("delete <url> [--param ...]", "Envia DELETE.", "Sends DELETE."),
                        item("head <url>", "Envia HEAD.", "Sends HEAD."),
                        item("options <url>", "Envia OPTIONS.", "Sends OPTIONS."),
                        item("send <method> <url> [--header ...] [--body ...]", "Request customizado.", "Custom request."),
                        item("bench <url> [--method] [--calls] [--concurrency] [--warmup]", "Benchmark dedicado com RPS e percentis.", "Dedicated benchmark with RPS and percentiles.")
                ),
                section("Resposta", "Response",
                        item("response", "Mostra a resposta completa.", "Shows full response."),
                        item("body", "Mostra apenas o body.", "Shows only the body."),
                        item("response-headers", "Mostra headers da resposta.", "Shows response headers."),
                        item("curl", "Gera cURL do ultimo request.", "Generates cURL from last request."),
                        item("import-curl \"curl ...\" [--dry-run]", "Importa e executa cURL.", "Imports and executes cURL."),
                        item("save-response <arquivo>", "Salva body em arquivo.", "Saves body to file.")
                ),
                section("Headers Padrao", "Default Headers",
                        item("set-header <key> <value>", "Define header padrao.", "Sets default header."),
                        item("unset-header <key>", "Remove header padrao.", "Removes default header."),
                        item("headers", "Lista headers padrao.", "Lists default headers."),
                        item("clear-headers", "Limpa headers padrao.", "Clears default headers."),
                        item("bearer <token>", "Define Authorization Bearer.", "Sets Authorization Bearer.")
                ),
                section("Ambientes", "Environments",
                        item("env-create <nome>", "Cria ambiente.", "Creates environment."),
                        item("env-set <env> <key> <value>", "Define variavel.", "Sets variable."),
                        item("env-unset <env> <key>", "Remove variavel.", "Removes variable."),
                        item("env-use <nome>", "Ativa ambiente.", "Activates environment."),
                        item("env-clear", "Desativa ambiente.", "Deactivates environment."),
                        item("envs / env-show <nome> / env-rm <nome>", "Lista, mostra e remove ambientes.", "Lists, shows and removes environments.")
                ),
                section("Colecoes", "Collections",
                        item("col-create <nome>", "Cria colecao.", "Creates collection."),
                        item("col-add <colecao> <nomeRequest>", "Salva ultimo request.", "Saves last request."),
                        item("col-show <colecao> / cols", "Mostra/lista colecoes.", "Shows/lists collections."),
                        item("col-run <colecao> <indice>", "Executa request salvo.", "Runs saved request."),
                        item("col-run-all <colecao>", "Executa todos os requests.", "Runs all requests."),
                        item("col-rm-req <colecao> <indice> / col-rm <nome>", "Remove request/colecao.", "Removes request/collection.")
                ),
                section("Historico", "History",
                        item("history [--limit N]", "Mostra historico.", "Shows history."),
                        item("replay <indice>", "Reexecuta request do historico.", "Replays history request."),
                        item("clear-history", "Limpa historico.", "Clears history.")
                ),
                section("Templates", "Templates",
                        item("{{$timestamp}} / {{$isoTimestamp}} / {{$uuid}} / {{$cpf}}", "Variaveis dinamicas em URL/header/body.", "Dynamic variables in URL/header/body."),
                        item("{{chave}} ou {{env.chave}}", "Variavel do ambiente ativo.", "Variable from active environment."),
                        item("{{last.status}} / {{last.header.content-type}}", "Usa dados da ultima resposta.", "Uses data from last response."),
                        item("{{last.body.user.id}}", "Extrai campo JSON da ultima resposta.", "Extracts JSON field from last response.")
                ),
                section("Mock Server", "Mock Server",
                        item("mock-start [--port 8089] / mock-stop / mock-status", "Controla servidor mock.", "Controls mock server."),
                        item("mock-add <method> <path> [--status --body --header --delay --timeout-config --scenario --desc]", "Adiciona rota.", "Adds route."),
                        item("mock-add-json <method> <path> --body '{...}' [--timeout-config --scenario]", "Atalho para rota JSON.", "Shortcut for JSON route."),
                        item("mock-add-crud <basePath>", "Cria CRUD completo.", "Creates full CRUD."),
                        item("mock-from-response <path> [--method GET]", "Cria rota da ultima resposta.", "Creates route from last response."),
                        item("mock-list / mock-show <i> / mock-edit <i> ...", "Lista, mostra e edita rotas.", "Lists, shows and edits routes."),
                        item("mock-rm <i> / mock-clear", "Remove rota(s).", "Removes route(s)."),
                        item("mock-logs [--limit N] / mock-clear-logs", "Mostra/limpa logs do mock.", "Shows/clears mock logs."),
                        item("{{param.id}} / {{query.page}} / {{faker.uuid}} / {{faker.cpf}}", "Templates dinamicos para body/headers mock.", "Dynamic templates for mock body/headers.")
                )
        );
    }

    private HelpSection section(String titlePt, String titleEn, HelpItem... items) {
        return new HelpSection(titlePt, titleEn, List.of(items));
    }

    private HelpItem item(String command, String descPt, String descEn) {
        return new HelpItem(command, descPt, descEn);
    }

    private record HelpSection(String titlePt, String titleEn, List<HelpItem> items) {}

    private record HelpItem(String command, String descPt, String descEn) {}
}
