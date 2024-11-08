package com.learn.hub.security.service;


import com.learn.hub.email.service.EmailService;
import com.learn.hub.email.vo.AccountActivationRequest;
import com.learn.hub.entity.TokenEntity;
import com.learn.hub.entity.UserEntity;
import com.learn.hub.enums.UserRoleEnum;
import com.learn.hub.exception.LearnHubException;
import com.learn.hub.exception.UserNotFoundException;
import com.learn.hub.handler.ErrorCode;
import com.learn.hub.mapper.UserMapper;
import com.learn.hub.repo.TokenRepository;
import com.learn.hub.repo.UserRepository;
import com.learn.hub.security.util.JwtTokenUtil;
import com.learn.hub.security.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final TokenRepository tokenRepo;
    private final UserMapper userMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationProvider authenticationProvider;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public LoginResponse login(LoginRequest requestVO) {
        Authentication authentication = authenticateUser(requestVO);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        return jwtTokenUtil.prepareLoginResponse(userDetails);
    }

    private Authentication authenticateUser(LoginRequest requestVO) {
        try {
            return authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(requestVO.getEmail(), requestVO.getPassword()));
        } catch (InternalAuthenticationServiceException | UserNotFoundException ex) {
            log.error(ex);
            throw new LearnHubException(ErrorCode.USER_NOT_AUTHENTICATED, HttpStatus.UNAUTHORIZED);
        } catch (Exception ex) {
            log.error(ex);
            throw ex;
        }
    }

    @Override
    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {
        Optional<UserEntity> userEntity = userRepo.findByEmail(request.getEmail());
        if (userEntity.isPresent()) {
            throw new LearnHubException(ErrorCode.EMAIL_EXIST, HttpStatus.BAD_REQUEST, request.getEmail());
        }
        UserEntity user = UserEntity.builder()
                .role(UserRoleEnum.STUDENT)
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .accountLocked(false)
                .enabled(false)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        UserEntity savedUser = userRepo.save(user);
        sendActivationEmail(savedUser);
        return userMapper.toRegisterResponse(savedUser);
    }

    @Override
    public void activateAccount(String code, String email) {
        TokenEntity token = tokenRepo.findByCodeAndUserEmail(code, email).orElseThrow(() -> new LearnHubException(ErrorCode.INVALID_ACTIVATION_TOKEN, HttpStatus.BAD_REQUEST));
        var user = token.getUser();
        if (LocalDateTime.now().isAfter(token.getExpiresAt()) && !user.isEnabled()) {
            sendActivationEmail(user);
            throw new LearnHubException(ErrorCode.ACTIVATION_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
        }
        if (token.getExpiresAt() == null) {
            user.setEnabled(true);
            userRepo.save(user);
            token.setValidatedAt(LocalDateTime.now());
            tokenRepo.save(token);
        } else {
            throw new LearnHubException(ErrorCode.ACTIVATION_TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
        }
    }

    private void sendActivationEmail(UserEntity user) {
        String code = generateAndSaveActivationToken(user);
        AccountActivationRequest request = AccountActivationRequest.builder()
                .code(code)
                .name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .build();
        emailService.sendAccountActivationEmail(request);
    }

    private String generateAndSaveActivationToken(UserEntity user) {
        String activationCode = createActivationCode();
        TokenEntity token = TokenEntity.builder()
                .code(activationCode)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        tokenRepo.save(token);
        return activationCode;
    }

    private String createActivationCode() {
        String chars = "0123456789";
        StringBuilder codeBuilder = new StringBuilder(6);
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < 6; i++) {
            int randomIndex = secureRandom.nextInt(chars.length());
            codeBuilder.append(chars.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

}

