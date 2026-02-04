package com.sallahli.mapper;

import com.sallahli.dto.sallahli.ClientDTO;
import com.sallahli.model.Client;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@org.mapstruct.Mapper(componentModel = "spring", uses = { MediaMapper.class, AddressMapper.class })
public interface ClientMapper extends Mapper<Client, ClientDTO> {

    @Override
    Client toModel(ClientDTO ClientDTO);

    @Override
    @Named("toDto")
    ClientDTO toDto(Client Client);


    @Named("toLightDto")
    ClientDTO toLightDto(Client Client);


    @Override
    default List<ClientDTO> toDtos(List<Client> models) {
        if (models == null) {
            return Collections.emptyList();
        }

        return models.stream()
                .map(this::toLightDto)
                .collect(Collectors.toList());
    }

    @Override
    default Class<Client> getModelClass() {
        return Client.class;
    }
}
