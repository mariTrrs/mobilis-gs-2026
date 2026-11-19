package br.com.fiap.ms.comodato.client;

import br.com.fiap.ms.comodato.client.dto.EquipamentoClientResponseDTO;
import br.com.fiap.ms.comodato.client.dto.EquipamentoStatusClientRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-inventario", fallbackFactory = InventarioClientFallbackFactory.class)
public interface InventarioClient {

    @GetMapping("/api/v1/equipamentos/{id}")
    EquipamentoClientResponseDTO findEquipamentoById(@PathVariable("id") Long id);

    @PatchMapping("/api/v1/equipamentos/{id}/status")
    EquipamentoClientResponseDTO atualizarStatus(@PathVariable("id") Long id,
                                                 @RequestBody EquipamentoStatusClientRequestDTO dto);
}
