package com.nstrange.userservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "User information data transfer object")
public class UserInfoDto
{

    @JsonProperty("user_id")
    @NonNull
    @Schema(description = "Unique user identifier", example = "user123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @JsonProperty("first_name")
    @NonNull
    @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @JsonProperty("last_name")
    @NonNull
    @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @JsonProperty("phone_number")
    @NonNull
    @Schema(description = "User's phone number", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long phoneNumber;

    @JsonProperty("email")
    @NonNull
    @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @JsonProperty("profile_pic")
    @Schema(description = "URL of the user's profile picture", example = "https://example.com/pic.jpg")
    private String profilePic;

    public UserInfo transformToUserInfo() {
        return UserInfo.builder()
                .firstName(firstName)
                .lastName(lastName)
                .userId(userId)
                .email(email)
                .profilePic(profilePic)
                .phoneNumber(phoneNumber).build();
    }

}
