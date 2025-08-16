package server.vpn.com.servermanagement.util.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WireGuardDeploymentStage {
    QUEUED("QUEUED", "Задача в очереди", 0),
    PRECHECK_LOCAL("PRECHECK_LOCAL", "Локальная подготовка", 5),
    CONNECT_SSH("CONNECT_SSH", "Подключение по SSH", 10),
    DISCOVER_OS("DISCOVER_OS", "Определение ОС", 15),
    PRECHECK_NETWORK("PRECHECK_NETWORK", "Проверка сети", 20),
    INSTALL_WG("INSTALL_WG", "Установка WireGuard", 35),
    KERNEL_TUNING("KERNEL_TUNING", "Настройка ядра", 45),
    FIREWALL_SETUP("FIREWALL_SETUP", "Настройка файрвола", 55),
    KEYS_AND_CONFIG("KEYS_AND_CONFIG", "Генерация ключей и конфигурации", 70),
    SERVICE_ENABLE("SERVICE_ENABLE", "Включение сервиса", 80),
    SERVICE_START("SERVICE_START", "Запуск сервиса", 85),
    HEALTHCHECK("HEALTHCHECK", "Проверка работоспособности", 95),
    DONE("DONE", "Завершено успешно", 100),
    FAILED("FAILED", "Ошибка развертывания", -1);

    private final String code;
    private final String description;
    private final int progress;
}
