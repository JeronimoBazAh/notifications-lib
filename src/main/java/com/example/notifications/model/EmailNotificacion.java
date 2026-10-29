package com.example.notifications.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.notifications.model.Validation.copyOf;
import static com.example.notifications.model.Validation.hasText;
import static com.example.notifications.model.Validation.isEmail;

/** Mensaje de correo electrónico. Validado en el momento de su creación: no puede existir una instancia no válida. */
public record EmailNotificacion(
        String id, Instant createdAt, String from, List<String> to, List<String> cc, List<String> bcc,
        String subject, String textBody, String htmlBody, Map<String, String> metadata) implements Notificacion {

    public EmailNotificacion {
        id = id == null ? UUID.randomUUID().toString() : id;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        to = copyOf(to);
        cc = copyOf(cc);
        bcc = copyOf(bcc);
        metadata = copyOf(metadata);

        Validation v = new Validation();
        v.require(isEmail(from), "'from' must be a valid email address");
        v.require(!to.isEmpty(), "at least one 'to' recipient is required");
        to.forEach(a -> v.require(isEmail(a), "invalid 'to' address: '" + a + "'"));
        cc.forEach(a -> v.require(isEmail(a), "invalid 'cc' address: '" + a + "'"));
        bcc.forEach(a -> v.require(isEmail(a), "invalid 'bcc' address: '" + a + "'"));
        v.require(hasText(subject), "'subject' must not be blank");
        v.require(hasText(textBody) || hasText(htmlBody), "'textBody' or 'htmlBody' is required");
        v.throwIfAny("EmailNotification");
    }

    @Override
    public TipoCanal channel() {
        return TipoCanal.EMAIL;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Seguro respecto a la PII: los destinatarios y el cuerpo del mensaje nunca terminan en los registros de forma accidental. */
    @Override
    public String toString() {
        return "EmailNotification[id=" + id + ", recipients=" + (to.size() + cc.size() + bcc.size()) + "]";
    }

    public static final class Builder {
        private String id, from, subject, textBody, htmlBody;
        private final List<String> to = new ArrayList<>(), cc = new ArrayList<>(), bcc = new ArrayList<>();
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder to(String address) { this.to.add(address); return this; }
        public Builder cc(String address) { this.cc.add(address); return this; }
        public Builder bcc(String address) { this.bcc.add(address); return this; }
        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder textBody(String textBody) { this.textBody = textBody; return this; }
        public Builder htmlBody(String htmlBody) { this.htmlBody = htmlBody; return this; }
        public Builder metadata(String key, String value) { this.metadata.put(key, value); return this; }

        /** @throws com.example.notifications.error.ValidationException enumerando todas las infracciones */
        public EmailNotificacion build() {
            return new EmailNotificacion(id, null, from, to, cc, bcc, subject, textBody, htmlBody, metadata);
        }
    }
}
