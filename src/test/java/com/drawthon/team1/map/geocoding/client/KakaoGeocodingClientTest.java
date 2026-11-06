package com.drawthon.team1.map.geocoding.client;

import com.drawthon.team1.map.geocoding.dto.GeocodingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoGeocodingClientTest {

    private static final String ADDRESS = "서울특별시 영등포구 여의동로 330";

    private MockRestServiceServer mockServer;
    private KakaoGeocodingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoGeocodingClient(builder, "test-rest-api-key");
    }

    @Test
    void convertsAddressToCoordinates() {
        mockServer.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/v2/local/search/address.json");
                    assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                            .isEqualTo("KakaoAK test-rest-api-key");
                    assertThat(URLDecoder.decode(
                            request.getURI().getRawQuery(),
                            StandardCharsets.UTF_8
                    )).isEqualTo("query=" + ADDRESS);
                })
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {
                              "address_name": "서울특별시 영등포구 여의동로 330",
                              "x": "126.9349",
                              "y": "37.5285"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        GeocodingResponse response = client.geocode(ADDRESS);

        assertThat(response.address()).isEqualTo(ADDRESS);
        assertThat(response.latitude()).isEqualTo(37.5285);
        assertThat(response.longitude()).isEqualTo(126.9349);
        mockServer.verify();
    }

    @Test
    void throwsNotFoundWhenAddressDoesNotMatch() {
        mockServer.expect(request -> assertThat(request.getURI().getPath())
                        .isEqualTo("/v2/local/search/address.json"))
                .andRespond(withSuccess("{\"documents\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.geocode("존재하지 않는 주소"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");

        mockServer.verify();
    }

    @Test
    void throwsServiceUnavailableWhenApiKeyIsMissing() {
        KakaoGeocodingClient clientWithoutKey = new KakaoGeocodingClient(
                RestClient.builder(),
                ""
        );

        assertThatThrownBy(() -> clientWithoutKey.geocode(ADDRESS))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503 SERVICE_UNAVAILABLE");
    }
}
