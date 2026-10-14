package com.vora.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;




public final class Http {

    private Http() {
    }

    public static byte[] get(String urlString) throws IOException {
        return get(urlString, 10000, 10000);
    }

    public static byte[] get(String urlString, int connectTimeout, int readTimeout) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setRequestProperty("User-Agent", "Vora/1.0");
        conn.setRequestProperty("Accept", "application/octet-stream, */*");
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code >= 400) {
            try (InputStream err = conn.getErrorStream()) {
                String msg = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
                throw new IOException("HTTP " + code + " for " + urlString + (msg.isEmpty() ? "" : ": " + msg));
            }
        }

        try (InputStream in = conn.getInputStream()) {
            String encoding = conn.getContentEncoding();
            if ("gzip".equalsIgnoreCase(encoding)) {
                try (GZIPInputStream gis = new GZIPInputStream(in)) {
                    return gis.readAllBytes();
                }
            }
            return in.readAllBytes();
        }
    }

    public static String getString(String url) throws IOException {
        return new String(get(url), StandardCharsets.UTF_8);
    }

    public static void downloadToFile(String urlString, Path dest) throws IOException {
        byte[] data = get(urlString);
        Files.createDirectories(dest.getParent());
        Files.write(dest, data);
    }
}