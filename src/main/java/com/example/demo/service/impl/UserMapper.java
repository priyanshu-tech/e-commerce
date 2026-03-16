package com.example.demo.service.impl;

import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.util.LogUtils;
import com.example.demo.vo.user.AddressVO;
import com.example.demo.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class UserMapper {

    static UserVO toVO(User user) {
        if (user == null) return null;
        UserVO result = UserVO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
        LogUtils.info(log, "Mapped User to UserVO", result);
        return result;
    }

    static User toEntity(UserVO userVO) {
        if (userVO == null) return null;
        LogUtils.info(log, "Mapping UserVO to entity", userVO);
        return User.builder()
                .username(userVO.getUsername())
                .email(userVO.getEmail())
                .firstName(userVO.getFirstName())
                .lastName(userVO.getLastName())
                .phoneNumber(userVO.getPhoneNumber())
                .status(userVO.getStatus())
                .build();
    }

    static AddressVO toAddressVO(Address address) {
        if (address == null) return null;
        AddressVO result = AddressVO.builder()
                .addressId(address.getAddressId())
                .username(address.getUsername())
                .email(address.getEmail())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .addressType(address.getAddressType())
                .isDefault(address.getIsDefault())
                .build();
        LogUtils.info(log, "Mapped Address to AddressVO", result);
        return result;
    }

    static Address toAddressEntity(AddressVO addressVO) {
        if (addressVO == null) return null;
        LogUtils.info(log, "Mapping AddressVO to entity", addressVO);
        return Address.builder()
                .addressId(addressVO.getAddressId())
                .username(addressVO.getUsername())
                .email(addressVO.getEmail())
                .addressLine1(addressVO.getAddressLine1())
                .addressLine2(addressVO.getAddressLine2())
                .city(addressVO.getCity())
                .state(addressVO.getState())
                .zipCode(addressVO.getZipCode())
                .country(addressVO.getCountry())
                .addressType(addressVO.getAddressType())
                .isDefault(addressVO.getIsDefault())
                .build();
    }
}
