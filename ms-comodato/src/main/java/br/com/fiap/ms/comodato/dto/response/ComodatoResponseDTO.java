package br.com.fiap.ms.comodato.dto.response;

import br.com.fiap.ms.comodato.entity.Comodato;
import br.com.fiap.ms.comodato.entity.StatusComodato;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComodatoResponseDTO {

    private Long id;
    private String nomeSolicitante;
    private String documentoSolicitante;
    private Long equipamentoId;
    private StatusComodato status;
    private LocalDate dataReserva;
    private LocalDate prazoDevolucao;
    private Boolean consentimentoLgpd;

    public ComodatoResponseDTO(Comodato entity) {
        this.id = entity.getId();
        this.nomeSolicitante = entity.getNomeSolicitante();
        this.documentoSolicitante = entity.getDocumentoSolicitante();
        this.equipamentoId = entity.getEquipamentoId();
        this.status = entity.getStatus();
        this.dataReserva = entity.getDataReserva();
        this.prazoDevolucao = entity.getPrazoDevolucao();
        this.consentimentoLgpd = entity.getConsentimentoLgpd();
    }
}
