package br.dev.ctrls.api.application.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Evento publicado quando uma submissão é criada e está pronta para processamento assíncrono.
 */
@Getter
public class SubmissionCreatedEvent extends ApplicationEvent {

    private final UUID submissionId;

    public SubmissionCreatedEvent(Object source, UUID submissionId) {
        super(source);
        this.submissionId = submissionId;
    }
}

