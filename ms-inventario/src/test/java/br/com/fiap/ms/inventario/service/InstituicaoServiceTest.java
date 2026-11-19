package br.com.fiap.ms.inventario.service;

import br.com.fiap.ms.inventario.dto.request.InstituicaoRequestDTO;
import br.com.fiap.ms.inventario.dto.response.InstituicaoResponseDTO;
import br.com.fiap.ms.inventario.entity.Instituicao;
import br.com.fiap.ms.inventario.exceptions.ResourceNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstituicaoServiceTest {

    @Mock
    private InstituicaoRepository repository;

    @InjectMocks
    private InstituicaoService service;

    @Test
    void findAllDeveRetornarLista() {
        when(repository.findAll()).thenReturn(List.of(
                new Instituicao(1L, "ONG", "12.345.678/0001-90", "Rua A", "Centro",
                        "Sao Paulo", "11999999999", -23.5, -46.6)
        ));

        List<InstituicaoResponseDTO> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("ONG", result.getFirst().getNome());
    }

    @Test
    void createDevePersistir() {
        InstituicaoRequestDTO dto = new InstituicaoRequestDTO(
                "ONG", "12.345.678/0001-90", "Rua A", "Centro", "Sao Paulo",
                "11999999999", -23.5, -46.6);
        when(repository.save(any(Instituicao.class))).thenAnswer(invocation -> {
            Instituicao entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        InstituicaoResponseDTO response = service.create(dto);

        assertEquals(1L, response.getId());
        assertEquals("ONG", response.getNome());
    }

    @Test
    void findByIdQuandoNaoExisteDeveLancarExcecao() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }
}
