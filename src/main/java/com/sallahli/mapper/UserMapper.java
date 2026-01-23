package com.sallahli.mapper;


import com.sallahli.model.User;
import com.sallahli.dto.UserDTO;
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

