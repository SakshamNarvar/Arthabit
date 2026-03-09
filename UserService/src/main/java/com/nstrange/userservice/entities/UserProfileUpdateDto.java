package com.nstrange.userservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO for updating user profile fields owned by User Service (firstName, lastName, profilePic)")
public class UserProfileUpdateDto {

    @JsonProperty("first_name")
    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @JsonProperty("last_name")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @JsonProperty("profile_pic")
    @Schema(description = "URL of the user's profile picture", example = "https://example.com/pic.jpg")
    private String profilePic;
}

