package com.smartparking.user.convert;

import com.smartparking.user.entity.User;
import com.smartparking.user.vo.UserVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserConvert {

    UserVO toVO(User user);

    List<UserVO> toVOList(List<User> users);
}
