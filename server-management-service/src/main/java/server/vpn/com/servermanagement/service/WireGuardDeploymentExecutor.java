package server.vpn.com.servermanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import server.vpn.com.servermanagement.dto.DeploymentEvent;
import server.vpn.com.servermanagement.dto.DeploymentEvent.EventLevel;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.request.WireGuardDeploymentRequest;
import server.vpn.com.servermanagement.dto.response.WireGuardDeploymentResponse;
import server.vpn.com.servermanagement.service.impl.SshConnectionServiceImpl;
import server.vpn.com.servermanagement.util.enums.WireGuardDeploymentStage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WireGuardDeploymentExecutor {

    private final SshConnectionServiceImpl sshConnectionService;
    
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    private static final Pattern CIDR_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/([0-9]|[1-2][0-9]|3[0-2])$"
    );

    public void deployWireGuard(WireGuardDeploymentRequest request, SseEmitter emitter, String jobId) {

        AtomicLong sequenceNumber = new AtomicLong(0);
        SSHClient sshClient = null;
        
        try {
            // QUEUED stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(), 
                     WireGuardDeploymentStage.QUEUED, EventLevel.INFO, 
                     "Задача добавлена в очередь", null);
            
            validateRequest(request, emitter, jobId, sequenceNumber);
            
            // PRECHECK_LOCAL stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.PRECHECK_LOCAL, EventLevel.INFO,
                     "Подготовка артефактов", null);
            
            Map<String, String> artifacts = prepareArtifacts(request);
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.PRECHECK_LOCAL, EventLevel.INFO,
                     "Артефакты подготовлены", Map.of("artifacts", artifacts.size()));
            
            // CONNECT_SSH stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.CONNECT_SSH, EventLevel.INFO,
                     "Подключение к серверу " + request.getSsh().getHost(), null);
            
            sshClient = connectSsh(request, emitter, jobId, sequenceNumber);
            
            // DISCOVER_OS stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.DISCOVER_OS, EventLevel.INFO,
                     "Определение операционной системы", null);
            
            Map<String, String> osInfo = discoverOS(sshClient, emitter, jobId, sequenceNumber);
            
            // PRECHECK_NETWORK stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.PRECHECK_NETWORK, EventLevel.INFO,
                     "Проверка сетевых настроек", null);
            
            checkNetwork(sshClient, request, emitter, jobId, sequenceNumber);
            
            // INSTALL_WG stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.INSTALL_WG, EventLevel.INFO,
                     "Установка WireGuard", null);
            
            installWireGuard(sshClient, osInfo, emitter, jobId, sequenceNumber);
            
            // KERNEL_TUNING stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.KERNEL_TUNING, EventLevel.INFO,
                     "Настройка параметров ядра", null);
            
            tuneKernel(sshClient, emitter, jobId, sequenceNumber);
            
            // FIREWALL_SETUP stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.FIREWALL_SETUP, EventLevel.INFO,
                     "Настройка файрвола", null);
            
            setupFirewall(sshClient, request, osInfo, emitter, jobId, sequenceNumber);
            
            // KEYS_AND_CONFIG stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.KEYS_AND_CONFIG, EventLevel.INFO,
                     "Генерация ключей и конфигурации", null);
            
            Map<String, String> keys = generateKeysAndConfig(sshClient, request, emitter, jobId, sequenceNumber);
            
            // SERVICE_ENABLE stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.SERVICE_ENABLE, EventLevel.INFO,
                     "Включение сервиса WireGuard", null);
            
            enableService(sshClient, emitter, jobId, sequenceNumber);
            
            // SERVICE_START stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.SERVICE_START, EventLevel.INFO,
                     "Запуск сервиса WireGuard", null);
            
            startService(sshClient, emitter, jobId, sequenceNumber);
            
            // HEALTHCHECK stage
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.HEALTHCHECK, EventLevel.INFO,
                     "Проверка работоспособности", null);
            
            performHealthCheck(sshClient, request, emitter, jobId, sequenceNumber);
            
            // DONE stage
            Map<String, Object> finalDetails = new HashMap<>();
            finalDetails.put("serverPublicKey", keys.get("publicKey"));
            finalDetails.put("endpoint", request.getWg().getEndpoint());
            finalDetails.put("cidr", request.getWg().getNetwork().getCidr());
            finalDetails.put("iface", "wg0");
            
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.DONE, EventLevel.INFO,
                     "WireGuard успешно развернут", finalDetails);
            
            emitter.complete();
            
        } catch (Exception e) {
            log.error("Ошибка развертывания WireGuard", e);
            sendEvent(emitter, jobId, sequenceNumber.incrementAndGet(),
                     WireGuardDeploymentStage.FAILED, EventLevel.ERROR,
                     "Ошибка развертывания: " + e.getMessage(), 
                     Map.of("error", e.getClass().getSimpleName()));
            emitter.completeWithError(e);
        } finally {
            if (sshClient != null && sshClient.isConnected()) {
                try {
                    sshClient.disconnect();
                } catch (Exception e) {
                    log.error("Ошибка при закрытии SSH соединения", e);
                }
            }
        }
    }
    
    private void validateRequest(WireGuardDeploymentRequest request, SseEmitter emitter, 
                                String jobId, AtomicLong seq) throws IOException {
        // Validate SSH settings
        if (!IPV4_PATTERN.matcher(request.getSsh().getHost()).matches()) {
            throw new IllegalArgumentException("Некорректный IP адрес: " + request.getSsh().getHost());
        }
        
        if (request.getSsh().getPort() < 1 || request.getSsh().getPort() > 65535) {
            throw new IllegalArgumentException("Некорректный порт: " + request.getSsh().getPort());
        }

//        if (!"password".equals(request.getSsh().getAuth()) && !"key".equals(request.getSsh().getAuth())) {
//            throw new IllegalArgumentException("Неподдерживаемый тип авторизации: " + request.getSsh().getAuth());
//        }


        // Validate WireGuard settings
        if (request.getWg().getListenPort() < 1 || request.getWg().getListenPort() > 65535) {
            throw new IllegalArgumentException("Некорректный порт WireGuard: " + request.getWg().getListenPort());
        }

        if (!CIDR_PATTERN.matcher(request.getWg().getNetwork().getCidr()).matches()) {
            throw new IllegalArgumentException("Некорректный CIDR: " + request.getWg().getNetwork().getCidr());
        }

        // Validate server address is within CIDR
        String serverAddress = request.getWg().getServerAddress();
        if (!serverAddress.contains("/")) {
            throw new IllegalArgumentException("Адрес сервера должен содержать маску: " + serverAddress);
        }

        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.QUEUED, EventLevel.INFO,
                 "Валидация пройдена успешно", Map.of("event", "VALIDATION_OK"));
    }
    
    private Map<String, String> prepareArtifacts(WireGuardDeploymentRequest request) {
        Map<String, String> artifacts = new HashMap<>();
        
        // Prepare WireGuard installation script
        artifacts.put("install_wg.sh", prepareInstallScript());
        
        // Prepare firewall setup script
        artifacts.put("setup_firewall.sh", prepareFirewallScript());
        
        // Prepare WireGuard config template
        artifacts.put("wg0.conf.template", prepareConfigTemplate());
        
        return artifacts;
    }
    
    private SSHClient connectSsh(WireGuardDeploymentRequest request, SseEmitter emitter,
                              String jobId, AtomicLong seq) throws Exception {
        int retries = 5;
        int backoff = 1000; // 1 second
        
        for (int i = 0; i < retries; i++) {
            try {
                SSHClient sshClient = new SSHClient();
                
                // Configure host key verification
                configureHostKeyVerification(sshClient, request.getSsh().getHostKey());
                
                // Set timeout
                sshClient.setConnectTimeout(10000);
                
                // Connect
                sshClient.connect(request.getSsh().getHost(), request.getSsh().getPort());
                
                // Authenticate
                if ("password".equals(request.getSsh().getAuth())) {
                    sshClient.authPassword(request.getSsh().getUser(), request.getSsh().getPassword());
                } else if ("key".equals(request.getSsh().getAuth())) {
                    // TODO: Implement key authentication
                    throw new UnsupportedOperationException("Аутентификация по ключу еще не реализована");
                }
                
                // Test connection
                String result = executeCommand(sshClient, "echo ok");
                if ("ok".equals(result.trim())) {
                    sendEvent(emitter, jobId, seq.incrementAndGet(),
                             WireGuardDeploymentStage.CONNECT_SSH, EventLevel.INFO,
                             "SSH подключение установлено", Map.of("event", "SSH_CONNECTED"));
                    return sshClient;
                }
            } catch (Exception e) {
                if (i < retries - 1) {
                    sendEvent(emitter, jobId, seq.incrementAndGet(),
                             WireGuardDeploymentStage.CONNECT_SSH, EventLevel.WARNING,
                             "Попытка подключения " + (i + 1) + " из " + retries + " не удалась",
                             Map.of("retry", i + 1, "backoff", backoff));
                    Thread.sleep(backoff);
                    backoff *= 2; // Exponential backoff
                } else {
                    throw e;
                }
            }
        }
        
        throw new RuntimeException("Не удалось установить SSH соединение после " + retries + " попыток");
    }
    
    private void configureHostKeyVerification(SSHClient sshClient, ServerConnectionRequest.HostKeyConfig hostKeyConfig) {
        if (hostKeyConfig == null || hostKeyConfig.getVerify() == null || 
            "insecure".equals(hostKeyConfig.getVerify()) || 
            "accept-new".equals(hostKeyConfig.getVerify())) {
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        }
    }
    
    private String executeCommand(SSHClient sshClient, String command) throws IOException {
        try (Session session = sshClient.startSession();
             Session.Command cmd = session.exec(command);
             BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()))) {
             
            String result = reader.lines().collect(Collectors.joining("\n"));
            
            // Wait for command to complete
            cmd.join(10, TimeUnit.SECONDS);
            
            Integer exitStatus = cmd.getExitStatus();
            if (exitStatus != null && exitStatus != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(cmd.getErrorStream()));
                String error = errorReader.lines().collect(Collectors.joining("\n"));
                throw new RuntimeException("Команда завершилась с ошибкой (код " + exitStatus + "): " + error);
            }
            
            return result;
        }
    }
    
    // Метод для команд, где не важен код возврата (например, grep)
    private String executeCommandIgnoreError(SSHClient sshClient, String command) {
        try {
            return executeCommand(sshClient, command);
        } catch (Exception e) {
            return "";
        }
    }
    
    private Map<String, String> discoverOS(SSHClient sshClient, SseEmitter emitter,
                                          String jobId, AtomicLong seq) throws Exception {
        Map<String, String> osInfo = new HashMap<>();
        
        // Get OS info
        String osRelease = executeCommand(sshClient, "cat /etc/os-release");
        String kernel = executeCommand(sshClient, "uname -r");
        
        // Parse OS info
        String osId = "";
        String osVersion = "";
        for (String line : osRelease.split("\n")) {
            if (line.startsWith("ID=")) {
                osId = line.substring(3).replace("\"", "");
            } else if (line.startsWith("VERSION_ID=")) {
                osVersion = line.substring(11).replace("\"", "");
            }
        }
        
        osInfo.put("id", osId);
        osInfo.put("version", osVersion);
        osInfo.put("kernel", kernel.trim());
        
        // Detect package manager
        String pkgManager = detectPackageManager(sshClient);
        osInfo.put("pkgManager", pkgManager);
        
        // Check for systemd
        String systemdCheck = executeCommandIgnoreError(sshClient, "which systemctl");
        osInfo.put("hasSystemd", !systemdCheck.isEmpty() ? "true" : "false");
        
        // Check firewall type
        String firewallType = detectFirewallType(sshClient);
        osInfo.put("firewall", firewallType);
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.DISCOVER_OS, EventLevel.INFO,
                 "ОС определена: " + osId + " " + osVersion,
                 Map.of("event", "OS_DETECTED", "os", osInfo));
        
        // Check if OS is supported
        if (!isOSSupported(osId, osVersion)) {
            throw new RuntimeException("Неподдерживаемая ОС: " + osId + " " + osVersion);
        }
        
        return osInfo;
    }
    
    private void checkNetwork(SSHClient sshClient, WireGuardDeploymentRequest request,
                             SseEmitter emitter, String jobId, AtomicLong seq) throws Exception {
        // Check if port is in use
        String portCheck = executeCommandIgnoreError(sshClient, 
            "ss -lnu | grep :" + request.getWg().getListenPort());
        
        if (!portCheck.isEmpty()) {
            sendEvent(emitter, jobId, seq.incrementAndGet(),
                     WireGuardDeploymentStage.PRECHECK_NETWORK, EventLevel.ERROR,
                     "Порт " + request.getWg().getListenPort() + " уже используется",
                     Map.of("event", "PORT_IN_USE"));
            throw new RuntimeException("Порт " + request.getWg().getListenPort() + " уже используется");
        }
        
        // Check IP forwarding
        String ipForward = executeCommand(sshClient,
            "sysctl net.ipv4.ip_forward | awk '{print $3}'");
        
        if (!"1".equals(ipForward.trim())) {
            sendEvent(emitter, jobId, seq.incrementAndGet(),
                     WireGuardDeploymentStage.PRECHECK_NETWORK, EventLevel.WARNING,
                     "IP forwarding отключен, будет включен",
                     Map.of("event", "IP_FORWARD_DISABLED"));
        }
        
        // Check for existing WireGuard interfaces
        String wgCheck = executeCommandIgnoreError(sshClient, "ip link show wg0");
        if (!wgCheck.isEmpty()) {
            throw new RuntimeException("Интерфейс wg0 уже существует");
        }
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.PRECHECK_NETWORK, EventLevel.INFO,
                 "Сетевые проверки пройдены",
                 Map.of("event", "NET_OK"));
    }
    
    private void installWireGuard(SSHClient sshClient, Map<String, String> osInfo,
                                 SseEmitter emitter, String jobId, AtomicLong seq) throws Exception {
        String pkgManager = osInfo.get("pkgManager");
        String osId = osInfo.get("id");
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.INSTALL_WG, EventLevel.INFO,
                 "Обновление списка пакетов", null);
        
        // Update package list
        if ("apt".equals(pkgManager)) {
            executeCommand(sshClient, "apt-get update -y");
        } else if ("yum".equals(pkgManager)) {
            executeCommand(sshClient, "yum makecache -y");
        }
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.INSTALL_WG, EventLevel.INFO,
                 "Установка пакетов WireGuard", null);
        
        // Install WireGuard
        String installCmd = "";
        if ("apt".equals(pkgManager)) {
            installCmd = "apt-get install -y wireguard wireguard-tools";
        } else if ("yum".equals(pkgManager)) {
            // For CentOS/RHEL, might need EPEL
            executeCommand(sshClient, 
                "yum install -y epel-release elrepo-release");
            installCmd = "yum install -y wireguard-tools kmod-wireguard";
        }
        
        for (int i = 0; i < 3; i++) {
            try {
                executeCommand(sshClient, installCmd);
                break;
            } catch (Exception e) {
                if (i < 2) {
                    sendEvent(emitter, jobId, seq.incrementAndGet(),
                             WireGuardDeploymentStage.INSTALL_WG, EventLevel.WARNING,
                             "Попытка установки " + (i + 1) + " из 3",
                             Map.of("retry", i + 1));
                    Thread.sleep(2000);
                } else {
                    throw e;
                }
            }
        }
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.INSTALL_WG, EventLevel.INFO,
                 "WireGuard успешно установлен",
                 Map.of("event", "PKGS_INSTALLED"));
    }
    
    private void tuneKernel(SSHClient sshClient, SseEmitter emitter,
                           String jobId, AtomicLong seq) throws Exception {
        // Enable IP forwarding
        executeCommand(sshClient, "sysctl -w net.ipv4.ip_forward=1");
        
        // Make it permanent
        executeCommand(sshClient, 
            "echo 'net.ipv4.ip_forward=1' > /etc/sysctl.d/99-wg.conf");
        
        // Optional tuning
        executeCommand(sshClient, 
            "sysctl -w net.ipv4.conf.all.src_valid_mark=1");
        executeCommand(sshClient,
            "echo 'net.ipv4.conf.all.src_valid_mark=1' >> /etc/sysctl.d/99-wg.conf");
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.KERNEL_TUNING, EventLevel.INFO,
                 "Параметры ядра настроены",
                 Map.of("event", "SYSCTL_APPLIED"));
    }
    
    private void setupFirewall(SSHClient sshClient, WireGuardDeploymentRequest request,
                              Map<String, String> osInfo, SseEmitter emitter,
                              String jobId, AtomicLong seq) throws Exception {
        String firewallType = osInfo.get("firewall");
        String uplinkInterface = detectUplinkInterface(sshClient);
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.FIREWALL_SETUP, EventLevel.INFO,
                 "Настройка " + firewallType + ", uplink: " + uplinkInterface, null);
        
        if ("nftables".equals(firewallType)) {
            setupNftables(sshClient, request, uplinkInterface);
        } else if ("iptables".equals(firewallType)) {
            setupIptables(sshClient, request, uplinkInterface);
        } else if ("ufw".equals(firewallType)) {
            setupUfw(sshClient, request, uplinkInterface);
        } else if ("firewalld".equals(firewallType)) {
            setupFirewalld(sshClient, request, uplinkInterface);
        }
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.FIREWALL_SETUP, EventLevel.INFO,
                 "Файрвол настроен",
                 Map.of("event", "FW_CONFIGURED"));
    }
    
    private Map<String, String> generateKeysAndConfig(SSHClient sshClient, WireGuardDeploymentRequest request,
                                                     SseEmitter emitter, String jobId, AtomicLong seq) throws Exception {
        Map<String, String> keys = new HashMap<>();
        
        // Generate private key
        String privateKey = executeCommand(sshClient, "wg genkey").trim();
        keys.put("privateKey", privateKey);
        
        // Generate public key
        String publicKey = executeCommand(sshClient, 
            "echo '" + privateKey + "' | wg pubkey").trim();
        keys.put("publicKey", publicKey);
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.KEYS_AND_CONFIG, EventLevel.INFO,
                 "Ключи сгенерированы", Map.of("publicKey", publicKey));
        
        // Create config
        String config = generateWireGuardConfig(privateKey, request);
        
        // Save config
        executeCommand(sshClient,
            "echo '" + config + "' > /etc/wireguard/wg0.conf");
        executeCommand(sshClient,
            "chmod 600 /etc/wireguard/wg0.conf");
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.KEYS_AND_CONFIG, EventLevel.INFO,
                 "Конфигурация сохранена",
                 Map.of("event", "CFG_SAVED", "path", "/etc/wireguard/wg0.conf"));
        
        return keys;
    }
    
    private void enableService(SSHClient sshClient, SseEmitter emitter,
                              String jobId, AtomicLong seq) throws Exception {
        executeCommand(sshClient, "systemctl enable wg-quick@wg0");
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.SERVICE_ENABLE, EventLevel.INFO,
                 "Сервис включен в автозагрузку",
                 Map.of("event", "SERVICE_ENABLED"));
    }
    
    private void startService(SSHClient sshClient, SseEmitter emitter,
                             String jobId, AtomicLong seq) throws Exception {
        try {
            executeCommand(sshClient, "systemctl start wg-quick@wg0");
            
            // Wait a bit for service to start
            Thread.sleep(2000);
            
            // Check service status
            String status = executeCommand(sshClient,
                "systemctl is-active wg-quick@wg0");
            
            if (!"active".equals(status.trim())) {
                String logs = executeCommand(sshClient,
                    "journalctl -u wg-quick@wg0 --since '-2 minutes' --no-pager");
                throw new RuntimeException("Сервис не запустился: " + logs);
            }
            
            sendEvent(emitter, jobId, seq.incrementAndGet(),
                     WireGuardDeploymentStage.SERVICE_START, EventLevel.INFO,
                     "Сервис запущен",
                     Map.of("event", "SERVICE_STARTED"));
        } catch (Exception e) {
            sendEvent(emitter, jobId, seq.incrementAndGet(),
                     WireGuardDeploymentStage.SERVICE_START, EventLevel.ERROR,
                     "Ошибка запуска сервиса",
                     Map.of("event", "SERVICE_START_FAILED", "error", e.getMessage()));
            throw e;
        }
    }
    
    private void performHealthCheck(SSHClient sshClient, WireGuardDeploymentRequest request,
                                   SseEmitter emitter, String jobId, AtomicLong seq) throws Exception {
        // Check interface
        String ifaceCheck = executeCommand(sshClient, "wg show wg0");
        if (ifaceCheck.isEmpty()) {
            throw new RuntimeException("Интерфейс wg0 не найден");
        }
        
        // Check port listening
        String portCheck = executeCommandIgnoreError(sshClient,
            "ss -ulpn | grep :" + request.getWg().getListenPort());
        if (portCheck.isEmpty()) {
            throw new RuntimeException("Порт " + request.getWg().getListenPort() + " не слушается");
        }
        
        // Check IP forwarding
        String ipForward = executeCommand(sshClient,
            "sysctl net.ipv4.ip_forward | awk '{print $3}'");
        if (!"1".equals(ipForward.trim())) {
            throw new RuntimeException("IP forwarding не активен");
        }
        
        sendEvent(emitter, jobId, seq.incrementAndGet(),
                 WireGuardDeploymentStage.HEALTHCHECK, EventLevel.INFO,
                 "Проверка работоспособности пройдена",
                 Map.of("event", "HEALTHY"));
    }
    
    private void sendEvent(SseEmitter emitter, String jobId, Long seq,
                          WireGuardDeploymentStage stage, EventLevel level,
                          String message, Map<String, Object> details) {
        try {
            DeploymentEvent event = DeploymentEvent.builder()
                .ts(LocalDateTime.now())
                .jobId(jobId)
                .seq(seq)
                .stage(stage)
                .level(level)
                .message(message)
                .progress(stage.getProgress())
                .details(details)
                .build();
            
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                .id(String.valueOf(seq))
                .name(stage.getCode())
                .data(event);
            
            emitter.send(eventBuilder);
        } catch (IOException e) {
            log.error("Ошибка отправки SSE события", e);
            emitter.completeWithError(e);
        }
    }
    
    // Helper methods
    
    private String detectPackageManager(SSHClient sshClient) throws Exception {
        String aptCheck = executeCommandIgnoreError(sshClient, "which apt-get");
        if (!aptCheck.isEmpty()) return "apt";
        
        String yumCheck = executeCommandIgnoreError(sshClient, "which yum");
        if (!yumCheck.isEmpty()) return "yum";
        
        String dnfCheck = executeCommandIgnoreError(sshClient, "which dnf");
        if (!dnfCheck.isEmpty()) return "dnf";
        
        throw new RuntimeException("Не удалось определить менеджер пакетов");
    }
    
    private String detectFirewallType(SSHClient sshClient) throws Exception {
        String nftCheck = executeCommandIgnoreError(sshClient, "which nft");
        if (!nftCheck.isEmpty()) return "nftables";
        
        String iptablesCheck = executeCommandIgnoreError(sshClient, "which iptables");
        if (!iptablesCheck.isEmpty()) return "iptables";
        
        String ufwCheck = executeCommandIgnoreError(sshClient, "which ufw");
        if (!ufwCheck.isEmpty()) return "ufw";
        
        String firewalldCheck = executeCommandIgnoreError(sshClient, "which firewall-cmd");
        if (!firewalldCheck.isEmpty()) return "firewalld";
        
        return "none";
    }
    
    private String detectUplinkInterface(SSHClient sshClient) throws Exception {
        String result = executeCommand(sshClient,
            "ip route | grep default | awk '{print $5}' | head -1");
        return result.trim().isEmpty() ? "eth0" : result.trim();
    }
    
    private boolean isOSSupported(String osId, String version) {
        Set<String> supportedOS = Set.of("ubuntu", "debian", "centos", "rhel", "almalinux", "rocky");
        return supportedOS.contains(osId.toLowerCase());
    }
    
    private String prepareInstallScript() {
        return """
            #!/bin/bash
            set -e
            
            # Detect OS and install WireGuard
            if [ -f /etc/debian_version ]; then
                apt-get update
                apt-get install -y wireguard wireguard-tools
            elif [ -f /etc/redhat-release ]; then
                yum install -y epel-release elrepo-release
                yum install -y wireguard-tools kmod-wireguard
            fi
            """;
    }
    
    private String prepareFirewallScript() {
        return """
            #!/bin/bash
            set -e
            
            # Firewall setup script
            LISTEN_PORT=$1
            UPLINK=$2
            
            if command -v nft >/dev/null; then
                # nftables setup
                nft add table inet wireguard
                nft add chain inet wireguard input { type filter hook input priority 0\\; }
                nft add rule inet wireguard input udp dport $LISTEN_PORT accept
            elif command -v iptables >/dev/null; then
                # iptables setup
                iptables -A INPUT -p udp --dport $LISTEN_PORT -j ACCEPT
                iptables -A FORWARD -i wg0 -j ACCEPT
                iptables -t nat -A POSTROUTING -o $UPLINK -j MASQUERADE
            fi
            """;
    }
    
    private String prepareConfigTemplate() {
        return """
            [Interface]
            PrivateKey = %PRIVATE_KEY%
            Address = %SERVER_ADDRESS%
            ListenPort = %LISTEN_PORT%
            SaveConfig = true
            
            PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o %UPLINK% -j MASQUERADE
            PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o %UPLINK% -j MASQUERADE
            """;
    }
    
    private String generateWireGuardConfig(String privateKey, WireGuardDeploymentRequest request) {
        return String.format("""
            [Interface]
            PrivateKey = %s
            Address = %s
            ListenPort = %d
            SaveConfig = true
            
            PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
            PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE
            """,
            privateKey,
            request.getWg().getServerAddress(),
            request.getWg().getListenPort()
        );
    }
    
    private void setupNftables(SSHClient sshClient, WireGuardDeploymentRequest request, String uplink) throws Exception {
        String nftRules = String.format("""
            #!/usr/sbin/nft -f
            
            table inet wireguard {
                chain input {
                    type filter hook input priority 0; policy accept;
                    udp dport %d accept
                }
                
                chain forward {
                    type filter hook forward priority 0; policy accept;
                    iifname "wg0" accept
                    oifname "wg0" accept
                }
                
                chain postrouting {
                    type nat hook postrouting priority 100; policy accept;
                    oifname "%s" masquerade
                }
            }
            """, request.getWg().getListenPort(), uplink);
        
        executeCommand(sshClient, "echo '" + nftRules + "' > /tmp/wg-nft.rules");
        executeCommand(sshClient, "nft -f /tmp/wg-nft.rules");
        executeCommand(sshClient, "rm /tmp/wg-nft.rules");
    }
    
    private void setupIptables(SSHClient sshClient, WireGuardDeploymentRequest request, String uplink) throws Exception {
        // Allow WireGuard port
        executeCommand(sshClient,
            "iptables -A INPUT -p udp --dport " + request.getWg().getListenPort() + " -j ACCEPT");
        
        // Allow forwarding
        executeCommand(sshClient, "iptables -A FORWARD -i wg0 -j ACCEPT");
        executeCommand(sshClient, "iptables -A FORWARD -o wg0 -j ACCEPT");
        
        // NAT
        executeCommand(sshClient,
            "iptables -t nat -A POSTROUTING -o " + uplink + " -j MASQUERADE");
        
        // Save rules
        String osId = executeCommand(sshClient, "grep ^ID= /etc/os-release | cut -d= -f2");
        if (osId.contains("debian") || osId.contains("ubuntu")) {
            executeCommand(sshClient, "iptables-save > /etc/iptables/rules.v4");
        } else {
            executeCommand(sshClient, "service iptables save");
        }
    }
    
    private void setupUfw(SSHClient sshClient, WireGuardDeploymentRequest request, String uplink) throws Exception {
        // Allow WireGuard port
        executeCommand(sshClient,
            "ufw allow " + request.getWg().getListenPort() + "/udp");
        
        // Enable forwarding
        executeCommand(sshClient,
            "sed -i 's/DEFAULT_FORWARD_POLICY=\"DROP\"/DEFAULT_FORWARD_POLICY=\"ACCEPT\"/' /etc/default/ufw");
        
        // Add NAT rules
        String natRules = String.format("""
            # NAT table rules
            *nat
            :POSTROUTING ACCEPT [0:0]
            -A POSTROUTING -s %s -o %s -j MASQUERADE
            COMMIT
            """, request.getWg().getNetwork().getCidr(), uplink);
        
        executeCommand(sshClient,
            "echo '" + natRules + "' >> /etc/ufw/before.rules");
        
        // Reload UFW
        executeCommand(sshClient, "ufw --force enable");
    }
    
    private void setupFirewalld(SSHClient sshClient, WireGuardDeploymentRequest request, String uplink) throws Exception {
        // Allow WireGuard port
        executeCommand(sshClient,
            "firewall-cmd --permanent --add-port=" + request.getWg().getListenPort() + "/udp");
        
        // Enable masquerading
        executeCommand(sshClient,
            "firewall-cmd --permanent --add-masquerade");
        
        // Add WireGuard interface to trusted zone
        executeCommand(sshClient,
            "firewall-cmd --permanent --zone=trusted --add-interface=wg0");
        
        // Reload firewall
        executeCommand(sshClient, "firewall-cmd --reload");
    }
}
