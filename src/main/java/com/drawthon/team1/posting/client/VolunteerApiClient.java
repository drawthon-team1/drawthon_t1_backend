package com.drawthon.team1.posting.client;

import com.drawthon.team1.posting.dto.PostingExternalDTO;
import com.drawthon.team1.posting.entity.Post;
import com.drawthon.team1.posting.entity.RewardType;
import com.drawthon.team1.posting.entity.WorkType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 행정안전부 봉사참여정보서비스(1365) 조회 클라이언트
 * https://www.data.go.kr/data/15157582/openapi.do
 *
 * - 현재 조회만 가능
 */

@Slf4j
@Component
public class VolunteerApiClient {
    private static final String ROOT_ELEMENT = "response";
    private static final String SUCCESS_CODE = "00";
    private static final String UPSTREAM_ERROR_MESSAGE = "Failed to call 1365 api";

    private final RestClient restClient;
    private final String serviceKey;
    /** 1365 검색에 항상 붙이는 상위분야코드. 0600 = 농어촌 봉사. 비우면 분야 제한 없이 검색한다 */
    private final String upperCategoryCode;

    public VolunteerApiClient(@Value("${volunteer.api.base-url}") String baseUrl,
                              @Value("${volunteer.api.service-key}") String serviceKey,
                              @Value("${volunteer.api.upper-category-code:0600}") String upperCategoryCode) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.serviceKey = serviceKey;
        this.upperCategoryCode = upperCategoryCode;
    }

    /**
     * 상세 조회 결과를 저장 전 Post로 변환한다.
     * @return 1365에 해당 프로그램이 없으면 빈 값
     */
    /** 인증키가 설정돼 있어 1365를 호출할 수 있는지 */
    public boolean isConfigured() {
        return StringUtils.hasText(serviceKey);
    }

    public Optional<Post> fetchDetail(String programRegisterNo) {
        List<Map<String, String>> items = fetchItems("getVltrPartcptnItem", Map.of("progrmRegistNo", programRegisterNo));
        return items.stream().findFirst().map(item -> toPost(programRegisterNo, item));
    }

    /** 설정한 상위분야(기본 0600 농어촌 봉사) 안에서만 검색한다. */
    public List<PostingExternalDTO> search(String keyword, int pageNo, int numOfRows) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", pageNo);
        params.put("numOfRows", numOfRows);
        if (StringUtils.hasText(upperCategoryCode)) {
            params.put("upperClCode", upperCategoryCode.trim());
        }
        if (StringUtils.hasText(keyword)) {
            params.put("keyword", keyword);
            params.put("schCateGu", "all");
        }
        return fetchItems("getVltrSearchWordList", params).stream()
                .map(VolunteerApiClient::toExternalDTO)
                .toList();
    }

    private List<Map<String, String>> fetchItems(String operation, Map<String, Object> params) {
        if (!StringUtils.hasText(serviceKey)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "1365 API 인증키가 설정되지 않았습니다.");
        }

        // 인증키가 예외 메시지와 로그에 남지 않도록 원본 예외는 전달하지 않는다.
        try {
            byte[] body = request(operation, params);
            return parseItems(body);
        } catch (RestClientResponseException e) {
            log.warn("1365 API error: operation={}, status={}, body={}", operation, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.warn("1365 API request failed: operation={}, cause={}", operation, e.getClass().getSimpleName());
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.warn("1365 API response parse failed: operation={}, cause={}", operation, e.getClass().getSimpleName());
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, UPSTREAM_ERROR_MESSAGE);
    }

    /**
     * 응답에 charset이 없어 문자열로 받으면 한글이 깨지므로,
     * XML 선언의 인코딩을 파서가 읽도록 byte[]로 받는다.
     */
    private byte[] request(String operation, Map<String, Object> params) {
        Map<String, Object> variables = new HashMap<>(params);
        variables.put("serviceKey", serviceKey);

        return restClient.get()
                .uri(builder -> {
                    builder.path("/" + operation);
                    variables.keySet().forEach(name -> builder.queryParam(name, "{" + name + "}"));
                    return builder.build(variables);
                })
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .body(byte[].class);
    }

    private List<Map<String, String>> parseItems(byte[] body) throws ParserConfigurationException, SAXException, IOException {
        if (body == null) {
            throw new SAXException("empty response");
        }

        Document document = newDocumentBuilder().parse(new ByteArrayInputStream(body));
        Element root = document.getDocumentElement();
        String resultCode = firstText(root, "resultCode");
        if (!ROOT_ELEMENT.equals(root.getTagName()) || !SUCCESS_CODE.equals(resultCode)) {
            throw new SAXException("unexpected result: root=" + root.getTagName() + ", resultCode=" + resultCode);
        }

        NodeList itemNodes = root.getElementsByTagName("item");
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < itemNodes.getLength(); i++) {
            items.add(toMap(itemNodes.item(i)));
        }
        return items;
    }

    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        return builder;
    }

    private static String firstText(Element root, String tagName) {
        NodeList nodes = root.getElementsByTagName(tagName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
    }

    private static Map<String, String> toMap(Node item) {
        Map<String, String> values = new LinkedHashMap<>();
        NodeList children = item.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String text = child.getTextContent().trim();
            if (child.getNodeType() == Node.ELEMENT_NODE && !text.isEmpty()) {
                values.put(child.getNodeName(), text);
            }
        }
        return values;
    }

    private static Post toPost(String programRegistrationNumber, Map<String, String> item) {
        try {
            return Post.builder()
                    .programRegistrationNumber(programRegistrationNumber)
                    .programTitle(item.get("progrmSj"))
                    .programContent(item.get("progrmCn"))
                    .programStartDate(toDate(item.get("progrmBgnde")))
                    .programEndDate(toDate(item.get("progrmEndde")))
                    .activityStartTime(item.get("actBeginTm"))
                    .activityEndTime(item.get("actEndTm"))
                    .recruitmentStartDate(toDate(item.get("noticeBgnde")))
                    .recruitmentEndDate(toDate(item.get("noticeEndde")))
                    .recruitmentCapacity(toInteger(item.get("rcritNmpr")))
                    .registeringOrganizationName(item.get("nanmmbyNm"))
                    .activityPlace(item.get("actPlace"))
                    .rewardType(RewardType.VOLUNTEER_HOURS)
                    .reward(Post.volunteerHours(item.get("actBeginTm"), item.get("actEndTm")))
                    .address(Post.normalizeAddress(item.get("areaAddress1")))
                    .workType(WorkType.infer(item.get("progrmSj"), item.get("progrmCn")))
                    .build();
        } catch (DateTimeParseException | NumberFormatException e) {
            log.warn("1365 API item conversion failed: programRegistrationNumber={}, cause={}", programRegistrationNumber, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "1365 API 응답 형식이 올바르지 않습니다.");
        }
    }

    private static PostingExternalDTO toExternalDTO(Map<String, String> item) {
        return PostingExternalDTO.builder()
                .programRegistrationNumber(item.get("progrmRegistNo"))
                .programTitle(item.get("progrmSj"))
                .registeringOrganizationName(item.get("nanmmbyNm"))
                .programStartDate(item.get("progrmBgnde"))
                .programEndDate(item.get("progrmEndde"))
                .recruitmentStatus(item.get("progrmSttusSe"))
                .recruitmentStartDate(item.get("noticeBgnde"))
                .recruitmentEndDate(item.get("noticeEndde"))
                .activityPlace(item.get("actPlace"))
                .programDetailUrl(item.get("url"))
                .build();
    }

    /** 20260301, 2026-03-01 형식을 모두 허용한다. */
    private static LocalDate toDate(String value) {
        return value == null ? null : LocalDate.parse(value.replaceAll("\\D", ""), DateTimeFormatter.BASIC_ISO_DATE);
    }

    private static Integer toInteger(String value) {
        return value == null ? null : Integer.valueOf(value);
    }
}
