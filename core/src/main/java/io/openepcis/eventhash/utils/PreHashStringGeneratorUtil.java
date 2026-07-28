package io.openepcis.eventhash.utils;

import com.fasterxml.jackson.databind.JsonNode;
import io.openepcis.constants.EPCIS;
import io.openepcis.eventhash.ContextNode;
import io.openepcis.identifiers.converter.util.ConverterUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;

import static io.openepcis.eventhash.constant.ConstantEventHashInfo.DATE_FORMATTER;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PreHashStringGeneratorUtil {

    /**
     * Timestamp at millisecond precision. Sub-millisecond digits are rounded half-up (CBV rule 9); values with 3 or fewer decimals are returned unchanged.
     */
    public static String formatCanonicalTime(final String value) {
        final Instant parsed = Instant.parse(value);
        final long millis = Math.round(parsed.getNano() / 1_000_000.0);
        return DATE_FORMATTER.format(Instant.ofEpochSecond(parsed.getEpochSecond()).plusMillis(millis));
    }

    /**
     * A JSON "attributed leaf" is how an XML element with attributes + simple text is encoded: e.g. {"@measurementUnitCode":"KGM","value":"3.5"}
     * exactly one "value" key (the text) plus zero or more "@"-prefixed attribute keys, and nothing else.
     **/
    public static boolean isAttributedLeaf(final JsonNode obj) {
        if (obj == null || !obj.isObject() || !obj.has("value")) {
            return false;
        }

        final Iterator<String> keys = obj.fieldNames();
        while (keys.hasNext()) {
            final String key = keys.next();

            // a non-attribute, non-value key -> not a leaf (e.g. sensorReport)
            if (!key.equals("value") && !key.startsWith("@")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Build the same ContextNode shape the XML SaxHandler produces for such an element:
     * the element carries its value inline ({ns}name=3.5), attributes become children with the JSON "@" stripped.
     */
    public static ContextNode attributedLeafToContextNode(final ContextNode parent, final String name, final JsonNode obj) {
        // value inline -> {ns}drainedWeight=3.5
        final ContextNode node = new ContextNode(parent, name, obj.get("value").asText());

        // add attributes as children
        for (final Map.Entry<String, JsonNode> attr : obj.properties()) {
            final String attrKey = attr.getKey();
            if (attrKey.startsWith("@")) {
                node.getChildren().add(new ContextNode(node, attrKey.substring(1), attr.getValue().asText()));
            }
        }

        return node;
    }

    // CBV rule 18-II: the constrained canonical Digital Link used in the pre-hash carries NO query string.
    public static String stripQueryParameters(final String uri) {
        if (StringUtils.isBlank(uri) || !uri.startsWith(EPCIS.GS1_IDENTIFIER_DOMAIN)) return uri;
        final int q = uri.indexOf("?");
        return q < 0 ? uri : uri.substring(0, q);
    }

    /**
     * Convert to URI
     */
    public static String toUri(final String v) {
        return stripQueryParameters(ConverterUtil.toURI(v));
    }

    public static String shortName(final String v) {
        return stripQueryParameters(ConverterUtil.shortNameReplacer(v));
    }

    public static String toUriClass(final String v) {
        return stripQueryParameters(ConverterUtil.toURIForClassLevelIdentifier(v));
    }
}

