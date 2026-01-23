package com.sallahli.dto.payment;

import com.sallahli.dto.PaymentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serial;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class MasriviPaymentResponse extends PaymentDTO {

    @Serial
    private static final long serialVersionUID = -775136401416607362L;

    private String sessionId;
    private String purchaseRef;
    private String sessionUrl;
    private String merchantId;
}
