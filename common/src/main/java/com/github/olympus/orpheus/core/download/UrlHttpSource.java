package com.github.olympus.orpheus.core.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

/** Plain HttpURLConnection download with timeouts and a size limit. */
public final class UrlHttpSource implements HttpSource {
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;
    private static final int MAX_BYTES = 64 * 1024 * 1024;

    @Override
    public byte[] get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "Orpheus-Music-Mod");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status + " for " + url);
            }
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    if (output.size() > MAX_BYTES) {
                        throw new IOException("Download is larger than " + MAX_BYTES + " bytes: " + url);
                    }
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }
}
