package server.vpn.com.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BecomeSellerRequest {

    @NotNull(message = "Поле 'Самозанятость' обязательно")
    private Boolean isSelfEmployed;
    
    private String businessName;
    private String businessDescription;
}