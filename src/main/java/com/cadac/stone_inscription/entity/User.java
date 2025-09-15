package com.cadac.stone_inscription.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users_profile")
public class User {

  @Id
  @JsonProperty("_id")
      @JsonSerialize(using = ToStringSerializer.class)
private ObjectId id;

  @Field("authid")
  @JsonProperty("authId")
      @JsonSerialize(using = ToStringSerializer.class)
  private ObjectId authId;

  @Field("name")
  @JsonProperty("name")
  @NotBlank
  @Indexed
  private String name;

  @Field("email")
  @JsonProperty("email")
  @Email
  @Indexed(unique = true)
  private String email;

  @Field("profileImage")
  @JsonProperty("profileImage")
  private String profileImage;

  @Field("imagesUploaded")
  @JsonProperty("imagesUploaded")
  @Builder.Default
  private Integer imagesUploaded = 0;

  @Field("upvotesReceived")
  @JsonProperty("upvotesReceived")
  @Builder.Default
  private Integer upvotesReceived = 0;

  @Field("followers")
  @JsonProperty("followers")
  @Builder.Default
  private Integer followers = 0;

  @Field("points")
  @JsonProperty("points")
  @Builder.Default
  private Integer points = 0;

  @CreatedDate
  @Field("createdAt")
  @JsonProperty("createdAt")
  @Indexed
  private Date createdAt;

  @LastModifiedDate
  @Field("updatedAt")
  @JsonProperty("updatedAt")
  private Date updatedAt;

  @Field("active")
  @JsonProperty("active")
  private Boolean active;
}
