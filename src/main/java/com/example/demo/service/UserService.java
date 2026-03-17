package com.example.demo.service;

import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;

import java.util.List;

public interface UserService {

    UserVO getUser(String username, String email);

    UserVO createUser(UserVO userVO);

    UserVO updateUser(String username, String email, UserVO userVO);

    List<AddressVO> getUserAddresses(Long userId);

    AddressVO addAddress(Long userId, AddressVO addressVO);
}
