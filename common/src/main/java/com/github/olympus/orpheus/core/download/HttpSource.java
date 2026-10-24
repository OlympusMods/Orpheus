package com.github.olympus.orpheus.core.download;

import java.io.IOException;

/** Fetches the bytes behind a URL. Exists so the installer can be tested without a network. */
public interface HttpSource {
    byte[] get(String url) throws IOException;
}
