package server.vpn.com.vpnmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateServerRequest {

    @NotBlank(message = "Название сервера обязательно")
    @Size(max = 255, message = "Название сервера не должно превышать 255 символов")
    private String name;

    @NotBlank(message = "Страна обязательна")
    @Size(max = 100, message = "Название страны не должно превышать 100 символов")
    private String country;

    @NotBlank(message = "Код страны обязателен")
    @Size(min = 2, max = 2, message = "Код страны должен состоять из 2 символов")
    private String countryCode;

    @NotBlank(message = "Город обязателен")
    @Size(max = 100, message = "Название города не должно превышать 100 символов")
    private String city;

    @NotNull(message = "Максимальное количество подключений обязательно")
    @Min(value = 1, message = "Минимальное количество подключений: 1")
    @Max(value = 10000, message = "Максимальное количество подключений: 10000")
    private Integer maxConnections;

    @NotBlank(message = "Пропускная способность обязательна")
    @Size(max = 50, message = "Пропускная способность не должна превышать 50 символов")
    private String bandwidth;

    @NotBlank(message = "Скорость обязательна")
    @Size(max = 50, message = "Скорость не должна превышать 50 символов")
    private String speed;

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;

    private List<String> features;
}