package com.drawthon.team1.user.controller;

import com.drawthon.team1.user.dto.UserResponseDTO;
import com.drawthon.team1.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(value = "http://localhost:3000")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/api/v1/user")
    public ResponseEntity<UserResponseDTO> createUser(){
        return ResponseEntity.ok(

        )
    }

    @GetMapping("/api/v1/user/{userId}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long userId){

    }

    @GetMapping("/api/v1/user")
    public ResponseEntity<List<UserResponseDTO>> getUsers(){

    }

    @PutMapping("/api/v1/user/{userId}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateDTO){

    }

    @DeleteMapping("/api/v1/user/{userId")
    public void deleteUser(@PathVariable Long userId){

    }
}
