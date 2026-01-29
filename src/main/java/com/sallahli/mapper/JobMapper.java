package com.sallahli.mapper;

import com.sallahli.dto.sallahli.JobDTO;
import com.sallahli.model.Job;
import org.mapstruct.Mapping;

@org.mapstruct.Mapper(componentModel = "spring", uses = {CustomerRequestMapper.class, LeadOfferMapper.class, ProMapper.class, ClientMapper.class})
public interface JobMapper extends Mapper<Job, JobDTO> {

    @Override
    @Mapping(target = "request", source = "request")
    @Mapping(target = "leadOffer", source = "leadOffer")
    @Mapping(target = "pro", source = "pro")
    @Mapping(target = "client", source = "client")
    JobDTO toDto(Job model);

    @Override
    @Mapping(target = "request", source = "request")
    @Mapping(target = "leadOffer", source = "leadOffer")
    @Mapping(target = "pro", source = "pro")
    @Mapping(target = "client", source = "client")
    Job toModel(JobDTO dto);

    @Override
    default Class<Job> getModelClass() {
        return Job.class;
    }
}

