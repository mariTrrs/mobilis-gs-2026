package br.com.fiap.ms.inventario.service;

import br.com.fiap.ms.inventario.dto.request.EquipamentoRequestDTO;
import br.com.fiap.ms.inventario.dto.request.EquipamentoStatusRequestDTO;
import br.com.fiap.ms.inventario.dto.response.EquipamentoResponseDTO;
import br.com.fiap.ms.inventario.entity.Equipamento;
import br.com.fiap.ms.inventario.entity.StatusEquipamento;
import br.com.fiap.ms.inventario.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.inventario.repository.EquipamentoRepository;
import br.com.fiap.ms.inventario.repository.InstituicaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipamentoServiceTest {

    @Mock
    private EquipamentoRepository repository;

    @Mock
    private InstituicaoRepository instituicaoRepository;

    @InjectMocks
    private EquipamentoService service;

    @Test
    void findAllDeveRetornarLista() {
        Equipamento equipamento = equipamento(1L, StatusEquipamento.DISPONIVEL);
        when(repository.findAll()).thenReturn(List.of(equipamento));

        List<EquipamentoResponseDTO> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("EQ-001", result.getFirst().getCodigoTag());
    }

    @Test
    void findByIdQuandoNaoExisteDeveLancarExcecao() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void createDevePersistirQuandoInstituicaoExiste() {
        EquipamentoRequestDTO dto = new EquipamentoRequestDTO(
                "EQ-010", "Muleta", "Par de muletas", "BOA", 1L, StatusEquipamento.DISPONIVEL);
        when(instituicaoRepository.existsById(1L)).thenReturn(true);
        when(repository.save(any(Equipamento.class))).thenAnswer(invocation -> {
            Equipamento entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        EquipamentoResponseDTO response = service.create(dto);

        assertEquals(10L, response.getId());
        assertEquals(StatusEquipamento.DISPONIVEL, response.getStatus());
    }

    @Test
    void updateStatusDeveAlterarStatus() {
        Equipamento equipamento = equipamento(1L, StatusEquipamento.DISPONIVEL);
        when(repository.findById(1L)).thenReturn(Optional.of(equipamento));
        when(repository.save(any(Equipamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EquipamentoResponseDTO response = service.updateStatus(
                1L, new EquipamentoStatusRequestDTO(StatusEquipamento.RESERVADO));

        assertEquals(StatusEquipamento.RESERVADO, response.getStatus());
        verify(repository).save(equipamento);
    }

    @Test
    void deleteQuandoNaoExisteDeveLancarExcecao() {
        when(repository.existsById(5L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));
    }

    private Equipamento equipamento(Long id, StatusEquipamento status) {
        return new Equipamento(id, "EQ-001", "Cadeira", "Cadeira de rodas", "BOA", 1L, status);
    }
}
