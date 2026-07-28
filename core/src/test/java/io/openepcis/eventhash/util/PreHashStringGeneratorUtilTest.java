package io.openepcis.eventhash.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreHashStringGeneratorUtilTest {

    @Test
    void stripsQueryFromCanonicalDl() {
        assertEquals("https://id.gs1.org/01/04012345111118/21/9876", PreHashStringGeneratorUtil.stripQueryParameters("https://id.gs1.org/01/04012345111118/21/9876?17=20260910"));
    }

    @Test
    void leavesFreeFormUrlUntouched() {   // not a GS1 DL -> keep query (matches Python)
        String url = "https://identifiers.org/inchikey:ABC?x=1";
        assertEquals(url, PreHashStringGeneratorUtil.stripQueryParameters(url));
    }

    @Test
    void nullAndNoQueryAreNoOps() {
        assertEquals(null, PreHashStringGeneratorUtil.stripQueryParameters(null));
        assertEquals("https://id.gs1.org/01/09780345418913", PreHashStringGeneratorUtil.stripQueryParameters("https://id.gs1.org/01/09780345418913"));
    }
}