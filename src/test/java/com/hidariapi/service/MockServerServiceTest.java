package com.hidariapi.service;

import com.hidariapi.model.HttpMethod;
import com.hidariapi.model.MockRoute;
import com.hidariapi.store.MockStore;
import com.hidariapi.util.AppPaths;
import com.hidariapi.util.TestFs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class MockServerServiceTest {

    private MockServerService service;

    @BeforeEach
    void setup() throws Exception {
        TestFs.deleteRecursively(AppPaths.configDir());
        service = new MockServerService(new MockStore());
    }

    @AfterEach
    void tearDown() {
        if (service != null && service.isRunning()) {
            service.stop();
        }
    }

    @Test
    void servesTemplatedMockWithParamQueryAndFaker() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }

        service.addRoute(MockRoute.json(HttpMethod.GET, "/users/{id}",
                "{\"id\":\"{{param.id}}\",\"q\":\"{{query.q}}\",\"cpf\":\"{{faker.cpf}}\",\"cnpj\":\"{{faker.cnpj}}\",\"cep\":\"{{faker.cep}}\",\"phone\":\"{{faker.phone_br}}\",\"phoneDdd\":\"{{phone_br.ddd}}\",\"phoneNumber\":\"{{phone_br.number}}\",\"name\":\"{{faker.full_name_br}}\",\"firstName\":\"{{full_name_br.first_name}}\",\"middleName\":\"{{full_name_br.middle_name}}\",\"lastName\":\"{{full_name_br.last_name}}\",\"address\":\"{{faker.address_br}}\",\"addressNumber\":\"{{address_br.number}}\",\"addressCity\":\"{{address_br.city}}\",\"addressState\":\"{{address_br.state}}\",\"addressCep\":\"{{address_br.cep}}\"}"));
        service.start(port);

        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder().uri(new URI("http://localhost:" + port + "/users/10?q=abc")).GET().build();
        var res = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"id\":\"10\""));
        assertTrue(res.body().contains("\"q\":\"abc\""));
        assertTrue(res.body().matches(".*\\\"cpf\\\":\\\"\\d{11}\\\".*"));
        assertTrue(res.body().matches(".*\\\"cnpj\\\":\\\"\\d{14}\\\".*"));
        assertTrue(res.body().matches(".*\\\"cep\\\":\\\"\\d{8}\\\".*"));
        assertTrue(res.body().matches(".*\\\"phone\\\":\\\"\\d{2}9\\d{8}\\\".*"));
        assertTrue(res.body().matches(".*\\\"phoneDdd\\\":\\\"\\d{2}\\\".*"));
        assertTrue(res.body().matches(".*\\\"phoneNumber\\\":\\\"9\\d{8}\\\".*"));
        assertTrue(res.body().matches(".*\\\"name\\\":\\\"[^\\\"]+\\\".*"));
        assertTrue(res.body().matches(".*\\\"firstName\\\":\\\"[A-Za-z]+\\\".*"));
        assertTrue(res.body().matches(".*\\\"middleName\\\":\\\"[A-Za-z]+\\\".*"));
        assertTrue(res.body().matches(".*\\\"lastName\\\":\\\"[A-Za-z]+\\\".*"));
        assertTrue(res.body().matches(".*\\\"address\\\":\\\"[^\\\"]*\\d{8}\\\".*"));
        assertTrue(res.body().matches(".*\\\"addressNumber\\\":\\\"\\d+\\\".*"));
        assertTrue(res.body().matches(".*\\\"addressCity\\\":\\\"[^\\\"]+\\\".*"));
        assertTrue(res.body().matches(".*\\\"addressState\\\":\\\"[A-Z]{2}\\\".*"));
        assertTrue(res.body().matches(".*\\\"addressCep\\\":\\\"\\d{8}\\\".*"));
        assertFalse(service.getRequestLogs(10).isEmpty());
    }
}
