package com.nstrange.userservice.consumer;

import com.nstrange.userservice.entities.UserInfoDto;
import com.nstrange.userservice.entities.UserProfileUpdateDto;
import com.nstrange.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "APIs for managing user profiles")
public class UserController
{

    private final UserService userService;

    @Operation(summary = "Get user by ID", description = "Fetches a user profile by their unique user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserInfoDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/user/v1/getUser")
    public ResponseEntity<UserInfoDto> getUser(
            @Parameter(description = "The unique user ID", required = true, example = "user123")
            @RequestParam("userId") String userId){
        try{
            UserInfoDto user = userService.getUserById(userId);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception ex){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Update user profile",
            description = "Updates only User Service owned fields: firstName, lastName, and profilePic. " +
                    "Fields like userId, email, and phoneNumber are managed by Auth Service and cannot be changed here.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserInfoDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PutMapping("/user/v1/updateProfile")
    public ResponseEntity<UserInfoDto> updateUserProfile(
            @Parameter(description = "The unique user ID", required = true, example = "user123")
            @RequestParam("userId") String userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Profile fields to update (firstName, lastName, profilePic). Null fields are ignored.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserProfileUpdateDto.class)))
            @RequestBody UserProfileUpdateDto updateDto){
        try{
            UserInfoDto user = userService.updateUserProfile(userId, updateDto);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception ex){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Health check", description = "Returns the health status of the User Service")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth(){
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

}
