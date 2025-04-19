package com.felipearrano.challenge.infraestructure.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("call_history")
public class HistotyLogEntity {

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

    @Column("is_sucess")
    private Boolean isSucces;

    @Column("error_message")
    private String errorMessage;

}
