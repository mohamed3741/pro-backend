package com.sallahli.mapper;

import com.sallahli.dto.sallahli.LeadAcceptanceDTO;
import com.sallahli.model.LeadAcceptance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {LeadOfferMapper.class, CustomerRequestMapper.class, ProMapper.class})
public interface LeadAcceptanceMapper extends Mapper<LeadAcceptance, LeadAcceptanceDTO> {

    @Override
    @Mapping(target = "leadOffer", source = "leadOffer")
    @Mapping(target = "request", source = "request")
    @Mapping(target = "pro", source = "pro")
    LeadAcceptanceDTO toDto(LeadAcceptance model);

    @Override
    @Mapping(target = "leadOffer", source = "leadOffer")
    @Mapping(target = "request", source = "request")
    @Mapping(target = "pro", source = "pro")
    LeadAcceptance toModel(LeadAcceptanceDTO dto);

    @Override
    default Class<LeadAcceptance> getModelClass() {
        return LeadAcceptance.class;
    }
}

