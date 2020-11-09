package eu.livotov.labs.android.d3s;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;

/**
 * Utilities to find 3DS values in ACS webpages.
 */
final class D3SRegexUtils {

    /**
     * Pattern to find the value of an attribute named value from an html tag with an attribute named name and a value of PaRes.
     */
    private static final Pattern paresFinder = compile("<input(?=.+?name=\"PaRes\")(?=.+?value=\"(\\S+?)\").+>", DOTALL | CASE_INSENSITIVE);

    /**
     * Finds the PaRes in an html page.
     * <p>
     * Note: If more than one PaRes is found in a page only the first will be returned.
     *
     * @param html String representation of the html page to search within.
     * @return PaRes or null if not found
     */
    @Nullable
    static String findPaRes(@NonNull String html) {
        if (html.trim().isEmpty()) return null;

        String paRes = null;
        Matcher paresMatcher = paresFinder.matcher(html);
        if (paresMatcher.find()) {
            paRes = paresMatcher.group(1);
        }

        return paRes;
    }
}