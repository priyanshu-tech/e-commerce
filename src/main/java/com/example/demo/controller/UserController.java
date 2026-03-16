package com.example.demo.controller;

import com.example.demo.service.UserService;
import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User Controller
 * Handles user profile and address management
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private final UserService userService;

    @GetMapping
    public UserVO getUser(@RequestParam String username, @RequestParam String email) {
        return userService.getUser(username, email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserVO createUser(@RequestBody UserVO userVO) {
        return userService.createUser(userVO);
    }

    @PostMapping("/update")
    public UserVO updateUser(@RequestParam String username, @RequestParam String email, @RequestBody UserVO userVO) {
        return userService.updateUser(username, email, userVO);
    }

    @GetMapping("/addresses")
    public List<AddressVO> getUserAddresses(@RequestParam String username, @RequestParam String email) {
        return userService.getUserAddresses(username, email);
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressVO addAddress(@RequestParam String username, @RequestParam String email, @RequestBody AddressVO addressVO) {
        return userService.addAddress(username, email, addressVO);
    }
}
