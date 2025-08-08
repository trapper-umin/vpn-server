package server.vpn.com.vpnmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionPlanRequest {

    @NotBlank(message = "Название плана обязательно")
    @Size(max = 255, message = "Название плана не должно превышать 255 символов")
    private String name;

    @NotNull(message = "Тип плана обязателен")
    private String type;

    @NotNull(message = "Месячная цена обязательна")
    @DecimalMin(value = "0.01", message = "Месячная цена должна быть больше 0")
    @Digits(integer = 8, fraction = 2, message = "Неверный формат цены")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Годовая цена обязательна")
    @DecimalMin(value = "0.01", message = "Годовая цена должна быть больше 0")
    @Digits(integer = 8, fraction = 2, message = "Неверный формат цены")
    private BigDecimal yearlyPrice;

    @NotNull(message = "Максимальное количество подключений обязательно")
    @Min(value = 1, message = "Минимальное количество подключений: 1")
    @Max(value = 100, message = "Максимальное количество подключений: 100")
    private Integer maxConnections;

    @Size(max = 50, message = "Ограничение по пропускной способности не должно превышать 50 символов")
    private String bandwidthLimit;

    @Size(max = 50, message = "Ограничение по скорости не должно превышать 50 символов")
    private String speedLimit;

    @Builder.Default
    private Boolean isPopular = false;

    @Builder.Default
    private Integer sortOrder = 0;

    private List<String> features;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;
}