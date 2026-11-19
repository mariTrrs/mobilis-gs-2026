package br.com.fiap.ms.comodato.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComodatoRequestDTO {

    @NotBlank
    @Size(max = 150)
    private String nomeSolicitante;

    @NotBlank
    @Size(max = 20)
    private String documentoSolicitante;

    @NotNull
    private Long equipamentoId;

    @NotNull
    private LocalDate dataReserva;

    @NotNull
    private LocalDate prazoDevolucao;

    @NotNull
    @AssertTrue(message = "Consentimento LGPD é obrigatório")
    private Boolean consentimentoLgpd;
}
