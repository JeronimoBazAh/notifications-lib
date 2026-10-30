package com.example.notifications;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.NotificationException;
import com.example.notifications.error.SendException;
import com.example.notifications.model.ChanelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendResultTest {
    private static final SendException ERROR = new SendException(ErrorCode.AUTHENTICATION, "p", false, "bad key");

    @Test
    void sentIsSuccessfulHasNoErrorAndOrElseThrowReturnsItself() {
        SendResult r = new SendResult.Sent("1", ChanelType.EMAIL, "sendgrid", "abc", 1);
        assertTrue(r.isSuccess());
        assertNull(r.error());
        assertSame(r, r.orElseThrow());
    }

    @Test
    void failedIsNotSuccessfulHasNoMessageIdAndOrElseThrowThrowsTheCause() {
        SendResult r = new SendResult.Failed("1", ChanelType.EMAIL, "sendgrid", 2, ERROR);
        assertFalse(r.isSuccess());
        assertNull(r.providerMessageId());
        assertSame(ERROR, assertThrows(NotificationException.class, r::orElseThrow));
    }

    @Test
    void failedWithoutErrorCannotBeCreated() {
        assertThrows(NullPointerException.class, () -> new SendResult.Failed("1", ChanelType.EMAIL, "p", 1, null));
    }

    @Test
    void exhaustivePatternMatchingCoversBothOutcomes() {
        SendResult ok = new SendResult.Sent("1", ChanelType.SMS, "twilio", "SM1", 1);
        SendResult ko = new SendResult.Failed("2", ChanelType.SMS, "twilio", 1, ERROR);
        // No default branch: the compiler proves every case is handled.
        assertEquals("sent:SM1", describe(ok));
        assertEquals("failed:AUTHENTICATION", describe(ko));
    }

    private static String describe(SendResult result) {
        return switch (result) {
            case SendResult.Sent s -> "sent:" + s.providerMessageId();
            case SendResult.Failed f -> "failed:" + f.error().code();
        };
    }
}
