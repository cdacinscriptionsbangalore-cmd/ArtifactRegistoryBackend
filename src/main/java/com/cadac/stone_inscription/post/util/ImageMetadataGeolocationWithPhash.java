package com.cadac.stone_inscription.post.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.exception.StoneInscriptionException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.brachtendorf.jimagehash.hash.Hash;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
public class ImageMetadataGeolocationWithPhash {

    @Value("${geolocation.api.url}")
    private String BASE_URL;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeoApiResponse {

        @Builder.Default
        @JsonProperty("place_id")
        private Long placeId = 0L;

        @Builder.Default
        private String licence = "";

        @Builder.Default
        @JsonProperty("osm_type")
        private String osmType = "";

        @Builder.Default
        @JsonProperty("osm_id")
        private Long osmId = 0L;

        @Builder.Default
        @JsonProperty("class")
        private String clazz = ""; // "class" is reserved in Java

        @Builder.Default
        private String type = "";

        @Builder.Default
        @JsonProperty("place_rank")
        private Integer placeRank = 0;

        @Builder.Default
        private Double importance = 0.0;

        @Builder.Default
        @JsonProperty("addresstype")
        private String addressType = "";

        @Builder.Default
        private String name = "";

        @Builder.Default
        @JsonProperty("display_name")
        private String displayName = "";

        @Builder.Default
        private List<String> boundingbox = new ArrayList<>();

        private Address address;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {

        @Builder.Default
        private String amenity = "";

        @Builder.Default
        private String road = "";

        @Builder.Default
        private String neighbourhood = "";

        @Builder.Default
        private String suburb = "";

        @Builder.Default
        @JsonProperty("city_district")
        private String cityDistrict = "";

        @Builder.Default
        private String city = "";

        @Builder.Default
        private String county = "";

        @Builder.Default
        @JsonProperty("state_district")
        private String stateDistrict = "";

        @Builder.Default
        private String state = "";

        @Builder.Default
        @JsonProperty("ISO3166-2-lvl4")
        private String iso3166Lvl4 = "";

        @Builder.Default
        private String postcode = "";

        @Builder.Default
        private String country = "";

        @Builder.Default
        @JsonProperty("country_code")
        private String countryCode = "";
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeoCordinates {
        String latitude;
        String longitude;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageMetaAndInfo {
        private byte[] file;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private Hash pHash;
        private GeoCordinates geocCordinates;
        private GeoApiResponse geoApiResponse;
        // private String city;
        // private String state;
        // private String country;

    }

    public List<ImageMetaAndInfo> getGeoLocationWithIamgeMetaandInfo(MultipartFile... files) {

        List<ImageMetaAndInfo> ls = new LinkedList<>();

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    ImageMetaAndInfo info = ImageMetaAndInfo.builder().fileName(file.getOriginalFilename())
                            .fileSize(file.getSize()).file(file.getBytes())
                            .contentType(file.getContentType()).pHash(ImagePhash.genratePhash(file)).build();

                    try (InputStream inputStream = file.getInputStream()) {
                        Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

                        // Extract GPS directory
                        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

                        if (gpsDirectory != null) {
                            GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                            if (geoLocation != null && !geoLocation.isZero()) {

                                Double latitude = geoLocation.getLatitude();
                                Double longitude = geoLocation.getLongitude();

                                info.setGeoApiResponse(getGeolocation(latitude, longitude));

                                // Address address = getAddress(latitude, longitude);
                                // info.setCity(address.getCity());
                                // info.setState(address.getState());
                                // info.setCountry(address.getCountry());
                                info.setGeocCordinates(
                                        (GeoCordinates.builder().latitude(latitude.toString())
                                                .longitude(longitude.toString()).build()));

                            } else {
                                System.out.println("No valid GPS data found in image.");
                            }
                        } else {
                            System.out.println("null hai bhai");
                        }
                    }
                    ls.add(info);
                }

            }

            // Now continue saving your post with file + geo location

        } catch (Exception e) {
            e.printStackTrace();

        }

        return ls;
    }

    public GeoApiResponse getGeolocation(double latitude, double longitude) {

        String url = BASE_URL + "?lat=" + latitude + "&lon=" + longitude + "&format=json";

        try {
            GeoApiResponse response = new RestTemplate().getForObject(url, GeoApiResponse.class);
            if (response != null && response.getAddress() != null) {
                return response;
            } else {

                throw new StoneInscriptionException("Geo Location Not Found invalid lat long", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {

            throw new StoneInscriptionException("Geo Location Failed to Map", HttpStatus.UNPROCESSABLE_ENTITY);
        }

    }

}
