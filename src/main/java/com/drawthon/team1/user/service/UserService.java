package com.drawthon.team1.user.service;

import com.drawthon.team1.posting.repository.PostApplicationRepository;
import com.drawthon.team1.user.dto.UserCreateDTO;
import com.drawthon.team1.user.dto.UserResponseDTO;
import com.drawthon.team1.user.dto.UserUpdateDTO;
import com.drawthon.team1.user.entity.Role;
import com.drawthon.team1.user.entity.User;
import com.drawthon.team1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PostApplicationRepository postApplicationRepository;

    /**
     * 이메일 중복은 동시 요청까지 고려해 DB unique 제약 위반도 409로 변환한다.
     */
    public UserResponseDTO createUser(UserCreateDTO userCreateDTO) {
        if (userRepository.existsByEmail(userCreateDTO.getEmail())) {
            throw duplicateEmail();
        }

        User user = User.builder()
                .name(userCreateDTO.getName())
                .email(userCreateDTO.getEmail())
                .role(Role.USER)
                .build();
        try {
            return UserResponseDTO.from(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            throw duplicateEmail();
        }
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUser(Long userId) {
        return UserResponseDTO.from(findUser(userId));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::from)
                .toList();
    }

    @Transactional
    public UserResponseDTO updateUser(Long userId, UserUpdateDTO userUpdateDTO) {
        User user = findUser(userId);
        if (userRepository.existsByEmailAndIdNot(userUpdateDTO.getEmail(), userId)) {
            throw duplicateEmail();
        }

        user.update(userUpdateDTO);
        try {
            // 커밋 시점까지 미루면 제약 위반을 여기서 잡을 수 없어 바로 flush한다.
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw duplicateEmail();
        }
        return UserResponseDTO.from(user);
    }

    /**
     * 해당 사용자의 공고 신청 내역도 함께 삭제한다.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);
        postApplicationRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private static ResponseStatusException duplicateEmail() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
    }
}
