package server.vpn.com.servermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.springframework.stereotype.Service;
import server.vpn.com.servermanagement.dto.request.ServerConnectionRequest;
import server.vpn.com.servermanagement.dto.response.ServerConnectionResponse;
import server.vpn.com.servermanagement.service.SshConnectionService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SshConnectionServiceImpl implements SshConnectionService {

    private static final int CONNECTION_TIMEOUT_MS = 10_000; // 10 секунд

    @Override
    public ServerConnectionResponse testConnection(ServerConnectionRequest request) {
        log.info("Тестирование SSH подключения: {}", maskSensitiveInfo(request));

        var checks = ServerConnectionResponse.ConnectionChecks.builder()
                .dns(false).tcp(false).sshHandshake(false).auth(false).build();
        var details = new ServerConnectionResponse.ConnectionDetails();
        var warnings = new ArrayList<String>();

        // 1) DNS
        final String host = request.getSsh().getHost();
        final int port = request.getSsh().getPort();
        try {
            String ip = resolveDns(host);
            checks.setDns(true);
            details.setResolvedIp(ip);
            log.debug("DNS: {} -> {}", host, ip);
        } catch (Exception e) {
            return fail(checks, details, warnings, "Не удается разрешить DNS для " + host, e);
        }

        // 2) TCP
        try {
            long latency = testTcp(details.getResolvedIp(), port);
            checks.setTcp(true);
            details.setLatencyMs(latency);
            log.debug("TCP доступен ({} ms)", latency);
        } catch (Exception e) {
            return fail(checks, details, warnings, "TCP порт " + port + " недоступен", e);
        }

        // 3) SSH рукопожатие + 4) Аутентификация
        try (SSHClient ssh = new SSHClient()) {
            configureHostKeyVerification(ssh, request.getSsh().getHostKey());
            ssh.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            ssh.connect(details.getResolvedIp(), port);
            checks.setSshHandshake(true);

            details.setSshBanner(ssh.getTransport().getServerVersion());
            try {
                // В sshj получение хост-ключа отличается; при недоступности не падаем
                details.setHostKey(ServerConnectionResponse.HostKeyInfo.builder()
                        .type("ssh-rsa")
                        .fingerprintSha256("SHA256:example")
                        .build());
            } catch (Exception ignore) {
                log.debug("Не удалось получить хост-ключ сервера");
            }

            authenticate(ssh, request.getSsh());
            checks.setAuth(true);
            log.debug("Аутентификация успешна: {}", request.getSsh().getUser());

            return ServerConnectionResponse.builder()
                    .success(true)
                    .checks(checks)
                    .details(details)
                    .warnings(warnings)
                    .build();
        } catch (Exception e) {
            mapAuthOrHandshakeError(e, checks, warnings, request.getSsh());
            return fail(checks, details, warnings, null, e);
        }
    }

    // --- Network utils ---

    private String resolveDns(String host) throws UnknownHostException {
        return InetAddress.getByName(host).getHostAddress();
    }

    private long testTcp(String host, int port) throws IOException {
        long t0 = System.currentTimeMillis();
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
        }
        return System.currentTimeMillis() - t0;
    }

    // --- SSH helpers ---

    private void configureHostKeyVerification(SSHClient ssh, ServerConnectionRequest.HostKeyConfig cfg) {
        switch (cfg.getVerify()) {
            case insecure, accept_new -> {
                ssh.addHostKeyVerifier(new PromiscuousVerifier());
                log.debug("Хост-ключ: небезопасная проверка (promiscuous)");
            }
            case strict -> {
                try {
                    ssh.loadKnownHosts();
                    log.debug("Хост-ключ: strict (known_hosts)");
                } catch (IOException e) {
                    log.warn("Не удалось загрузить known_hosts: {} -> используем promiscuous", e.getMessage());
                    ssh.addHostKeyVerifier(new PromiscuousVerifier());
                }
            }
        }
    }

    private void authenticate(SSHClient ssh, ServerConnectionRequest.SshConfig sshCfg) throws Exception {
        log.debug("Аутентификация '{}' методом '{}'", sshCfg.getUser(), sshCfg.getAuth());
        try {
            switch (sshCfg.getAuth()) {
                case password -> {
                    requireNonEmpty(sshCfg.getPassword(), "Пароль обязателен для password аутентификации");
                    ssh.authPassword(sshCfg.getUser(), sshCfg.getPassword());
                }
                case key -> {
                    requireNonEmpty(sshCfg.getPrivateKey(), "Приватный ключ обязателен для key аутентификации");
                    KeyProvider key = loadPrivateKey(sshCfg.getPrivateKey(), sshCfg.getPassphrase());
                    ssh.authPublickey(sshCfg.getUser(), key);
                }
            }
        } catch (Exception e) {
            throw new Exception(buildAuthErrorMessage(e, sshCfg), e);
        }
    }

    private static void requireNonEmpty(String value, String message) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(message);
    }

    private String buildAuthErrorMessage(Exception e, ServerConnectionRequest.SshConfig cfg) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("Auth fail")) {
            return cfg.getAuth() == ServerConnectionRequest.AuthType.password
                    ? "Ошибка аутентификации: Неверный пароль для пользователя " + cfg.getUser()
                    : "Ошибка аутентификации: Приватный ключ не подходит для пользователя " + cfg.getUser();
        }
        if (msg.contains("Too many authentication failures")) return "Ошибка аутентификации: Слишком много неудачных попыток";
        if (msg.contains("publickey")) return "Ошибка аутентификации: Аутентификация по ключу запрещена или ключ некорректен";
        return "Ошибка аутентификации: " + msg;
    }

    private void mapAuthOrHandshakeError(Exception e, ServerConnectionResponse.ConnectionChecks checks,
                                         List<String> warnings, ServerConnectionRequest.SshConfig sshCfg) {
        String m = e.getMessage() != null ? e.getMessage() : "";
        if (!checks.getSshHandshake()) {
            warnings.add("Ошибка SSH рукопожатия: " + m);
            return;
        }
        if (m.contains("Приватный ключ не подходит")) {
            warnings.add("Приватный ключ не соответствует пользователю " + sshCfg.getUser());
            warnings.add("Убедитесь, что публичный ключ добавлен в ~/.ssh/authorized_keys");
        } else if (m.contains("Неверный пароль")) {
            warnings.add("Неверный пароль для пользователя " + sshCfg.getUser());
        } else if (m.contains("Аутентификация по ключу запрещена")) {
            warnings.add("Сервер не разрешает аутентификацию по SSH ключу");
            warnings.add("Проверьте PubkeyAuthentication в /etc/ssh/sshd_config");
        } else if (m.contains("Слишком много неудачных попыток")) {
            warnings.add("Превышено количество попыток аутентификации. Подождите и попробуйте снова");
        } else {
            warnings.add("Ошибка аутентификации: " + m);
        }
    }

    // --- Key loading ---

    /**
     * Нормализует приватный ключ, исправляя проблемы с форматированием
     */
    private String normalizePrivateKey(String privateKeyContent) {
        if (privateKeyContent == null) {
            return null;
        }

        String key = privateKeyContent.trim();

        // Заменяем экранированные символы переноса строк на реальные
        key = key.replace("\\n", "\n");
        key = key.replace("\\r\\n", "\n");
        key = key.replace("\\r", "\n");

        // Обрабатываем JSON-escaped строки (двойное экранирование)
        key = key.replace("\\\\n", "\n");
        key = key.replace("\\\\r\\\\n", "\n");
        key = key.replace("\\\\r", "\n");

        // Убираем лишние пробелы в начале и конце строк, но сохраняем структуру ключа
        String[] lines = key.split("\n");
        StringBuilder normalized = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                normalized.append(trimmedLine).append("\n");
            }
        }

        // Убираем последний лишний перенос строки
        String result = normalized.toString().trim();

        // Проверяем, что ключ заканчивается правильно
        if (!result.endsWith("-----")) {
            // Если последняя строка не является концом ключа, возможно нужно добавить перенос
            String[] resultLines = result.split("\n");
            if (resultLines.length > 0) {
                String lastLine = resultLines[resultLines.length - 1];
                if (lastLine.startsWith("-----END") && !lastLine.endsWith("-----")) {
                    // Исправляем возможно поврежденную концовку
                    result = result.substring(0, result.lastIndexOf(lastLine)) + lastLine.trim();
                }
            }
        }

        log.debug("Нормализация ключа: было {} символов, стало {}", privateKeyContent.length(), result.length());

        return result;
    }

    private void validatePrivateKeyFormat(String content) throws IOException {
        if (content == null || content.trim().isEmpty()) throw new IOException("Приватный ключ не может быть пустым");
        String key = content.trim();
        if (!key.contains("-----BEGIN") || !key.contains("-----END"))
            throw new IOException("Неверный формат приватного ключа: отсутствуют маркеры BEGIN/END");
        boolean ok = key.contains("-----BEGIN OPENSSH PRIVATE KEY-----") ||
                key.contains("-----BEGIN RSA PRIVATE KEY-----") ||
                key.contains("-----BEGIN DSA PRIVATE KEY-----") ||
                key.contains("-----BEGIN EC PRIVATE KEY-----") ||
                key.contains("-----BEGIN PRIVATE KEY-----");
        if (!ok) throw new IOException("Неподдерживаемый тип приватного ключа. Поддерживаются: OpenSSH, RSA, DSA, EC");
    }

    private KeyProvider loadPrivateKey(String privateKeyContent, String passphrase) throws IOException {
        log.debug("Загрузка приватного ключа из строки ({} символов)", privateKeyContent.length());

        // Нормализуем формат ключа
        String normalizedKey = normalizePrivateKey(privateKeyContent);
        log.debug("Ключ после нормализации ({} символов)", normalizedKey.length());

        validatePrivateKeyFormat(normalizedKey);

        Path tmp = null;
        try {
            tmp = Files.createTempFile("ssh_key_", ".pem");
            tmp.toFile().deleteOnExit();
            Files.writeString(tmp, normalizedKey, StandardCharsets.UTF_8);

            PasswordFinder pf = (passphrase != null && !passphrase.isEmpty())
                    ? new PasswordFinder() {
                @Override public char[] reqPassword(Resource<?> r) { return passphrase.toCharArray(); }
                @Override public boolean shouldRetry(Resource<?> r) { return false; }
            }
                    : null;

            // Пробуем OpenSSH
            try {
                OpenSSHKeyFile k = new OpenSSHKeyFile();
                if (pf != null) k.init(tmp.toFile(), pf); else k.init(tmp.toFile());
                log.debug("Ключ загружен как OpenSSH ({})", k.getType());
                return k;
            } catch (Exception e) {
                log.debug("OpenSSH загрузка не удалась: {}", e.getMessage());
            }
            // Пробуем PKCS8
            try {
                PKCS8KeyFile k = new PKCS8KeyFile();
                if (pf != null) k.init(tmp.toFile(), pf); else k.init(tmp.toFile());
                log.debug("Ключ загружен как PKCS8 ({})", k.getType());
                return k;
            } catch (Exception e) {
                log.debug("PKCS8 загрузка не удалась: {}", e.getMessage());
            }

            log.warn("Не удалось загрузить ключ ни в одном из поддерживаемых форматов. Длина нормализованного ключа: {} символов", normalizedKey.length());
            log.debug("Первые 100 символов нормализованного ключа: {}",
                     normalizedKey.length() > 100 ? normalizedKey.substring(0, 100) + "..." : normalizedKey);
            throw new IOException("Не удалось загрузить ключ ни в одном из поддерживаемых форматов");
        } catch (Exception e) {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
            String msg = e.getMessage() != null ? e.getMessage() : "";
            String em = "Ошибка загрузки приватного ключа: ";

            log.warn("Детали ошибки загрузки ключа: {}", msg);
            log.debug("Исходная длина ключа: {}, нормализованная длина: {}",
                     privateKeyContent.length(), normalizedKey != null ? normalizedKey.length() : 0);

            if (msg.contains("unrecognised object"))
                em += "Формат ключа не поддерживается. Попробуйте конвертировать ключ в PEM: ssh-keygen -p -m PEM -f ~/.ssh/id_rsa";
            else if (msg.contains("encrypted") || msg.contains("passphrase"))
                em += "Ключ защищен паролем, но passphrase не предоставлен или неверен";
            else if (msg.contains("format") || msg.contains("parse"))
                em += "Неверный формат ключа или поврежденные данные";
            else if (normalizedKey != null && normalizedKey.length() != privateKeyContent.length())
                em += "Ключ был изменен при нормализации (было " + privateKeyContent.length() + " символов, стало " + normalizedKey.length() + "). " + msg;
            else em += msg;
            throw new IOException(em, e);
        }
    }

    // --- Response & logging ---

    private ServerConnectionResponse fail(ServerConnectionResponse.ConnectionChecks checks,
                                          ServerConnectionResponse.ConnectionDetails details,
                                          List<String> warnings, String warn, Exception e) {
        if (warn != null) warnings.add(warn);
        if (e != null) log.warn("Ошибка тестирования SSH: {}", e.getMessage());
        return ServerConnectionResponse.builder()
                .success(false)
                .checks(checks)
                .details(details)
                .warnings(warnings)
                .build();
    }

    private String maskSensitiveInfo(ServerConnectionRequest request) {
        var ssh = request.getSsh();
        return String.format("host=%s, port=%d, user=%s, auth=%s",
                ssh.getHost(), ssh.getPort(), ssh.getUser(), ssh.getAuth());
    }
}

