package com.sallahli.mapper;

import com.sallahli.dto.OnlineTransactionDTO;
import com.sallahli.dto.PaymentDTO;
import com.sallahli.model.OnlineTransaction;
import com.sallahli.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper extends Mapper<Payment, PaymentDTO> {

    @Override
    @Mapping(target = "clientId", source = "client.id")
    @Mapping(target = "proId", source = "pro.id")
    PaymentDTO toDto(Payment payment);

    OnlineTransactionDTO toDto(OnlineTransaction onlineTransaction);
}
