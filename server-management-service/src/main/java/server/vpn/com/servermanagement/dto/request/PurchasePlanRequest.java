package server.vpn.com.servermanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchasePlanRequest {

    @NotNull(message = "Тип тарификации обязателен")
    private String billingCycle;
}