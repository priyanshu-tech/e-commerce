package com.example.demo.service;

import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;

import java.util.List;

/**
 * User Service Interface
 * Handles user profile and address business logic
 */
public interface UserService {

    UserVO getUser(String username, String email);

    UserVO createUser(UserVO userVO);

    UserVO updateUser(String username, String email, UserVO userVO);

    List<AddressVO> getUserAddresses(String username, String email);

    AddressVO addAddress(String username, String email, AddressVO addressVO);
}
