package com.example.vehicleprivacy;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class PrivacyRuleProposalTest {
    private Preset preset() { return new Preset("id", "가족", Collections.emptyMap()); }

    @Test public void supportedProposalDoesNotChangePresetBeforeConfirmation() {
        Preset draft = preset();
        draft.setBoolean("protect_notifications", false);
        draft.setBoolean("hide_notification_preview", false);
        PrivacyRuleProposal.Result proposal = PrivacyRuleProposal.parse("알림 미리보기를 가려줘");
        assertEquals(PrivacyRuleProposal.Status.RECOGNIZED, proposal.getStatus());
        assertEquals(2, proposal.getChanges().size());
        assertFalse(draft.getBoolean("protect_notifications", true));
        proposal.applyTo(draft);
        assertTrue(draft.getBoolean("protect_notifications", false));
        assertTrue(draft.getBoolean("hide_notification_preview", false));
    }

    @Test public void unknownOrAmbiguousSentencesNeverChangeSettings() {
        Preset draft = preset();
        draft.setBoolean("protect_notifications", false);
        for (String input : new String[] {"보호 설정 켜줘", "알림 미리보기를 가려줘 말고 취소", "알림을 가리지 마"}) {
            PrivacyRuleProposal.Result proposal = PrivacyRuleProposal.parse(input);
            assertEquals(PrivacyRuleProposal.Status.UNRECOGNIZED, proposal.getStatus());
            assertTrue(proposal.getChanges().isEmpty());
            proposal.applyTo(draft);
            assertFalse(draft.getBoolean("protect_notifications", true));
        }
    }

    @Test public void supportedContactRuleOnlyChangesItsExplicitSettings() {
        Preset draft = preset();
        draft.setBoolean("protect_contact", false);
        draft.setBoolean("hide_contact_photo", false);
        draft.setBoolean("hold_notifications", false);
        PrivacyRuleProposal.Result proposal = PrivacyRuleProposal.parse("  연락처   사진을 가려줘  ");
        assertEquals(PrivacyRuleProposal.Status.RECOGNIZED, proposal.getStatus());
        proposal.applyTo(draft);
        assertTrue(draft.getBoolean("protect_contact", false));
        assertTrue(draft.getBoolean("hide_contact_photo", false));
        assertFalse(draft.getBoolean("hold_notifications", true));
    }

    @Test public void emptyAndLongInputAreRejectedWithoutKeepingOriginal() {
        assertEquals(PrivacyRuleProposal.Status.EMPTY, PrivacyRuleProposal.parse(null).getStatus());
        assertEquals(PrivacyRuleProposal.Status.EMPTY, PrivacyRuleProposal.parse("  ").getStatus());
        String input = new String(new char[PrivacyRuleProposal.MAX_INPUT_CHARS + 1]).replace('\0', 'a');
        PrivacyRuleProposal.Result proposal = PrivacyRuleProposal.parse(input);
        assertEquals(PrivacyRuleProposal.Status.TOO_LONG, proposal.getStatus());
        assertFalse(proposal.getDescription().contains(input));
        assertTrue(proposal.getChanges().isEmpty());
    }

    @Test public void supportedMappingsAreImmutableAndDoNotContainInput() {
        String input = "기본 화면을 가려줘";
        PrivacyRuleProposal.Result proposal = PrivacyRuleProposal.parse(input);
        assertEquals(PrivacyRuleProposal.Status.RECOGNIZED, proposal.getStatus());
        assertEquals("true", proposal.getChanges().get("protect_screen"));
        assertEquals("hide", proposal.getChanges().get("screen_mode"));
        assertFalse(proposal.getDescription().contains(input));
        try { proposal.getChanges().put("protect_screen", "false"); fail("proposal must be immutable"); }
        catch (UnsupportedOperationException expected) { }
    }
}
