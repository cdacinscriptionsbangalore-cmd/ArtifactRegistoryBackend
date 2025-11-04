package com.cadac.stone_inscription.post.util;

import java.io.InputStream;
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

import dev.brachtendorf.jimagehash.hash.Hash;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
public class ImageMetadataGeolocationWithPhash {

    @Value("${geolocation.api.url}")
    private  String BASE_URL;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GeoApiResponse {

        private Address address;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {

        private String city;
        private String state;
        private String country;

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
        private String city;
        private String state;
        private String country;

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

                                Address address = getAddress(latitude, longitude);
                                info.setCity(address.getCity());
                                info.setState(address.getState());
                                info.setCountry(address.getCountry());
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

    public Address getAddress(double latitude, double longitude) {

        String url = BASE_URL + "?lat=" + latitude + "&lon=" + longitude + "&format=json";

        try {
            GeoApiResponse response = new RestTemplate().getForObject(url, GeoApiResponse.class);
            if (response != null && response.getAddress() != null) {
                return response.getAddress();
            } else {

                throw new StoneInscriptionException("Geo Location Not Found invalid lat long", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            throw new StoneInscriptionException("Geo Location Failed to Map", HttpStatus.UNPROCESSABLE_ENTITY);
        }

    }

}
