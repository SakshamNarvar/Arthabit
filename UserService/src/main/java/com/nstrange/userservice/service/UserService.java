package com.nstrange.userservice.service;

import com.nstrange.userservice.entities.UserInfo;
import com.nstrange.userservice.dtos.UserInfoDto;
import com.nstrange.userservice.dtos.UserProfileUpdateDto;
import com.nstrange.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService
{
    private final UserRepository userRepository;

    public void createUserFromEvent(UserInfoDto userInfoDto){
        Optional<UserInfo> existing = userRepository.findByUserId(userInfoDto.getUserId());
        if (existing.isPresent()) {
            return;
        }

        userRepository.save(userInfoDto.transformToUserInfo());
    }

    public UserInfoDto getUserById(String userId) throws Exception{
        Optional<UserInfo> userInfoOpt = userRepository.findByUserId(userId);
        if(userInfoOpt.isEmpty()){
            throw new Exception("User not found");
        }
        return toDto(userInfoOpt.get());
    }

    public UserInfoDto updateUserProfile(String userId, UserProfileUpdateDto updateDto) throws Exception {
        Optional<UserInfo> userInfoOpt = userRepository.findByUserId(userId);
        if (userInfoOpt.isEmpty()) {
            throw new Exception("User not found");
        }

        UserInfo userInfo = userInfoOpt.get();

        if (updateDto.getFirstName() != null) {
            userInfo.setFirstName(updateDto.getFirstName());
        }
        if (updateDto.getLastName() != null) {
            userInfo.setLastName(updateDto.getLastName());
        }
        if (updateDto.getProfilePic() != null) {
            userInfo.setProfilePic(updateDto.getProfilePic());
        }

        UserInfo saved = userRepository.save(userInfo);
        return toDto(saved);
    }

    private UserInfoDto toDto(UserInfo userInfo) {
        return new UserInfoDto(
                userInfo.getUserId(),
                userInfo.getUsername(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getPhoneNumber(),
                userInfo.getEmail(),
                userInfo.getProfilePic()
        );
    }
}