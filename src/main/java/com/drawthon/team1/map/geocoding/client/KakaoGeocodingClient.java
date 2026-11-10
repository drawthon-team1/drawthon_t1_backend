package com.drawthon.team1.map.geocoding.client;

import com.drawthon.team1.map.geocoding.dto.GeocodingResponse;
import com.drawthon.team1.map.geocoding.dto.KakaoAddressSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KakaoGeocodingClient {

    private static final String KAKAO_API_BASE_URL = "https://dapi.kakao.com";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoGeocodingClient(
            RestClient.Builder restClientBuilder,
            @Value("${kakao.rest-api-key:}") String restApiKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(KAKAO_API_BASE_URL)
                .build();
        this.restApiKey = restApiKey;
    }

    public GeocodingResponse geocode(String address) {
        validateApiKey();

        try {
            KakaoAddressSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(KakaoAddressSearchResponse.class);

            KakaoAddressSearchResponse.Document document = getFirstDocument(response, address);

            return new GeocodingResponse(
                    document.addressName(),
                    parseCoordinate(document.y()),
                    parseCoordinate(document.x())
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException | NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "카카오 주소 검색 API 호출에 실패했습니다.",
                    exception
            );
        }
    }

    private void validateApiKey() {
        if (restApiKey == null || restApiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "KAKAO_REST_API_KEY 환경변수가 설정되지 않았습니다."
            );
        }
    }

    private KakaoAddressSearchResponse.Document getFirstDocument(
            KakaoAddressSearchResponse response,
            String address
    ) {
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "주소에 해당하는 좌표를 찾을 수 없습니다. address=" + address
            );
        }

        return response.documents().getFirst();
    }

    private Double parseCoordinate(String coordinate) {
        return Double.parseDouble(coordinate);
    }
}
