package br.com.fiap.ms.comodato.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tb_comodato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comodato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_solicitante", nullable = false, length = 150)
    private String nomeSolicitante;

    @Column(name = "documento_solicitante", nullable = false, length = 20)
    private String documentoSolicitante;

    @Column(name = "equipamento_id", nullable = false)
    private Long equipamentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusComodato status;

    @Column(name = "data_reserva", nullable = false)
    private LocalDate dataReserva;

    @Column(name = "prazo_devolucao", nullable = false)
    private LocalDate prazoDevolucao;

    @Column(name = "consentimento_lgpd", nullable = false)
    private Boolean consentimentoLgpd;
}
