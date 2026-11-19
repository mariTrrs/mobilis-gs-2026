package br.com.fiap.ms.comodato.client;

import br.com.fiap.ms.comodato.client.dto.EquipamentoClientResponseDTO;
import br.com.fiap.ms.comodato.client.dto.EquipamentoStatusClientRequestDTO;
import br.com.fiap.ms.comodato.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.comodato.exceptions.ServiceUnavailableException;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class InventarioClientFallbackFactory implements FallbackFactory<InventarioClient> {

    @Override
    public InventarioClient create(Throwable cause) {
        return new InventarioClient() {
            @Override
            public EquipamentoClientResponseDTO findEquipamentoById(Long id) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException("Equipamento não encontrado: " + id);
                }
                throw new ServiceUnavailableException("Serviço de inventário indisponível. Operação não realizada.");
            }

            @Override
            public EquipamentoClientResponseDTO atualizarStatus(Long id, EquipamentoStatusClientRequestDTO dto) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException("Equipamento não encontrado: " + id);
                }
                throw new ServiceUnavailableException("Serviço de inventário indisponível. Operação não realizada.");
            }
        };
    }
}
