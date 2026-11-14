package com.drawthon.team1.posting.client;

import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.entity.Post;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolunteerApiClientTest {
    private static final String SERVICE_KEY = "a+b/c==";

    private HttpServer server;
    private final AtomicReference<String> requestPath = new AtomicReference<>();
    private final AtomicReference<String> rawQuery = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody = "";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/xml");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private VolunteerApiClient client(String key) {
        return new VolunteerApiClient("http://127.0.0.1:" + server.getAddress().getPort() + "/svc", key);
    }

    private static String success(String items) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response><header><resultCode>00</resultCode>"
                + "<resultMsg>NORMAL SERVICE</resultMsg></header><body><items>" + items + "</items>"
                + "<numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>1</totalCount></body></response>";
    }

    private Map<String, String> decodedQuery() {
        return Arrays.stream(rawQuery.get().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(p -> p[0], p -> URLDecoder.decode(p[1], StandardCharsets.UTF_8)));
    }

    @Test
    void fetchDetailMapsItemAndKeepsKoreanWithoutCharsetHeader() {
        responseBody = success("<item><progrmRegistNo>3425935</progrmRegistNo><progrmSj>환경 정화 봉사</progrmSj>"
                + "<progrmBgnde>20260301</progrmBgnde><noticeEndde>2026-05-31</noticeEndde>"
                + "<rcritNmpr>5</rcritNmpr><appTotal>2</appTotal><actBeginTm>9</actBeginTm><email> </email></item>");

        Post post = client(SERVICE_KEY).fetchDetail("3425935").orElseThrow();

        assertThat(post.getProgramRegistrationNumber()).isEqualTo("3425935");
        assertThat(post.getProgramTitle()).isEqualTo("환경 정화 봉사");
        assertThat(post.getProgramStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(post.getRecruitmentEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(post.getRecruitmentCapacity()).isEqualTo(5);
        assertThat(post.getActivityStartTime()).isEqualTo("9");
        assertThat(post.getManagerEmail()).isNull();
        assertThat(requestPath.get()).isEqualTo("/svc/getVltrPartcptnItem");
    }

    @Test
    void serviceKeyWithReservedCharactersIsPercentEncodedOnce() {
        responseBody = success("");

        client(SERVICE_KEY).fetchDetail("1");

        assertThat(rawQuery.get()).contains("serviceKey=a%2Bb%2Fc%3D%3D");
        assertThat(decodedQuery()).containsEntry("serviceKey", SERVICE_KEY);
    }

    @Test
    void fetchDetailReturnsEmptyWhenNoItem() {
        responseBody = success("");

        assertThat(client(SERVICE_KEY).fetchDetail("1")).isEmpty();
    }

    @Test
    void searchSendsKeywordAndMapsItems() {
        responseBody = success("<item><progrmRegistNo>1</progrmRegistNo><progrmSj>첫째</progrmSj><url>http://a</url></item>"
                + "<item><progrmRegistNo>2</progrmRegistNo><progrmSj>둘째</progrmSj></item>");

        List<PostingExternalDTO> results = client(SERVICE_KEY).search("환경 봉사", 2, 20);

        assertThat(results).extracting(PostingExternalDTO::getProgramTitle).containsExactly("첫째", "둘째");
        assertThat(results.getFirst().getProgramDetailUrl()).isEqualTo("http://a");
        assertThat(requestPath.get()).isEqualTo("/svc/getVltrSearchWordList");
        assertThat(decodedQuery())
                .containsEntry("keyword", "환경 봉사")
                .containsEntry("schCateGu", "all")
                .containsEntry("pageNo", "2")
                .containsEntry("numOfRows", "20");
    }

    @Test
    void searchOmitsBlankKeyword() {
        responseBody = success("");

        client(SERVICE_KEY).search(" ", 1, 10);

        assertThat(decodedQuery()).doesNotContainKeys("keyword", "schCateGu");
    }

    private void assertUpstreamFailure(HttpStatus expected, String key) {
        assertThatThrownBy(() -> client(key).fetchDetail("1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(expected);
                    assertThat(e.getMessage()).doesNotContain(SERVICE_KEY);
                });
    }

    @Test
    void gatewayErrorBecomesBadGateway() {
        status = 401;
        responseBody = "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE_KEY_IS_NULL</errMsg></cmmMsgHeader></OpenAPI_ServiceResponse>";

        assertUpstreamFailure(HttpStatus.BAD_GATEWAY, SERVICE_KEY);
    }

    @Test
    void gatewayErrorWithStatus200BecomesBadGateway() {
        responseBody = "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg></cmmMsgHeader></OpenAPI_ServiceResponse>";

        assertUpstreamFailure(HttpStatus.BAD_GATEWAY, SERVICE_KEY);
    }

    @Test
    void failedResultCodeBecomesBadGateway() {
        responseBody = "<response><header><resultCode>99</resultCode></header></response>";

        assertUpstreamFailure(HttpStatus.BAD_GATEWAY, SERVICE_KEY);
    }

    @Test
    void malformedDateBecomesBadGateway() {
        responseBody = success("<item><progrmBgnde>not-a-date</progrmBgnde></item>");

        assertUpstreamFailure(HttpStatus.BAD_GATEWAY, SERVICE_KEY);
    }

    @Test
    void doctypeIsRejected() {
        responseBody = "<!DOCTYPE response [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<response><header><resultCode>00</resultCode></header><body><items><item><progrmSj>&x;</progrmSj></item></items></body></response>";

        assertUpstreamFailure(HttpStatus.BAD_GATEWAY, SERVICE_KEY);
    }

    @Test
    void connectionFailureBecomesBadGateway() {
        server.stop(0);

        assertUpstreamFailure(HttpStatus.BAD_GATEWAY, SERVICE_KEY);
    }

    @Test
    void missingServiceKeyBecomesServiceUnavailableWithoutCalling() {
        assertUpstreamFailure(HttpStatus.SERVICE_UNAVAILABLE, "");
        assertThat(requestPath.get()).isNull();
    }
}
