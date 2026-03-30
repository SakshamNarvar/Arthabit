package com.nstrange.userservice.controller;

import com.nstrange.userservice.dtos.UserInfoDto;
import com.nstrange.userservice.dtos.UserProfileUpdateDto;
import com.nstrange.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController
{

    private final UserService userService;

    @GetMapping("/v1/users/{userId}")
    public ResponseEntity<UserInfoDto> getUser(
            @PathVariable String userId){
        try{
            UserInfoDto user = userService.getUserById(userId);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception ex){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/v1/users/{userId}")
    public ResponseEntity<UserInfoDto> updateUserProfile(
            @PathVariable String userId,
            @RequestBody UserProfileUpdateDto updateDto){
        try{
            UserInfoDto user = userService.updateUserProfile(userId, updateDto);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception ex){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth(){
        return new ResponseEntity<>(true, HttpStatus.OK);
    }
}