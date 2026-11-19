package br.com.fiap.ms.inventario.controller;

import br.com.fiap.ms.inventario.dto.request.EquipamentoRequestDTO;
import br.com.fiap.ms.inventario.dto.request.EquipamentoStatusRequestDTO;
import br.com.fiap.ms.inventario.dto.response.EquipamentoResponseDTO;
import br.com.fiap.ms.inventario.entity.StatusEquipamento;
import br.com.fiap.ms.inventario.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.inventario.exceptions.handler.GlobalExceptionHandler;
import br.com.fiap.ms.inventario.service.EquipamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EquipamentoController.class)
@Import(GlobalExceptionHandler.class)
class EquipamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EquipamentoService service;

    @Test
    void findAllDeveRetornar200() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new EquipamentoResponseDTO(1L, "EQ-001", "Cadeira", "Desc", "BOA", 1L, StatusEquipamento.DISPONIVEL)
        ));

        mockMvc.perform(get("/api/v1/equipamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoTag").value("EQ-001"));
    }

    @Test
    void findByIdQuandoNaoExisteDeveRetornar404() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Equipamento não encontrado: 99"));

        mockMvc.perform(get("/api/v1/equipamentos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDeveRetornar201ComLocation() throws Exception {
        EquipamentoRequestDTO request = new EquipamentoRequestDTO(
                "EQ-010", "Muleta", "Par", "BOA", 1L, StatusEquipamento.DISPONIVEL);
        when(service.create(any(EquipamentoRequestDTO.class))).thenReturn(
                new EquipamentoResponseDTO(10L, "EQ-010", "Muleta", "Par", "BOA", 1L, StatusEquipamento.DISPONIVEL)
        );

        mockMvc.perform(post("/api/v1/equipamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/equipamentos/10"));
    }

    @Test
    void createComDadosInvalidosDeveRetornar422() throws Exception {
        EquipamentoRequestDTO request = new EquipamentoRequestDTO(
                "", "", "", "", null, null);

        mockMvc.perform(post("/api/v1/equipamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateStatusDeveRetornar200() throws Exception {
        EquipamentoStatusRequestDTO request = new EquipamentoStatusRequestDTO(StatusEquipamento.RESERVADO);
        when(service.updateStatus(eq(1L), any(EquipamentoStatusRequestDTO.class))).thenReturn(
                new EquipamentoResponseDTO(1L, "EQ-001", "Cadeira", "Desc", "BOA", 1L, StatusEquipamento.RESERVADO)
        );

        mockMvc.perform(patch("/api/v1/equipamentos/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVADO"));
    }

    @Test
    void deleteDeveRetornar204() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/v1/equipamentos/1"))
                .andExpect(status().isNoContent());
    }
}
