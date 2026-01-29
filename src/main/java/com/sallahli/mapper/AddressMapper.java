package com.sallahli.mapper;

import com.sallahli.dto.sallahli.AddressDTO;
import com.sallahli.model.Address;

import java.util.List;

@org.mapstruct.Mapper(componentModel = "spring")
public interface AddressMapper extends Mapper<Address, AddressDTO> {

    @Override
    AddressDTO toDto(Address model);

    @Override
    Address toModel(AddressDTO dto);

    @Override
    List<AddressDTO> toDtos(List<Address> models);

    @Override
    default Class<Address> getModelClass() {
        return Address.class;
    }
}
