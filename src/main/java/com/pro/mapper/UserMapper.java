package com.pro.mapper;


import com.pro.model.User;
import com.pro.dto.UserDTO;
import org.mapstruct.Builder;


@org.mapstruct.Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserMapper extends Mapper<User, UserDTO> {

    @Override
    User toModel(UserDTO userDTO);

    @Override
    UserDTO toDto(User user);

    @Override
    default Class<User> getModelClass() {
        return User.class;
    }
}

