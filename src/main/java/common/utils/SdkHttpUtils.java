package common.utils;

import com.ksc.SignableRequest;
import com.ksc.http.HttpMethodName;
import com.ksc.util.StringUtils;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SdkHttpUtils {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Pattern ENCODED_CHARACTERS_PATTERN;

    public SdkHttpUtils() {
    }

    public static String urlEncode(String value, boolean path) {
        if (value == null) {
            return "";
        } else {
            try {
                String encoded = URLEncoder.encode(value, "UTF-8");
                Matcher matcher = ENCODED_CHARACTERS_PATTERN.matcher(encoded);

                StringBuffer buffer;
                String replacement;
                for(buffer = new StringBuffer(encoded.length()); matcher.find(); matcher.appendReplacement(buffer, replacement)) {
                    replacement = matcher.group(0);
                    if ("+".equals(replacement)) {
                        replacement = "%20";
                    } else if ("*".equals(replacement)) {
                        replacement = "%2A";
                    } else if ("%7E".equals(replacement)) {
                        replacement = "~";
                    } else if (path && "%2F".equals(replacement)) {
                        replacement = "/";
                    }
                }

                matcher.appendTail(buffer);
                return buffer.toString();
            } catch (UnsupportedEncodingException var6) {
                throw new RuntimeException(var6);
            }
        }
    }

    public static String urlDecode(String value) {
        if (value == null) {
            return null;
        } else {
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException var2) {
                throw new RuntimeException(var2);
            }
        }
    }

    public static boolean isUsingNonDefaultPort(URI uri) {
        String scheme = StringUtils.lowerCase(uri.getScheme());
        int port = uri.getPort();
        if (port <= 0) {
            return false;
        } else if (scheme.equals("http") && port == 80) {
            return false;
        } else {
            return !scheme.equals("https") || port != 443;
        }
    }

    public static boolean usePayloadForQueryParameters(SignableRequest<?> request) {
        boolean requestIsPOST = HttpMethodName.POST.equals(request.getHttpMethod());
        boolean requestHasNoPayload = request.getContent() == null;
        return requestIsPOST && requestHasNoPayload;
    }

    public static String appendUri(String baseUri, String path) {
        return appendUri(baseUri, path, false);
    }

    public static String appendUri(String baseUri, String path, boolean escapeDoubleSlash) {
        String resultUri = baseUri;
        if (path != null && path.length() > 0) {
            if (path.startsWith("/")) {
                if (baseUri.endsWith("/")) {
                    resultUri = baseUri.substring(0, baseUri.length() - 1);
                }
            } else if (!baseUri.endsWith("/")) {
                resultUri = baseUri + "/";
            }

            path = urlEncode(path, true);
            if (escapeDoubleSlash) {
                resultUri = resultUri + path.replace("//", "/%2F");
            } else {
                resultUri = resultUri + path;
            }
        } else if (!baseUri.endsWith("/")) {
            resultUri = baseUri + "/";
        }

        return resultUri;
    }

    static {
        StringBuilder pattern = new StringBuilder();
        pattern.append(Pattern.quote("+")).append("|").append(Pattern.quote("*")).append("|").append(Pattern.quote("%7E")).append("|").append(Pattern.quote("%2F"));
        ENCODED_CHARACTERS_PATTERN = Pattern.compile(pattern.toString());
    }
}
