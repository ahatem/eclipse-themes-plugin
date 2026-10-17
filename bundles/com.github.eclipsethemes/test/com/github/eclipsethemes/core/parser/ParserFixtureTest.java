package com.github.eclipsethemes.core.parser;

import static org.junit.Assert.*;
import org.junit.Test;
import com.github.eclipsethemes.EclipseThemes;

public class ParserFixtureTest {
    @Test public void failedAcquisitionAndFailedBodyPreservePreviousActivator() throws Exception {
        Object previous = EclipseThemes.instance();
        assertThrows(NullPointerException.class, () -> ParserTestSupport.installLogger(null));
        assertSame(previous, EclipseThemes.instance());
        assertThrows(IllegalStateException.class, () -> {
            try (var fixture = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
                assertNotSame(previous, EclipseThemes.instance());
                throw new IllegalStateException("body failure");
            }
        });
        assertSame(previous, EclipseThemes.instance());
        try (var fixture = ParserTestSupport.installLogger(ParserTestSupport.quietLog())) {
            assertNotSame(previous, EclipseThemes.instance());
        }
        assertSame(previous, EclipseThemes.instance());
    }
}
