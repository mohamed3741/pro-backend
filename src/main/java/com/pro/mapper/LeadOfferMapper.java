package com.pro.mapper;

import com.pro.dto.sallahli.LeadOfferDTO;
import com.pro.model.LeadOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CustomerRequestMapper.class, ProMapper.class})
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
