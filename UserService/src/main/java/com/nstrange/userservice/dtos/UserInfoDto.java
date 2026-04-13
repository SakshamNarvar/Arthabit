package com.nstrange.userservice.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.nstrange.userservice.entities.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoDto
{

    @JsonProperty("user_id")
    @NonNull
    private String userId;

    @JsonProperty("username")
    @NonNull
    private String username;

    @JsonProperty("first_name")
    @NonNull
    private String firstName;

    @JsonProperty("last_name")
    @NonNull
    private String lastName;

    @JsonProperty("phone_number")
    @NonNull
    private Long phoneNumber;

    @JsonProperty("email")
    @NonNull
    private String email;

    @NonNull
    @JsonProperty("account_creation_date")
    private Timestamp accountCreationDate;

    @JsonProperty("profile_pic")
    private String profilePic;

    public UserInfo transformToUserInfo() {
        return UserInfo.builder()
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .userId(userId)
                .email(email)
                .accountCreationDate(accountCreationDate)
                .profilePic(profilePic)
                .phoneNumber(phoneNumber).build();
    }
}