package com.pro.mapper;

import com.pro.dto.sallahli.JobDTO;
import com.pro.model.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CustomerRequestMapper.class, LeadAcceptanceMapper.class, ProMapper.class, ClientMapper.class})
public interface JobMapper extends Mapper<Job, JobDTO> {

    @Override
    @Mapping(target = "request", source = "request")
    @Mapping(target = "acceptance", source = "acceptance")
    @Mapping(target = "pro", source = "pro")
    @Mapping(target = "client", source = "client")
    JobDTO toDto(Job model);

    @Override
    @Mapping(target = "request", source = "request")
    @Mapping(target = "acceptance", source = "acceptance")
    @Mapping(target = "pro", source = "pro")
    @Mapping(target = "client", source = "client")
    Job toModel(JobDTO dto);

    @Override
    default Class<Job> getModelClass() {
        return Job.class;
    }
}
