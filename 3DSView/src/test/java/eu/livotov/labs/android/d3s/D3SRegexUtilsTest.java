package eu.livotov.labs.android.d3s;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class D3SRegexUtilsTest {

    @Test
    public void given_empty_html_when_PaRes_match_attempted_then_should_return_null() {
        // Given
        String html = "";

        // When
        String paRes = D3SRegexUtils.findPaRes(html);

        // Then
        assertThat(paRes)
                .isNull();
    }

    @Test
    public void given_blank_html_when_PaRes_match_attempted_then_should_return_null() {
        // Given
        String html = "               ";

        // When
        String paRes = D3SRegexUtils.findPaRes(html);

        // Then
        assertThat(paRes)
                .isNull();
    }

    @Test
    public void given_html_with_no_pares_when_PaRes_match_attempted_then_should_return_null() {
        // Given
        String html = "<html>" +
                "<head>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"threeds-one\">" +
                "</html>";

        // When
        String paRes = D3SRegexUtils.findPaRes(html);

        // Then
        assertThat(paRes)
                .isNull();
    }

    @Test
    public void given_html_with_empty_pares_when_PaRes_match_attempted_then_should_return_null() {
        // https://github.com/LivotovLabs/3DSView/issues/30
        // Given
        String html = "<html>" +
                "<head>\n" +
                "</head>\n" +
                "<body>\n" +
                "<form method=\"POST\" id=\"TermForm\">\n" +
                "  <input type=\"hidden\" id=\"PaRes\" name=\"PaRes\" value=\"\">\n" +
                "  <input type=\"hidden\" id=\"MD\" name=\"MD\" value=\"\">\n" +
                "</form>" +
                "</html>";

        // When
        String paRes = D3SRegexUtils.findPaRes(html);

        // Then
        assertThat(paRes)
                .isNull();
    }

    @Test
    public void given_html_with_valid_pares_when_PaRes_match_attempted_then_should_return_null() {
        // Given
        String html = "<html>" +
                "<head>\n" +
                "</head>\n" +
                "<body>\n" +
                "<form method=\"POST\" id=\"TermForm\">\n" +
                "  <input type=\"hidden\" id=\"PaRes\" name=\"PaRes\" value=\"pares_value\">\n" +
                "  <input type=\"hidden\" id=\"MD\" name=\"MD\" value=\"\">\n" +
                "</form>" +
                "</html>";

        // When
        String paRes = D3SRegexUtils.findPaRes(html);

        // Then
        assertThat(paRes)
                .isEqualTo("pares_value");
    }

    @Test
    public void given_html_with_valid_pares_case_insensitive_when_PaRes_match_attempted_then_should_return_null() {
        // Given
        String html = "<html>" +
                "<head>\n" +
                "</head>\n" +
                "<body>\n" +
                "<form method=\"POST\" id=\"TermForm\">\n" +
                "  <input type=\"hidden\" id=\"PaRes\" name=\"pares\" value=\"pares_value\">\n" +
                "  <input type=\"hidden\" id=\"MD\" name=\"MD\" value=\"\">\n" +
                "</form>" +
                "</html>";

        // When
        String paRes = D3SRegexUtils.findPaRes(html);

        // Then
        assertThat(paRes)
                .isEqualTo("pares_value");
    }
}