package br.com.fiap.ms.inventario.dto.request;

import br.com.fiap.ms.inventario.entity.StatusEquipamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipamentoRequestDTO {

    @NotBlank
    @Size(max = 50)
    private String codigoTag;

    @NotBlank
    @Size(max = 80)
    private String tipo;

    @NotBlank
    @Size(max = 255)
    private String descricao;

    @NotBlank
    @Size(max = 80)
    private String condicao;

    @NotNull
    private Long instituicaoId;

    @NotNull
    private StatusEquipamento status;
}
