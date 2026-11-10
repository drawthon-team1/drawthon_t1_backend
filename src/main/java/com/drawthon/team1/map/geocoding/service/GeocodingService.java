package com.drawthon.team1.map.geocoding.service;

import com.drawthon.team1.map.geocoding.client.KakaoGeocodingClient;
import com.drawthon.team1.map.geocoding.dto.GeocodingResponse;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {

    private final KakaoGeocodingClient kakaoGeocodingClient;

    public GeocodingService(KakaoGeocodingClient kakaoGeocodingClient) {
        this.kakaoGeocodingClient = kakaoGeocodingClient;
    }

    public GeocodingResponse geocode(String address) {
        return kakaoGeocodingClient.geocode(address.trim());
    }
}
