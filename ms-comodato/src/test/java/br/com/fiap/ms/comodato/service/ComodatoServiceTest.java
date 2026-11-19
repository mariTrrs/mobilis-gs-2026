package br.com.fiap.ms.comodato.service;

import br.com.fiap.ms.comodato.client.InventarioIntegrationService;
import br.com.fiap.ms.comodato.client.dto.EquipamentoClientResponseDTO;
import br.com.fiap.ms.comodato.dto.request.ComodatoRequestDTO;
import br.com.fiap.ms.comodato.dto.response.ComodatoResponseDTO;
import br.com.fiap.ms.comodato.entity.Comodato;
import br.com.fiap.ms.comodato.entity.StatusComodato;
import br.com.fiap.ms.comodato.exceptions.BusinessException;
import br.com.fiap.ms.comodato.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.comodato.exceptions.ServiceUnavailableException;
import br.com.fiap.ms.comodato.repository.ComodatoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComodatoServiceTest {

    @Mock
    private ComodatoRepository repository;

    @Mock
    private InventarioIntegrationService inventarioIntegrationService;

    @InjectMocks
    private ComodatoService service;

    @Test
    void createDeveReservarEquipamentoEPersistir() {
        ComodatoRequestDTO dto = request(1L);
        when(inventarioIntegrationService.buscarEquipamento(1L)).thenReturn(
                new EquipamentoClientResponseDTO(1L, "EQ-001", "Cadeira", "Desc", "BOA", 1L, "DISPONIVEL"));
        when(repository.save(any(Comodato.class))).thenAnswer(invocation -> {
            Comodato entity = invocation.getArgument(0);
            entity.setId(100L);
            return entity;
        });

        ComodatoResponseDTO response = service.create(dto);

        assertEquals(100L, response.getId());
        assertEquals(StatusComodato.SOLICITADO, response.getStatus());
        verify(inventarioIntegrationService).atualizarStatus(1L, "RESERVADO");
    }

    @Test
    void createQuandoEquipamentoIndisponivelNaoDevePersistir() {
        ComodatoRequestDTO dto = request(1L);
        when(inventarioIntegrationService.buscarEquipamento(1L)).thenReturn(
                new EquipamentoClientResponseDTO(1L, "EQ-001", "Cadeira", "Desc", "BOA", 1L, "EMPRESTADO"));

        assertThrows(BusinessException.class, () -> service.create(dto));
        verify(repository, never()).save(any());
        verify(inventarioIntegrationService, never()).atualizarStatus(eq(1L), eq("RESERVADO"));
    }

    @Test
    void createQuandoInventarioIndisponivelDevePropagarFallback() {
        ComodatoRequestDTO dto = request(1L);
        when(inventarioIntegrationService.buscarEquipamento(1L))
                .thenThrow(new ServiceUnavailableException("Serviço de inventário indisponível. Operação não realizada."));

        assertThrows(ServiceUnavailableException.class, () -> service.create(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void retirarDeveAtualizarStatusParaEmprestado() {
        Comodato comodato = comodato(10L, StatusComodato.SOLICITADO);
        when(repository.findById(10L)).thenReturn(Optional.of(comodato));
        when(repository.save(any(Comodato.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComodatoResponseDTO response = service.retirar(10L);

        assertEquals(StatusComodato.RETIRADO, response.getStatus());
        verify(inventarioIntegrationService).atualizarStatus(1L, "EMPRESTADO");
    }

    @Test
    void devolverQuandoStatusInvalidoDeveLancarExcecao() {
        when(repository.findById(10L)).thenReturn(Optional.of(comodato(10L, StatusComodato.SOLICITADO)));

        assertThrows(BusinessException.class, () -> service.devolver(10L));
    }

    @Test
    void cancelarQuandoInventarioFalhaNaoDeveAlterarLocal() {
        Comodato comodato = comodato(10L, StatusComodato.SOLICITADO);
        when(repository.findById(10L)).thenReturn(Optional.of(comodato));
        doThrow(new ServiceUnavailableException("indisponível"))
                .when(inventarioIntegrationService).atualizarStatus(1L, "DISPONIVEL");

        assertThrows(ServiceUnavailableException.class, () -> service.cancelar(10L));
        assertEquals(StatusComodato.SOLICITADO, comodato.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdQuandoNaoExisteDeveLancarExcecao() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    private ComodatoRequestDTO request(Long equipamentoId) {
        return new ComodatoRequestDTO(
                "Ana", "11122233344", equipamentoId,
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true);
    }

    private Comodato comodato(Long id, StatusComodato status) {
        return new Comodato(id, "Ana", "11122233344", 1L, status,
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true);
    }
}
