package com.example.notifications.provider;

import com.example.notifications.spi.Transport;
import com.example.notifications.spi.TransportRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedTransportsTest {
    private static final TransportRequest REQUEST = new TransportRequest("POST", "https://example.test/send",
            Map.of("Authorization", "Bearer top-secret"), "{\"to\":\"ana@example.com\",\"text\":\"your code is 4821\"}");

    @Test
    void describeContainsOnlyMethodAndUrl() {
        assertEquals("POST https://example.test/send", SimulatedTransports.describe(REQUEST));
    }

    @Test
    void everySimulatedTransportLogsNeitherCredentialsNorMessageContent() throws Exception {
        List<String> logged = new CopyOnWriteArrayList<>();
        Logger logger = Logger.getLogger("notifications.simulated"); // System.Logger delegates to java.util.logging
        Handler capture = new Handler() {
            @Override public void publish(LogRecord record) { logged.add(record.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(capture);
        try {
            List<Transport> all = List.of(SimulatedTransports.sendGrid(), SimulatedTransports.mailgun("mg.example.com"),
                    SimulatedTransports.twilio(), SimulatedTransports.fcm("proj"), SimulatedTransports.slack());
            for (Transport t : all) {
                t.execute(REQUEST);
            }
        } finally {
            logger.removeHandler(capture);
        }

        assertEquals(5, logged.size(), "each transport must log exactly one line");
        for (String line : logged) {
            assertTrue(line.contains("https://example.test/send"), line);
            assertFalse(line.contains("top-secret"), "credentials leaked: " + line);
            assertFalse(line.contains("ana@example.com"), "personal data leaked: " + line);
            assertFalse(line.contains("4821"), "message content leaked: " + line);
        }
    }
}
