package lv.bootcamp.shelter.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RestClientPartnerRegistryClientTest {
    private MockWebServer server;
    private RestClientPartnerRegistryClient client;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void configureServer() throws IOException {
        this.server = new MockWebServer();
        server.start();
        this.client = new RestClientPartnerRegistryClient(RestClient.builder(), server.url("/").toString());
    }

    @AfterEach
    void disableServer() throws IOException{
        this.server.shutdown();
    }

    @Test
    void client_ShouldReturnSuccessfulPartnerResponse() throws JsonProcessingException {
        PartnerRegistryResult successfulResult =
                new PartnerRegistryResult(true, "Some name");

        this.server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mapper.writeValueAsString(successfulResult)));

        PartnerRegistryResult result = this.client.lookup("123");

        assertThat(result.registered()).isTrue();
        assertThat(result.partnerShelterName()).isEqualTo("Some name");
    }

    @Test
    void client_ShouldReturnFailPartnerResponse() {

        this.server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "text/plain")
                .setBody("No found a partner with the id you provided."));

        assertThatThrownBy(() -> this.client.lookup("123"))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageContaining("404 Not Found");
    }

}
