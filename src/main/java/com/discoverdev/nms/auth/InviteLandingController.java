package com.discoverdev.nms.auth;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public HTTPS landing page for invitation links.
 *
 * Email clients only linkify http/https URLs, not custom schemes like
 * {@code nurseryadmin://}. So the invite email points here (an https URL that IS
 * clickable); this page then hands off to the app's deep link. If the app is not
 * installed, the user sees a short fallback message.
 */
@RestController
public class InviteLandingController {

    @GetMapping(value = "/invite/open", produces = MediaType.TEXT_HTML_VALUE)
    public String open(@RequestParam(name = "token", required = false) String token) {
        String safe = jsString(token);
        String deepLink = "nurseryadmin://invitation?token=" + urlEncode(token);
        return """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Open your Yawmy Care invitation</title>
              <style>
                body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; background:#f6f7f9;
                       margin:0; display:flex; min-height:100vh; align-items:center; justify-content:center; }
                .card { background:#fff; max-width:420px; margin:16px; padding:28px 24px; border-radius:16px;
                        box-shadow:0 8px 30px rgba(0,0,0,.08); text-align:center; }
                h1 { font-size:20px; margin:0 0 8px; color:#1a1a1a; }
                p { color:#555; font-size:15px; line-height:1.5; margin:0 0 20px; }
                a.btn { display:inline-block; background:#2f6fed; color:#fff; text-decoration:none;
                        font-weight:600; padding:14px 22px; border-radius:12px; }
                .hint { margin-top:18px; font-size:13px; color:#888; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>Open your invitation</h1>
                <p>Tap the button below to open the Nursery Admin app and set up your account.</p>
                <a class="btn" id="open" href="%s">Open the app</a>
                <div class="hint">Don't have the app yet? Install <b>Nursery Admin</b> from your app store, then tap this link again.</div>
              </div>
              <script>
                var token = "%s";
                var deepLink = "nurseryadmin://invitation?token=" + encodeURIComponent(token);
                document.getElementById('open').setAttribute('href', deepLink);
                // Try to open the app automatically.
                setTimeout(function () { window.location.href = deepLink; }, 300);
              </script>
            </body>
            </html>
            """.formatted(deepLink, safe);
    }

    private static String jsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("<", "\\u003c").replace(">", "\\u003e");
    }

    private static String urlEncode(String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
