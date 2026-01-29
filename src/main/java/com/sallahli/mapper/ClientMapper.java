package com.sallahli.mapper;

import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.model.Client;
import org.mapstruct.Mapping;

@org.mapstruct.Mapper(componentModel = "spring", uses = { MediaMapper.class, AddressMapper.class })
public interface ClientMapper extends Mapper<Client, ClientDTO> {

    @Override
    @Mapping(target = "logo", source = "logo")
    ClientDTO toDto(Client model);

    @Override
    @Mapping(target = "logo", source = "logo")
    Client toModel(ClientDTO dto);

    @Override
    default Class<Client> getModelClass() {
        return Client.class;
    }
}
