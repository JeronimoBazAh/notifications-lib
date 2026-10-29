package com.example.notifications;

import com.example.notifications.error.ErrorCode;
import com.example.notifications.error.NotificationException;
import com.example.notifications.error.SendException;
import com.example.notifications.event.EventoNotificacion;
import com.example.notifications.model.EmailNotification;
import com.example.notifications.support.ScriptedProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {
    // Thread-safe: sendAll() runs deliveries concurrently.
    private final List<Duration> sleeps = new CopyOnWriteArrayList<>();
    private final List<EventoNotificacion> events = new CopyOnWriteArrayList<>();

    private static EmailNotification email() {
        return EmailNotification.builder().from("a@b.com").to("c@d.com").subject("s").textBody("t").build();
    }

    private static SendException retryable() {
        return new SendException(ErrorCode.PROVIDER_UNAVAILABLE, "p", true, "down");
    }

    private static SendException fatal() {
        return new SendException(ErrorCode.AUTHENTICATION, "p", false, "bad key");
    }

    private static SendException rejected() {
        return new SendException(ErrorCode.REJECTED_BY_PROVIDER, "p", false, "bad content");
    }

    private NotificationService.Builder base() {
        return NotificationService.builder().sleeper(sleeps::add).listener(events::add);
    }

    @Test
    void sendsThroughRegisteredProvider() {
        SendResult r = base().provider(new ScriptedProvider("primary")).build().send(email());
        assertTrue(r.isSuccess());
        assertEquals("primary", r.provider());
        assertEquals("primary-msg-id", r.providerMessageId());
        assertEquals(1, r.attempts());
    }

    @Test
    void retriesRetryableFailuresWithBackoff() {
        ScriptedProvider p = new ScriptedProvider("primary", retryable(), retryable());
        SendResult r = base().provider(p).retryPolicy(RetryPolicy.exponential(3, Duration.ofMillis(100))).build().send(email());
        assertTrue(r.isSuccess());
        assertEquals(3, r.attempts());
        assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(200)), sleeps);
    }

    @Test
    void doesNotRetryNonRetryableFailures() {
        ScriptedProvider p = new ScriptedProvider("primary", fatal());
        SendResult r = base().provider(p).retryPolicy(RetryPolicy.exponential(3, Duration.ofMillis(1))).build().send(email());
        assertFalse(r.isSuccess());
        assertEquals(ErrorCode.AUTHENTICATION, r.error().code());
        assertEquals(1, p.calls.get());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void failsOverToNextProviderAndPublishesEvents() {
        ScriptedProvider primary = new ScriptedProvider("primary", fatal());
        ScriptedProvider backup = new ScriptedProvider("backup");
        SendResult r = base().provider(primary).provider(backup).build().send(email());
        assertTrue(r.isSuccess());
        assertEquals("backup", r.provider());
        assertEquals(
                List.of(EventoNotificacion.Type.ATTEMPT_FAILED, EventoNotificacion.Type.FAILOVER, EventoNotificacion.Type.SENT),
                events.stream().map(EventoNotificacion::type).toList());
    }

    @Test
    void skipOnRejectionPolicyDoesNotFailOverOnContentRejection() {
        ScriptedProvider primary = new ScriptedProvider("primary", rejected());
        ScriptedProvider backup = new ScriptedProvider("backup");
        SendResult r = base().provider(primary).provider(backup).failoverPolicy(FailoverPolicy.SKIP_ON_REJECTION).build().send(email());
        assertFalse(r.isSuccess());
        assertEquals(ErrorCode.REJECTED_BY_PROVIDER, r.error().code());
        assertEquals(0, backup.calls.get());
    }

    @Test
    void skipOnRejectionPolicyStillFailsOverOnOutages() {
        ScriptedProvider primary = new ScriptedProvider("primary", retryable());
        ScriptedProvider backup = new ScriptedProvider("backup");
        SendResult r = base().provider(primary).provider(backup).failoverPolicy(FailoverPolicy.SKIP_ON_REJECTION).build().send(email());
        assertTrue(r.isSuccess());
        assertEquals("backup", r.provider());
    }

    @Test
    void neverPolicyUsesOnlyThePrimary() {
        ScriptedProvider primary = new ScriptedProvider("primary", fatal());
        ScriptedProvider backup = new ScriptedProvider("backup");
        SendResult r = base().provider(primary).provider(backup).failoverPolicy(FailoverPolicy.NEVER).build().send(email());
        assertFalse(r.isSuccess());
        assertEquals(0, backup.calls.get());
    }

    @Test
    void reportsFailureWhenAllProvidersFail() {
        SendResult r = base().provider(new ScriptedProvider("a", fatal())).provider(new ScriptedProvider("b", retryable()))
                .build().send(email());
        assertFalse(r.isSuccess());
        assertEquals(2, r.attempts());
        assertEquals(EventoNotificacion.Type.FAILED, events.get(events.size() - 1).type());
        assertThrows(NotificationException.class, r::orElseThrow);
    }

    @Test
    void failsClearlyWhenNoProviderForChannel() {
        SendResult r = base().build().send(email());
        assertEquals(ErrorCode.NO_PROVIDER, r.error().code());
        assertEquals(0, r.attempts());
    }

    @Test
    void interruptionWhileWaitingToRetryFailsAndRestoresInterruptFlag() {
        ScriptedProvider p = new ScriptedProvider("p", retryable());
        NotificationService service = NotificationService.builder()
                .provider(p)
                .retryPolicy(RetryPolicy.exponential(3, Duration.ofMillis(1)))
                .sleeper(d -> { throw new InterruptedException(); })
                .build();
        SendResult r = service.send(email());
        assertFalse(r.isSuccess());
        assertEquals(ErrorCode.UNEXPECTED, r.error().code());
        assertTrue(Thread.interrupted(), "interrupt flag must be restored (this call also clears it for other tests)");
    }

    @Test
    void faultyListenerDoesNotBreakDelivery() {
        SendResult r = base().provider(new ScriptedProvider("p"))
                .listener(e -> { throw new IllegalStateException("boom"); })
                .build().send(email());
        assertTrue(r.isSuccess());
    }

    @Test
    void eventsCarryNotificationMetadataForCorrelation() {
        EmailNotification n = EmailNotification.builder().from("a@b.com").to("c@d.com").subject("s").textBody("t")
                .metadata("tenant", "acme").build();
        base().provider(new ScriptedProvider("p")).build().send(n);
        assertEquals("acme", events.get(0).metadata().get("tenant"));
    }

    @Test
    void sendAllKeepsOrderAndIsolatesFailures() {
        ScriptedProvider p = new ScriptedProvider("p", fatal()); // first call fails, the rest succeed
        try (NotificationService s = base().provider(p).build()) {
            List<EmailNotification> batch = List.of(email(), email(), email());
            List<SendResult> results = s.sendAll(batch).join();
            assertEquals(3, results.size());
            assertEquals(batch.stream().map(EmailNotification::id).toList(),
                    results.stream().map(SendResult::notificationId).toList());
            assertEquals(2, results.stream().filter(SendResult::isSuccess).count());
        }
    }

    @Test
    void sendAsyncCompletesNormallyEvenWhenDeliveryFails() {
        try (NotificationService s = base().provider(new ScriptedProvider("p", fatal())).build()) {
            SendResult r = s.sendAsync(email()).join();
            assertFalse(r.isSuccess());
        }
    }
}
