package com.cadac.stone_inscription.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "DashboardCountsResponse", description = "Public aggregate counters for the application dashboard.")
public class DashboardCountsResponse {

    @Schema(description = "Registered users.", example = "128")
    private Integer totalUsers;

    @Schema(description = "Published posts.", example = "42")
    private Integer totalPosts;

    @Schema(description = "Uploaded image objects.", example = "280")
    private Integer totalImages;

    @Schema(description = "Posts with extracted geolocation metadata.", example = "31")
    private Integer totalGeoTaggedPosts;

    @Schema(description = "Posts with an English translation available.", example = "17")
    private Integer totalTranslations;

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Integer getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(Integer totalPosts) {
        this.totalPosts = totalPosts;
    }

    public Integer getTotalImages() {
        return totalImages;
    }

    public void setTotalImages(Integer totalImages) {
        this.totalImages = totalImages;
    }

    public Integer getTotalGeoTaggedPosts() {
        return totalGeoTaggedPosts;
    }

    public void setTotalGeoTaggedPosts(Integer totalGeoTaggedPosts) {
        this.totalGeoTaggedPosts = totalGeoTaggedPosts;
    }

    public Integer getTotalTranslations() {
        return totalTranslations;
    }

    public void setTotalTranslations(Integer totalTranslations) {
        this.totalTranslations = totalTranslations;
    }
}
