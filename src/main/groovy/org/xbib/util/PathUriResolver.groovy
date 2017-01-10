package org.xbib.util

import javax.xml.transform.TransformerException
import javax.xml.transform.URIResolver
import javax.xml.transform.stream.StreamSource
import java.nio.file.Files
import java.nio.file.Path

/**
 *
 */
class PathUriResolver implements URIResolver {

    private final Path root

    PathUriResolver(Path root) {
        this.root = root
    }

    @Override
    StreamSource resolve(String href, String base) throws TransformerException {
        URL url
        String s = href
        try {
            URI uri = URI.create(href)
            if (!uri.isAbsolute() && base != null) {
                s = new URI(base).resolve(href).getRawSchemeSpecificPart()
            }
        } catch (MalformedURLException | URISyntaxException e) {
            throw new TransformerException(e)
        }
        Path path = root.resolve(s)
        if (path == null || !Files.exists(path)) {
            throw new TransformerException("file not found: " + s + " root=" + root + " path=" + path)
        } else {
            try {
                return new StreamSource(Files.newBufferedReader(path))
            } catch (IOException e) {
                throw new TransformerException(e)
            }
        }
    }
}
