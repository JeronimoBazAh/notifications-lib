package com.example.notifications.support;

import com.example.notifications.error.SendException;
import com.example.notifications.model.TipoCanal;
import com.example.notifications.model.EmailNotificacion;
import com.example.notifications.spi.NotificationProvider;
import com.example.notifications.spi.ProviderResponse;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/** Fake provider: throws the scripted failures in order, then succeeds. No mocking framework needed. */
public final class ScriptedProvider implements NotificationProvider<EmailNotificacion> {
    private final String name;
    private final ConcurrentLinkedDeque<SendException> script = new ConcurrentLinkedDeque<>();
    public final AtomicInteger calls = new AtomicInteger();

    public ScriptedProvider(String name, SendException... failuresBeforeSuccess) {
        this.name = name;
        for (SendException e : failuresBeforeSuccess) script.add(e);
    }

    @Override public String name() { return name; }
    @Override public TipoCanal channel() { return TipoCanal.EMAIL; }
    @Override public Class<EmailNotificacion> notificationType() { return EmailNotificacion.class; }

    @Override
    public ProviderResponse send(EmailNotificacion n) throws SendException {
        calls.incrementAndGet();
        SendException failure = script.poll();
        if (failure != null) throw failure;
        return new ProviderResponse(name + "-msg-id", "ok");
    }
}
