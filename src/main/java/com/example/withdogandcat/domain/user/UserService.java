package com.example.withdogandcat.domain.user;

import com.example.withdogandcat.domain.user.dto.SignupRequestDto;
import com.example.withdogandcat.domain.user.entity.User;
import com.example.withdogandcat.domain.user.UserRepository;
import com.example.withdogandcat.domain.user.entity.UserRole;
import com.example.withdogandcat.global.email.Email;
import com.example.withdogandcat.global.email.EmailRepository;
import com.example.withdogandcat.global.exception.CustomException;
import com.example.withdogandcat.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailRepository emailRepository;

    @Transactional
    public void registerNewAccount(SignupRequestDto requestDto) {
        checkIfEmailExist(requestDto.getEmail());

        Email email = emailRepository.findByEmailAndExpiryDateAfterAndEmailVerifiedTrue(
                        requestDto.getEmail(), LocalDateTime.now())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());
        User newUser = User.builder()
                .email(requestDto.getEmail())
                .password(encodedPassword)
                .phoneNumber(requestDto.getPhoneNumber())
                .nickname(requestDto.getNickname())
                .role(UserRole.USER)
                .build();

        userRepository.save(newUser);
        emailRepository.delete(email);
    }

    private void checkIfEmailExist(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }


    @Transactional
    public void deleteUnverifiedEmails(LocalDateTime now) {
        List<Email> emails = emailRepository.findAll();
        for (Email email : emails) {
            if (email.isEmailVerified() && !email.isRegistrationComplete() && email.getExpiryDate().isBefore(now)) {
                emailRepository.delete(email);
            }
        }
    }
}

