import uk.rogerhunter.focusgram.dm.NavigationPolicy;
import static uk.rogerhunter.focusgram.dm.NavigationPolicy.Route.*;
public class NavigationPolicyTest {
    static int count;
    static void check(String url, NavigationPolicy.Route expected) {
        count++;
        if (NavigationPolicy.route(url) != expected) throw new AssertionError(url);
    }
    public static void main(String[] args) {
        check(NavigationPolicy.INBOX, INTERNAL);
        check("https://www.instagram.com/direct/t/123/", INTERNAL);
        check("https://instagram.com/direct/new/", INTERNAL);
        check("https://www.instagram.com/accounts/login/?next=/direct/inbox/", INTERNAL);
        check("https://www.instagram.com/challenge/abc/", INTERNAL);
        check("https://www.instagram.com/two_factor/", INTERNAL);
        check("https://www.instagram.com/", INBOX);
        check("https://www.instagram.com/explore/", INBOX);
        check("https://www.instagram.com/reels/abc/", INBOX);
        check("https://www.instagram.com/stories/abc/", INBOX);
        check("https://www.instagram.com/p/abc/", INBOX);
        check("https://www.instagram.com/some_profile/", INBOX);
        check("https://example.com/", EXTERNAL);
        check("https://l.instagram.com/?u=https://example.com/", EXTERNAL);
        check("https://www.instagram.com.evil.example/direct/", EXTERNAL);
        check("https://instagram.com@evil.example/direct/", DENY);
        check("https://www.instagram.com:444/direct/", EXTERNAL);
        check("javascript:alert(1)", DENY);
        check("file:///etc/passwd", DENY);
        check("content://private/session", DENY);
        check("intent://instagram/#Intent;end", DENY);
        check("data:text/html,hello", DENY);
        check("broken url", DENY);
        check("mailto:test@example.com", EXTERNAL);
        System.out.println("Passed " + count + " navigation cases.");
    }
}
