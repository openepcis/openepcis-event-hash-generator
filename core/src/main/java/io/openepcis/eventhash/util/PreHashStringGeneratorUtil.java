package io.openepcis.eventhash.util;

import io.openepcis.constants.EPCIS;
import io.openepcis.identifiers.converter.util.ConverterUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

import static io.openepcis.eventhash.constant.ConstantEventHashInfo.DATE_FORMATTER;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PreHashStringGeneratorUtil {

    /**
     * Timestamp at millisecond precision. Sub-millisecond digits are rounded half-up (CBV rule 9); values with 3 or fewer decimals are returned unchanged.
     */
    public static String formatCanonicalTime(final String value) {
        final Instant parsed = Instant.parse(value);
        final long millis = Math.round(parsed.getNano() / 1_000_000.0);
        return DATE_FORMATTER.format(Instant.ofEpochSecond(parsed.getEpochSecond()).plusMillis(millis));
    }

    /**
     * CBV rule 18-II: the constrained canonical Digital Link used in the pre-hash carries NO query string.
     */
    public static String stripQueryParameters(final String uri) {
        if (StringUtils.isBlank(uri) || !uri.startsWith(EPCIS.GS1_IDENTIFIER_DOMAIN)) return uri;
        final int q = uri.indexOf("?");
        return q < 0 ? uri : uri.substring(0, q);
    }

    /**
     * URN instance identifier to constrained canonical Digital Link (query string stripped, CBV rule 18-II).
     */
    public static String toUri(final String v) {
        return stripQueryParameters(ConverterUtil.toURI(v));
    }

    /**
     * Web-URI or short-name Digital Link to constrained canonical form on id.gs1.org (query string stripped, CBV rule 18-II).
     */
    public static String shortName(final String v) {
        return stripQueryParameters(ConverterUtil.shortNameReplacer(v));
    }

    /**
     * URN class-level identifier to constrained canonical Digital Link (query string stripped, CBV rule 18-II).
     */
    public static String toUriClass(final String v) {
        return stripQueryParameters(ConverterUtil.toURIForClassLevelIdentifier(v));
    }
}

