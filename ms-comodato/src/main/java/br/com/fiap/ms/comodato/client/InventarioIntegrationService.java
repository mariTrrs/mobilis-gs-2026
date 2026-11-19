package br.com.fiap.ms.comodato.client;

import br.com.fiap.ms.comodato.client.dto.EquipamentoClientResponseDTO;
import br.com.fiap.ms.comodato.client.dto.EquipamentoStatusClientRequestDTO;
import br.com.fiap.ms.comodato.exceptions.ResourceNotFoundException;
import feign.FeignException;
import org.springframework.stereotype.Service;

@Service
public class InventarioIntegrationService {

    private final InventarioClient inventarioClient;

    public InventarioIntegrationService(InventarioClient inventarioClient) {
        this.inventarioClient = inventarioClient;
    }

    public EquipamentoClientResponseDTO buscarEquipamento(Long equipamentoId) {
        try {
            return inventarioClient.findEquipamentoById(equipamentoId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Equipamento não encontrado: " + equipamentoId);
        }
    }

    public void atualizarStatus(Long equipamentoId, String status) {
        try {
            inventarioClient.atualizarStatus(equipamentoId, new EquipamentoStatusClientRequestDTO(status));
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Equipamento não encontrado: " + equipamentoId);
        }
    }
}
