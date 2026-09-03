package com.omenx.osint;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

public class DomainScanner {

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?:[A-Za-z0-9-]{2,}\\.)*[A-Za-z]{2,}$"
    );

    private static final String[] COMMON_SUBDOMAINS = {
            "www", "mail", "ftp", "webmail", "smtp", "pop", "ns1", "ns2",
            "api", "dev", "staging", "test", "admin", "portal", "blog",
            "shop", "cdn", "static", "img", "vpn", "remote"
    };

    public Map<String, String> scan(String rawDomain) {
        Map<String, String> results = new LinkedHashMap<>();

        if (rawDomain == null || rawDomain.isBlank()) {
            results.put("error", "Empty domain");
            return results;
        }

        String domain = cleanDomain(rawDomain);

        if (!DOMAIN_PATTERN.matcher(domain).matches()) {
            results.put("error", "Invalid domain format");
            return results;
        }

        results.put("domain", domain);

        // DNS
        Map<String, String> dns = lookupDns(domain);
        results.putAll(dns);

        // HTTP / HTTPS
        results.put("https", checkStatus("https://" + domain));
        results.put("http", checkStatus("http://" + domain));

        // SSL
        Map<String, String> ssl = checkSsl(domain);
        results.putAll(ssl);

        // Subdomains
        results.put("subdomains", checkSubdomains(domain));

        // WHOIS
        Map<String, String> whois = lookupWhois(domain);
        results.putAll(whois);

        results.put("reputation", "Requires external API (VirusTotal, AbuseIPDB...)");

        return results;
    }

    private String cleanDomain(String input) {
        return input.trim().toLowerCase()
                .replace("http://", "")
                .replace("https://", "")
                .replace("www.", "")
                .split("/")[0]
                .split(":")[0];
    }

    private Map<String, String> lookupDns(String domain) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            InitialDirContext ctx = new InitialDirContext(env);

            map.put("dns_a", getAttr(ctx, domain, "A"));
            map.put("dns_mx", getAttr(ctx, domain, "MX"));
            map.put("dns_ns", getAttr(ctx, domain, "NS"));
            map.put("dns_txt", getAttr(ctx, domain, "TXT"));
            map.put("dns_cname", getAttr(ctx, domain, "CNAME"));

            ctx.close();
        } catch (Exception e) {
            map.put("dns_a", "Error: " + e.getMessage());
        }
        return map;
    }

    private String getAttr(InitialDirContext ctx, String domain, String type) {
        try {
            Attributes attrs = ctx.getAttributes(domain, new String[]{type});
            Attribute attr = attrs.get(type);
            if (attr == null) return null;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < attr.size(); i++) {
                if (i > 0) sb.append("  •  ");
                sb.append(attr.get(i).toString().trim());
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String checkStatus(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            int code = conn.getResponseCode();
            String msg = conn.getResponseMessage();
            conn.disconnect();
            return code + " " + (msg != null ? msg : "");
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> checkSsl(String domain) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL("https://" + domain).openConnection();
            conn.setConnectTimeout(4000);
            conn.connect();

            var session = conn.getSSLSession();
            if (session.isPresent() && session.get().getPeerCertificates().length > 0) {
                X509Certificate cert = (X509Certificate) session.get().getPeerCertificates()[0];

                map.put("ssl_subject", cert.getSubjectX500Principal().getName());
                map.put("ssl_issuer", cert.getIssuerX500Principal().getName());

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone(ZoneId.systemDefault());
                String from = fmt.format(cert.getNotBefore().toInstant());
                String to = fmt.format(cert.getNotAfter().toInstant());
                map.put("ssl_validity", from + " → " + to);
            }
            conn.disconnect();
        } catch (Exception ignored) {}
        return map;
    }

    private String checkSubdomains(String domain) {
        List<String> found = new ArrayList<>();
        for (String sub : COMMON_SUBDOMAINS) {
            try {
                InetAddress.getByName(sub + "." + domain);
                found.add(sub + "." + domain);
            } catch (UnknownHostException ignored) {}
        }
        return found.isEmpty() ? null : String.join("  •  ", found);
    }

    private Map<String, String> lookupWhois(String domain) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            String server = getWhoisServer(domain);
            if (server == null) return map;

            String raw = queryWhois(server, domain);
            if (raw == null || raw.isBlank()) return map;

            map.put("whois_registrar", extract(raw, "Registrar:"));
            map.put("whois_org", firstNonNull(
                    extract(raw, "Registrant Organization:"),
                    extract(raw, "OrgName:"),
                    extract(raw, "Organization:")
            ));
            map.put("whois_created", firstNonNull(
                    extract(raw, "Creation Date:"),
                    extract(raw, "Created:")
            ));
            map.put("whois_expires", firstNonNull(
                    extract(raw, "Registry Expiry Date:"),
                    extract(raw, "Expiry Date:"),
                    extract(raw, "Expires:")
            ));
            map.put("whois_updated", extract(raw, "Updated Date:"));
            map.put("whois_server", server);

        } catch (Exception ignored) {}
        return map;
    }

    private String getWhoisServer(String domain) {
        try {
            String tld = domain.substring(domain.lastIndexOf('.') + 1);
            String response = queryWhois("whois.iana.org", tld);
            if (response == null) return "whois.verisign-grs.com";

            for (String line : response.split("\n")) {
                if (line.toLowerCase().startsWith("whois:")) {
                    return line.split(":", 2)[1].trim();
                }
            }
        } catch (Exception ignored) {}
        return "whois.verisign-grs.com";
    }

    private String queryWhois(String server, String query) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server, 43), 5000);
            socket.setSoTimeout(7000);
            socket.getOutputStream().write((query + "\r\n").getBytes());
            socket.getOutputStream().flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String extract(String text, String key) {
        for (String line : text.split("\n")) {
            if (line.trim().toLowerCase().startsWith(key.toLowerCase())) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String value = parts[1].trim();
                    if (!value.isEmpty() && !value.equalsIgnoreCase("redacted for privacy")) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}