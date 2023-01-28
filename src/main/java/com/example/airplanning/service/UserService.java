package com.example.airplanning.service;

import com.example.airplanning.domain.dto.user.UserDto;
import com.example.airplanning.domain.dto.user.UserJoinRequest;
import com.example.airplanning.domain.entity.User;
import com.example.airplanning.exception.AppException;
import com.example.airplanning.exception.ErrorCode;
import com.example.airplanning.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    public UserDto findUser(String userName) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUNDED));
        return UserDto.of(user);
    }

    public UserDto join(UserJoinRequest request) {

        // userName, nickname 중복 체크
        if(userRepository.existsByUserName(request.getUserName()) ||
                userRepository.existsByNickname(request.getNickname())) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String encodedPassword = encoder.encode(request.getPassword());
        User user = userRepository.save(request.toEntity(encodedPassword));
        return UserDto.of(user);
    }

    public boolean checkUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    public boolean checkNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean checkPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    // 이메일로 아이디 찾기
    public String findIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUNDED));
        return user.getUserName();
    }

    // 아이디 + 이메일로 비밀번호 찾기
    public boolean findPassword(String userName, String email) {
        return userRepository.existsByUserNameAndEmail(userName, email);
    }

    // 비밀번호 변경
    public void changePassword(String userName, String newPassword) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUNDED));

        String encodedPassword = encoder.encode(newPassword);
        user.changePassword(encodedPassword);
        userRepository.save(user);
    }

    // 닉네임 등록
    public void setNickname(String userName, String newNickname) {
        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUNDED));

        user.setNickname(newNickname);
        userRepository.save(user);
    }
}
