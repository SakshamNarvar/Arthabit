package com.nstrange.userservice.service;

import com.nstrange.userservice.entities.UserInfo;
import com.nstrange.userservice.entities.UserInfoDto;
import com.nstrange.userservice.entities.UserProfileUpdateDto;
import com.nstrange.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@Service
@RequiredArgsConstructor
public class UserService
{
    private final UserRepository userRepository;

    public UserInfoDto createOrUpdateUser(UserInfoDto userInfoDto){
        UnaryOperator<UserInfo> updatingUser = user -> {
            return userRepository.save(userInfoDto.transformToUserInfo());
        };

        Supplier<UserInfo> createUser = () -> {
             return userRepository.save(userInfoDto.transformToUserInfo());
        };

        UserInfo userInfo = userRepository.findByUserId(userInfoDto.getUserId())
                .map(updatingUser)
                .orElseGet(createUser);
        return new UserInfoDto(
                userInfo.getUserId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getPhoneNumber(),
                userInfo.getEmail(),
                userInfo.getProfilePic()
        );
    }

    public UserInfoDto getUser(UserInfoDto userInfoDto) throws Exception{
        Optional<UserInfo> userInfoDtoOpt = userRepository.findByUserId(userInfoDto.getUserId());
        if(userInfoDtoOpt.isEmpty()){
            throw new Exception("User not found");
        }
        UserInfo userInfo = userInfoDtoOpt.get();
        return new UserInfoDto(
                userInfo.getUserId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getPhoneNumber(),
                userInfo.getEmail(),
                userInfo.getProfilePic()
        );
    }

    public UserInfoDto getUserById(String userId) throws Exception{
        Optional<UserInfo> userInfoOpt = userRepository.findByUserId(userId);
        if(userInfoOpt.isEmpty()){
            throw new Exception("User not found");
        }
        UserInfo userInfo = userInfoOpt.get();
        return new UserInfoDto(
                userInfo.getUserId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getPhoneNumber(),
                userInfo.getEmail(),
                userInfo.getProfilePic()
        );
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
        return new UserInfoDto(
                saved.getUserId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getPhoneNumber(),
                saved.getEmail(),
                saved.getProfilePic()
        );
    }

}
