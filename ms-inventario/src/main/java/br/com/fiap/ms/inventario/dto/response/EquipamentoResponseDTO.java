package br.com.fiap.ms.inventario.dto.response;

import br.com.fiap.ms.inventario.entity.Equipamento;
import br.com.fiap.ms.inventario.entity.StatusEquipamento;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipamentoResponseDTO {

    private Long id;
    private String codigoTag;
    private String tipo;
    private String descricao;
    private String condicao;
    private Long instituicaoId;
    private StatusEquipamento status;

    public EquipamentoResponseDTO(Equipamento entity) {
        this.id = entity.getId();
        this.codigoTag = entity.getCodigoTag();
        this.tipo = entity.getTipo();
        this.descricao = entity.getDescricao();
        this.condicao = entity.getCondicao();
        this.instituicaoId = entity.getInstituicaoId();
        this.status = entity.getStatus();
    }
}
