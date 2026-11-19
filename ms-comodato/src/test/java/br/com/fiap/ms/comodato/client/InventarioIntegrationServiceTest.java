package br.com.fiap.ms.comodato.client;

import br.com.fiap.ms.comodato.client.dto.EquipamentoClientResponseDTO;
import br.com.fiap.ms.comodato.client.dto.EquipamentoStatusClientRequestDTO;
import br.com.fiap.ms.comodato.exceptions.ResourceNotFoundException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventarioIntegrationServiceTest {

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private InventarioIntegrationService service;

    @Test
    void buscarEquipamentoDeveRetornarDados() {
        when(inventarioClient.findEquipamentoById(1L)).thenReturn(
                new EquipamentoClientResponseDTO(1L, "EQ-001", "Cadeira", "Desc", "BOA", 1L, "DISPONIVEL"));

        EquipamentoClientResponseDTO response = service.buscarEquipamento(1L);

        assertEquals("EQ-001", response.getCodigoTag());
        assertEquals("DISPONIVEL", response.getStatus());
    }

    @Test
    void buscarEquipamentoQuandoNotFoundDeveLancarResourceNotFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/equipamentos/1",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(inventarioClient.findEquipamentoById(1L))
                .thenThrow(new FeignException.NotFound("not found", request, null, Collections.emptyMap()));

        assertThrows(ResourceNotFoundException.class, () -> service.buscarEquipamento(1L));
    }

    @Test
    void atualizarStatusDeveDelegarAoClient() {
        when(inventarioClient.atualizarStatus(eq(1L), any(EquipamentoStatusClientRequestDTO.class)))
                .thenReturn(new EquipamentoClientResponseDTO(1L, "EQ-001", "Cadeira", "Desc", "BOA", 1L, "RESERVADO"));

        service.atualizarStatus(1L, "RESERVADO");

        verify(inventarioClient).atualizarStatus(eq(1L), any(EquipamentoStatusClientRequestDTO.class));
    }

    @Test
    void atualizarStatusQuandoNotFoundDeveLancarResourceNotFound() {
        Request request = Request.create(Request.HttpMethod.PATCH, "/api/v1/equipamentos/1/status",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        when(inventarioClient.atualizarStatus(eq(1L), any(EquipamentoStatusClientRequestDTO.class)))
                .thenThrow(new FeignException.NotFound("not found", request, null, Collections.emptyMap()));

        assertThrows(ResourceNotFoundException.class, () -> service.atualizarStatus(1L, "RESERVADO"));
    }
}
