package com.hidariapi.shell;

import com.hidariapi.service.ApiImportService;
import com.hidariapi.service.LanguageService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Files;
import java.nio.file.Path;

@ShellComponent
public class ImportCommands extends LocalizedSupport {

    private final ApiImportService apiImportService;

    public ImportCommands(ApiImportService apiImportService, LanguageService lang) {
        super(lang);
        this.apiImportService = apiImportService;
    }

    @ShellMethod(key = "import-openapi", value = "Importa OpenAPI/Swagger e gera collection + mocks opcionais")
    public String importOpenApi(
            @ShellOption(help = "Arquivo OpenAPI JSON (use @/abs/path se quiser)") String file,
            @ShellOption(value = "--collection", defaultValue = ShellOption.NULL, help = "Nome da collection destino") String collection,
            @ShellOption(value = "--base-url", defaultValue = ShellOption.NULL, help = "Override de base URL") String baseUrl,
            @ShellOption(value = "--mocks", defaultValue = "true", help = "Gerar mocks iniciais automaticamente") boolean mocks) {
        try {
            String content = readFile(file);
            var result = apiImportService.importOpenApi(content, collection, baseUrl, mocks);
            var sb = new StringBuilder();
            sb.append(styled(GREEN, t("OpenAPI importado com sucesso.", "OpenAPI imported successfully."))).append("\n");
            sb.append(styled(DIM, "  " + t("Collection", "Collection") + ": ")).append(styled(CYAN, result.collectionName())).append("\n");
            sb.append(styled(DIM, "  " + t("Requests", "Requests") + ": ")).append(styled(CYAN, String.valueOf(result.importedRequests()))).append("\n");
            sb.append(styled(DIM, "  " + t("Mocks criados", "Mocks created") + ": ")).append(styled(CYAN, String.valueOf(result.createdMocks())));
            return sb.toString();
        } catch (Exception e) {
            return styled(RED, t("Erro ao importar OpenAPI: ", "Error importing OpenAPI: ") + e.getMessage());
        }
    }

    @ShellMethod(key = "import-postman", value = "Importa collection do Postman")
    public String importPostman(
            @ShellOption(help = "Arquivo Postman Collection JSON") String file,
            @ShellOption(value = "--collection", defaultValue = ShellOption.NULL, help = "Nome da collection destino") String collection) {
        try {
            String content = readFile(file);
            var result = apiImportService.importPostman(content, collection);
            return styled(GREEN, t("Collection Postman importada.", "Postman collection imported."))
                    + "\n" + styled(DIM, "  " + t("Collection", "Collection") + ": ")
                    + styled(CYAN, result.collectionName())
                    + "\n" + styled(DIM, "  " + t("Requests", "Requests") + ": ")
                    + styled(CYAN, String.valueOf(result.importedRequests()));
        } catch (Exception e) {
            return styled(RED, t("Erro ao importar Postman: ", "Error importing Postman: ") + e.getMessage());
        }
    }

    private String readFile(String pathInput) throws Exception {
        String filePath = pathInput;
        if (filePath.startsWith("@")) filePath = filePath.substring(1);
        return Files.readString(Path.of(filePath));
    }
}
