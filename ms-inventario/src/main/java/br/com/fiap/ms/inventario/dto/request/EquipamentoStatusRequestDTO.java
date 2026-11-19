package br.com.fiap.ms.inventario.dto.request;

import br.com.fiap.ms.inventario.entity.StatusEquipamento;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipamentoStatusRequestDTO {

    @NotNull
    private StatusEquipamento status;
}
