package com.example.notifications.event;

/**
 Las excepciones en los *listeners* se capturan y registran: un *listener* con errores nunca interrumpe la entrega.
 */
@FunctionalInterface
public interface NotificationListener {
    void onEvent(NotificationEvent event);
}
