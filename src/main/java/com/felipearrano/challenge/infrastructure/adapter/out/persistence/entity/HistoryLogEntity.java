package com.felipearrano.challenge.infrastructure.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("call_history")
public class HistoryLogEntity implements Persistable<UUID>{

    @Id
    private UUID id;

    @Column("timestamp")
    private Instant timestamp;

    @Column("endpoint_invoked")
    private String endpointInvoked;

    @Column("parameters_received")
    private String parametersReceived;

    @Column("response_body")
    private String responseBody;

    @Column("http_status")
    private Integer httpStatus;

    @Column("is_success")
    private Boolean isSuccess;

    @Column("error_message")
    private String errorMessage;

    /**
     * Indica a Spring Data si esta entidad es nueva (debe hacer INSERT) o no (debe hacer UPDATE).
     * Como siempre generamos un UUID nuevo antes de llamar a save() para los logs,
     * siempre queremos que haga un INSERT.
     * @return siempre true
     */
    @Override
    public boolean isNew() {
        // Forzamos a que siempre se considere nueva para que save() haga INSERT
        return true;
    }
}

