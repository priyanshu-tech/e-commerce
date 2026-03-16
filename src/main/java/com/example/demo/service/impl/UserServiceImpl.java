package com.example.demo.service.impl;

import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ExceptionUtils;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public UserVO getUser(String username, String email) {
        log.info("Getting user - username: {}, email: {}", username, email);
        User user = userRepository.findByUsernameAndEmail(username, email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username + " and email: " + email));
        UserVO result = UserMapper.toVO(user);
        LogUtils.info(log, "Fetched user", result);
        return result;
    }

    @Override
    @Transactional
    public UserVO createUser(UserVO userVO) {
        LogUtils.info(log, "Creating user", userVO);
        if (userRepository.existsByUsernameAndEmail(userVO.getUsername(), userVO.getEmail())) {
            throw new DuplicateResourceException("User already exists with username: " + userVO.getUsername() + " and email: " + userVO.getEmail());
        }
        if (userRepository.existsByEmail(userVO.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + userVO.getEmail());
        }
        User user = UserMapper.toEntity(userVO);
        User savedUser = userRepository.save(user);
        UserVO result = UserMapper.toVO(savedUser);
        LogUtils.info(log, "User created successfully", result);
        return result;
    }

    @Override
    @Transactional
    public UserVO updateUser(String username, String email, UserVO userVO) {
        log.info("Updating user - username: {}, email: {}", username, email);
        LogUtils.info(log, "Update payload", userVO);
        User existingUser = userRepository.findByUsernameAndEmail(username, email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username + " and email: " + email));
        existingUser.setFirstName(userVO.getFirstName());
        existingUser.setLastName(userVO.getLastName());
        existingUser.setPhoneNumber(userVO.getPhoneNumber());
        existingUser.setStatus(userVO.getStatus());
        User updatedUser = userRepository.save(existingUser);
        UserVO result = UserMapper.toVO(updatedUser);
        LogUtils.info(log, "User updated successfully", result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressVO> getUserAddresses(String username, String email) {
        log.info("Getting addresses - username: {}, email: {}", username, email);
        if (!userRepository.existsByUsernameAndEmail(username, email)) {
            throw new ResourceNotFoundException("User not found with username: " + username + " and email: " + email);
        }
        List<Address> addresses = addressRepository.findByUsernameAndEmail(username, email);
        List<AddressVO> result = addresses.stream()
                .map(UserMapper::toAddressVO)
                .collect(Collectors.toList());
        LogUtils.info(log, "Fetched addresses", result);
        return result;
    }

    @Override
    @Transactional
    public AddressVO addAddress(String username, String email, AddressVO addressVO) {
        log.info("Adding address - username: {}, email: {}", username, email);
        if (!userRepository.existsByUsernameAndEmail(username, email)) {
            throw new ResourceNotFoundException("User not found with username: " + username + " and email: " + email);
        }
        if (Boolean.TRUE.equals(addressVO.getIsDefault())) {
            addressRepository.findByUsernameAndEmailAndIsDefaultTrue(username, email)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        addressRepository.save(existingDefault);
                    });
        }
        Address address = UserMapper.toAddressEntity(addressVO);
        LogUtils.info(log, "Inserting address", addressVO);
        Address savedAddress = addressRepository.save(address);
        AddressVO result = UserMapper.toAddressVO(savedAddress);
        LogUtils.info(log, "Address added successfully", result);
        return result;
    }
}
