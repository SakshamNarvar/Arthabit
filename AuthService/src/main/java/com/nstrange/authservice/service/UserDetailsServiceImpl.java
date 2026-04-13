package com.nstrange.authservice.service;

import com.nstrange.authservice.entities.UserInfo;
import com.nstrange.authservice.entities.UserRole;
import com.nstrange.authservice.eventProducer.UserInfoEvent;
import com.nstrange.authservice.eventProducer.UserInfoProducer;
import com.nstrange.authservice.exception.UserAlreadyExistsException;
import com.nstrange.authservice.model.UserInfoDto;
import com.nstrange.authservice.repository.RoleRepository;
import com.nstrange.authservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final UserInfoProducer userInfoProducer;
    @Autowired
    private final RoleRepository roleRepository;

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        log.debug("Entering in loadUserByUsername Method...");
        UserInfo user = userRepository.findByUsername(username);
        if(user==null) {
            log.error("Username not found: " + username);
            throw new UsernameNotFoundException("could not find user with username: " + username);
        }
        log.info("User Authenticated Successfully..!!!");
        return new CustomUserDetails(user);
    }

    public UserInfo checkIfUserAlreadyExists(String username) {
        return userRepository.findByUsername(username);
    }

    public String signupUser(UserInfoDto userInfoDto) {
        if (Objects.nonNull(checkIfUserAlreadyExists(userInfoDto.getUsername()))) {
            throw new UserAlreadyExistsException(userInfoDto.getUsername());
        }
        String userId = UUID.randomUUID().toString();
        userInfoDto.setPassword(passwordEncoder.encode(userInfoDto.getPassword()));

        UserRole defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new UserRole(null, "ROLE_USER")));

        UserInfo userInfo = new UserInfo(
                userId,
                userInfoDto.getUsername(),
                userInfoDto.getPassword(),
                userInfoDto.getPasswordHint(),
                userInfoDto.getEmail(),
                userInfoDto.getPhoneNumber(),
                Set.of(defaultRole));
        userRepository.save(userInfo);

        // Fire-and-forget: Kafka failure must NOT block signup
        try {
            userInfoProducer.sendEventToKafka(userInfoEventToPublish(userInfoDto, userId));
        } catch (Exception ex) {
            log.error("Failed to publish user event to Kafka for userId={}: {}", userId, ex.getMessage(), ex);
        }

        return userId;
    }

    public String getUserByUsername(String username) {
        return Optional.of(userRepository.findByUsername(username)).map(UserInfo::getUserId).orElse(null);
    }

    private UserInfoEvent userInfoEventToPublish(UserInfoDto userInfoDto, String userId) {
        return UserInfoEvent.builder()
                .userId(userId)
                .username(userInfoDto.getUsername())
                .firstName(userInfoDto.getFirstName())
                .lastName(userInfoDto.getLastName())
                .email(userInfoDto.getEmail())
                .phoneNumber(userInfoDto.getPhoneNumber())
                .accountCreationDate(new java.sql.Timestamp(System.currentTimeMillis()))
                .build();
    }
}
