package com.example.vehicleprivacy;

import org.junit.Test;
import static org.junit.Assert.*;

public class SensitiveUtteranceGuardTest {
    private final SensitiveUtteranceGuard guard = new SensitiveUtteranceGuard();

    @Test public void detectsPhoneAndEmailWithoutReturningTheOriginalText() {
        String input = "연락처 010-1234-5678 또는 example@example.com";
        SensitiveUtteranceGuard.Result result = guard.inspect(input);
        assertEquals(SensitiveUtteranceGuard.Status.PATTERN_FOUND, result.getStatus());
        assertTrue(result.hasPhone());
        assertTrue(result.hasEmail());
        assertFalse(result.canSpeak());
        assertFalse(result.getPublicSummary().contains("010-1234-5678"));
        assertFalse(result.getPublicSummary().contains("example@example.com"));
    }

    @Test public void noMatchIsUnverifiedAndNeverAllowsSpeech() {
        String input = "내일 비밀 회의에 참석하세요";
        SensitiveUtteranceGuard.Result result = guard.inspect(input);
        assertEquals(SensitiveUtteranceGuard.Status.UNVERIFIED, result.getStatus());
        assertFalse(result.hasPhone());
        assertFalse(result.hasEmail());
        assertFalse(result.canSpeak());
        assertFalse(result.getPublicSummary().contains(input));
    }

    @Test public void emptyAndOversizedInputsFailClosed() {
        assertEquals(SensitiveUtteranceGuard.Status.EMPTY, guard.inspect(null).getStatus());
        assertEquals(SensitiveUtteranceGuard.Status.EMPTY, guard.inspect(" \n ").getStatus());
        String longText = new String(new char[SensitiveUtteranceGuard.MAX_INPUT_CHARS + 1]).replace('\0', 'a');
        SensitiveUtteranceGuard.Result result = guard.inspect(longText);
        assertEquals(SensitiveUtteranceGuard.Status.TOO_LONG, result.getStatus());
        assertFalse(result.canSpeak());
        assertFalse(result.getPublicSummary().contains(longText));
    }

    @Test public void recognizesCommonMobileFormatsAndEmailCaseInsensitively() {
        assertTrue(guard.inspect("+82 10 1234 5678").hasPhone());
        assertTrue(guard.inspect("01012345678").hasPhone());
        assertTrue(guard.inspect("HELLO+TEST@EXAMPLE.COM").hasEmail());
        assertFalse(guard.inspect("123456789012345").hasPhone());
    }
}
