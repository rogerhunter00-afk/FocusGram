package uk.rogerhunter.focusgram.dm;

import java.net.URI;

/** Main-frame routing only: CDN resources used by chats are not blocked. */
public final class NavigationPolicy {
    public enum Route { INTERNAL, INBOX, EXTERNAL, DENY }
    public static final String INBOX = "https://www.instagram.com/direct/inbox/";
    public static boolean instagram(URI uri) {
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
            && (uri.getPort() == -1 || uri.getPort() == 443)
            && ("www.instagram.com".equalsIgnoreCase(uri.getHost())
                || "instagram.com".equalsIgnoreCase(uri.getHost()));
    }
    public static boolean direct(String url) {
        try { URI u = URI.create(url); return instagram(u) && u.getPath().startsWith("/direct/"); }
        catch (Exception e) { return false; }
    }
    public static Route route(String url) {
        try {
            URI u = URI.create(url);
            if (instagram(u)) {
                String p = u.getPath();
                if (p.startsWith("/direct/") || p.equals("/direct")
                    || p.startsWith("/accounts/") || p.startsWith("/challenge/")
                    || p.startsWith("/checkpoint/") || p.startsWith("/two_factor/")
                    || p.startsWith("/consent/")) return Route.INTERNAL;
                return Route.INBOX;
            }
            String s = u.getScheme();
            if (("https".equalsIgnoreCase(s) || "http".equalsIgnoreCase(s))
                && u.getHost() != null && u.getUserInfo() == null) return Route.EXTERNAL;
            if ("mailto".equalsIgnoreCase(s) || "tel".equalsIgnoreCase(s)) return Route.EXTERNAL;
        } catch (Exception ignored) { }
        return Route.DENY;
    }
    private NavigationPolicy() { }
}
