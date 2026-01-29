package com.sallahli.mapper;

import com.sallahli.dto.sallahli.LeadOfferDTO;
import com.sallahli.model.LeadOffer;
import org.mapstruct.Mapping;

@org.mapstruct.Mapper(componentModel = "spring", uses = {CustomerRequestMapper.class, ProMapper.class})
public interface LeadOfferMapper extends Mapper<LeadOffer, LeadOfferDTO> {

    @Override
    @Mapping(target = "request", source = "request")
    @Mapping(target = "pro", source = "pro")
    LeadOfferDTO toDto(LeadOffer model);

    @Override
    @Mapping(target = "request", source = "request")
    @Mapping(target = "pro", source = "pro")
    LeadOffer toModel(LeadOfferDTO dto);

    @Override
    default Class<LeadOffer> getModelClass() {
        return LeadOffer.class;
    }
}

