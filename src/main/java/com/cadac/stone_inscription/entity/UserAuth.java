package com.cadac.stone_inscription.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users_auth")
public class UserAuth {

    @Id
    @JsonProperty("_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Field("username")
    @JsonProperty("username")
    @NotBlank
    @Indexed(unique = true)
    private String username;

    @Field("email")
    @JsonProperty("email")
    @Email
    @Indexed(unique = true)
    private String email;

    @Field("passwordHash")
    @JsonIgnore // ensure it's never returned in API responses
    private String passwordHash;

    @Field("provider")
    @JsonProperty("provider")
    private String provider;

    @Field("roles")
    @JsonProperty("roles")
    private List<String> roles;

    @CreatedDate
    @Field("createdAt")
    @JsonProperty("createdAt")
    private Date createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    @JsonProperty("updatedAt")
    private Date updatedAt;

    @Field("lastLogin")
    @JsonProperty("lastLogin")
    private Date lastLogin;
}
