package com.hidariapi.util;

import java.net.URI;

public final class UrlSanitizer {

    private UrlSanitizer() {}

    public static URI toUri(String rawUrl) {
        try {
            return URI.create(rawUrl);
        } catch (IllegalArgumentException ignored) {
            return rebuildUri(rawUrl);
        }
    }

    private static URI rebuildUri(String rawUrl) {
        int schemeEnd = rawUrl.indexOf("://");
        if (schemeEnd < 0) {
            throw new IllegalArgumentException("Invalid URL: " + rawUrl);
        }

        String scheme = rawUrl.substring(0, schemeEnd);
        int authorityStart = schemeEnd + 3;
        int pathStart = indexOfAny(rawUrl, authorityStart, '/', '?', '#');

        String authority = pathStart >= 0 ? rawUrl.substring(authorityStart, pathStart) : rawUrl.substring(authorityStart);
        String remainder = pathStart >= 0 ? rawUrl.substring(pathStart) : "";

        String path = "";
        String query = null;
        String fragment = null;

        if (!remainder.isEmpty()) {
            int queryIndex = remainder.indexOf('?');
            int fragmentIndex = remainder.indexOf('#');

            if (queryIndex >= 0) {
                path = remainder.substring(0, queryIndex);
                if (fragmentIndex >= 0) {
                    query = remainder.substring(queryIndex + 1, fragmentIndex);
                    fragment = remainder.substring(fragmentIndex + 1);
                } else {
                    query = remainder.substring(queryIndex + 1);
                }
            } else if (fragmentIndex >= 0) {
                path = remainder.substring(0, fragmentIndex);
                fragment = remainder.substring(fragmentIndex + 1);
            } else {
                path = remainder;
            }
        }

        var authorityParts = parseAuthority(authority);
        try {
            return new URI(
                    scheme,
                    authorityParts.userInfo(),
                    authorityParts.host(),
                    authorityParts.port(),
                    path.isEmpty() ? null : path,
                    query,
                    fragment);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL: " + rawUrl, e);
        }
    }

    private static int indexOfAny(String value, int fromIndex, char... chars) {
        for (int i = fromIndex; i < value.length(); i++) {
            char current = value.charAt(i);
            for (char target : chars) {
                if (current == target) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static AuthorityParts parseAuthority(String authority) {
        String userInfo = null;
        String hostPort = authority;

        int atIndex = authority.lastIndexOf('@');
        if (atIndex >= 0) {
            userInfo = authority.substring(0, atIndex);
            hostPort = authority.substring(atIndex + 1);
        }

        String host = hostPort;
        int port = -1;

        if (hostPort.startsWith("[")) {
            int closing = hostPort.indexOf(']');
            if (closing < 0) {
                throw new IllegalArgumentException("Invalid URL authority: " + authority);
            }
            host = hostPort.substring(0, closing + 1);
            if (closing + 1 < hostPort.length() && hostPort.charAt(closing + 1) == ':') {
                port = Integer.parseInt(hostPort.substring(closing + 2));
            }
        } else {
            int colon = hostPort.lastIndexOf(':');
            if (colon > -1 && hostPort.indexOf(':') == colon) {
                String possiblePort = hostPort.substring(colon + 1);
                if (!possiblePort.isEmpty() && possiblePort.chars().allMatch(Character::isDigit)) {
                    host = hostPort.substring(0, colon);
                    port = Integer.parseInt(possiblePort);
                }
            }
        }

        return new AuthorityParts(userInfo, host, port);
    }

    private record AuthorityParts(String userInfo, String host, int port) {}
}
