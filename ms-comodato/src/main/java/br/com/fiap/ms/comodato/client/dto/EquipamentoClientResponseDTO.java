package br.com.fiap.ms.comodato.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipamentoClientResponseDTO {

    private Long id;
    private String codigoTag;
    private String tipo;
    private String descricao;
    private String condicao;
    private Long instituicaoId;
    private String status;
}
