package pl.tomaszsieminski.coupon.web;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String IPV4_ADDRESS_PATTERN =
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$";
    private static final String IPV6_LITERAL_PATTERN = "^[0-9a-fA-F:.]+$";

    private final TrustedProxyProperties properties;

    public ClientIpResolver(TrustedProxyProperties properties) {
        this.properties = properties;
    }

    public String resolve(String xForwardedFor, String remoteAddress) {
        if (isTrustedProxy(remoteAddress) && xForwardedFor != null && !xForwardedFor.isBlank()) {
            return firstForwardedIp(xForwardedFor).orElse(remoteAddress);
        }
        return remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        Optional<InetAddress> parsedRemoteAddress = parseIpAddress(remoteAddress);
        if (parsedRemoteAddress.isEmpty()) {
            return false;
        }

        List<String> trustedProxies = properties.getTrustedProxies();
        return trustedProxies.stream()
                .anyMatch(trustedProxy -> matchesTrustedProxy(parsedRemoteAddress.get(), trustedProxy));
    }

    private boolean matchesTrustedProxy(InetAddress remoteAddress, String trustedProxy) {
        if (trustedProxy.contains("/")) {
            return matchesCidr(remoteAddress, trustedProxy);
        }
        return parseIpAddress(trustedProxy).map(remoteAddress::equals).orElse(false);
    }

    private boolean matchesCidr(InetAddress remoteAddress, String cidr) {
        String[] parts = cidr.split("/", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            Optional<InetAddress> networkAddress = parseIpAddress(parts[0]);
            if (networkAddress.isEmpty()) {
                return false;
            }

            byte[] remoteBytes = remoteAddress.getAddress();
            byte[] networkBytes = networkAddress.get().getAddress();
            if (remoteBytes.length != networkBytes.length) {
                return false;
            }

            int prefixLength = Integer.parseInt(parts[1]);
            if (prefixLength < 0 || prefixLength > remoteBytes.length * Byte.SIZE) {
                return false;
            }

            return matchesPrefix(remoteBytes, networkBytes, prefixLength);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean matchesPrefix(byte[] remoteBytes, byte[] networkBytes, int prefixLength) {
        int fullBytes = prefixLength / Byte.SIZE;
        int remainingBits = prefixLength % Byte.SIZE;

        if (!Arrays.equals(Arrays.copyOf(remoteBytes, fullBytes), Arrays.copyOf(networkBytes, fullBytes))) {
            return false;
        }

        if (remainingBits == 0) {
            return true;
        }

        int mask = 0xFF << (Byte.SIZE - remainingBits);
        return (remoteBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
    }

    private Optional<InetAddress> parseIpAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        String trimmedAddress = address.trim();
        if (!isIpLiteral(trimmedAddress)) {
            return Optional.empty();
        }

        try {
            InetAddress inetAddress = InetAddress.getByName(trimmedAddress);
            if (inetAddress instanceof Inet4Address || inetAddress instanceof Inet6Address) {
                return Optional.of(inetAddress);
            }
            return Optional.empty();
        } catch (UnknownHostException exception) {
            return Optional.empty();
        }
    }

    private boolean isIpLiteral(String address) {
        if (address.matches(IPV4_ADDRESS_PATTERN)) {
            return true;
        }
        if (!address.contains(":") || !address.matches(IPV6_LITERAL_PATTERN)) {
            return false;
        }
        try {
            return InetAddress.getByName(address) instanceof Inet6Address;
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private Optional<String> firstForwardedIp(String xForwardedFor) {
        String firstAddress = xForwardedFor.split(",")[0].trim();
        if (parseIpAddress(firstAddress).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(firstAddress);
    }
}
