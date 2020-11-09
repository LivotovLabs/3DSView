package eu.livotov.labs.android.d3s;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities to find 3DS values in ACS webpages.
 */
final class D3SRegexUtils {

    /**
     * Pattern to find an html tag with an attribute named PaRes.
     */
    private static final Pattern paresFinder = Pattern.compile(".*?(<input[^<>]* name=\"PaRes\"[^<>]*>).*?", Pattern.DOTALL);

    /**
     * Pattern to find the value from an html tag with an attribute named value.
     */
    private static final Pattern valuePattern = Pattern.compile(".*? value=\"(\\S+?)\"", Pattern.DOTALL);

    /**
     * Finds the PaRes in an html page.
     *
     * @param html String representation of the html page to search within.
     * @return PaRes or null if not found
     */
    @Nullable
    static String findPaRes(@NonNull String html) {
        if (html.trim().isEmpty()) return null;

        String paResTag = null;
        Matcher paresMatcher = paresFinder.matcher(html);
        if (paresMatcher.find()) {
            paResTag = paresMatcher.group(1);
        }

        if (paResTag == null) return null;

        String paRes = null;
        Matcher paresValueMatcher = valuePattern.matcher(paResTag);
        if (paresValueMatcher.find()) {
            paRes = paresValueMatcher.group(1);
        }

        return paRes;
    }
}
