package br.com.fiap.ms.comodato.controller;

import br.com.fiap.ms.comodato.dto.request.ComodatoRequestDTO;
import br.com.fiap.ms.comodato.dto.response.ComodatoResponseDTO;
import br.com.fiap.ms.comodato.entity.StatusComodato;
import br.com.fiap.ms.comodato.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.comodato.exceptions.handler.GlobalExceptionHandler;
import br.com.fiap.ms.comodato.service.ComodatoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComodatoController.class)
@Import(GlobalExceptionHandler.class)
class ComodatoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ComodatoService service;

    @Test
    void findAllDeveRetornar200() throws Exception {
        when(service.findAll()).thenReturn(List.of(
                new ComodatoResponseDTO(1L, "Ana", "111", 1L, StatusComodato.SOLICITADO,
                        LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true)
        ));

        mockMvc.perform(get("/api/v1/comodatos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nomeSolicitante").value("Ana"));
    }

    @Test
    void findByIdQuandoNaoExisteDeveRetornar404() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Comodato não encontrado: 99"));

        mockMvc.perform(get("/api/v1/comodatos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDeveRetornar201ComLocation() throws Exception {
        ComodatoRequestDTO request = new ComodatoRequestDTO(
                "Ana", "11122233344", 1L,
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true);
        when(service.create(any(ComodatoRequestDTO.class))).thenReturn(
                new ComodatoResponseDTO(10L, "Ana", "11122233344", 1L, StatusComodato.SOLICITADO,
                        LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true)
        );

        mockMvc.perform(post("/api/v1/comodatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/comodatos/10"));
    }

    @Test
    void createComDadosInvalidosDeveRetornar422() throws Exception {
        ComodatoRequestDTO request = new ComodatoRequestDTO(
                "", "", null, null, null, false);

        mockMvc.perform(post("/api/v1/comodatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void retiradaDeveRetornar200() throws Exception {
        when(service.retirar(1L)).thenReturn(
                new ComodatoResponseDTO(1L, "Ana", "111", 1L, StatusComodato.RETIRADO,
                        LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true)
        );

        mockMvc.perform(patch("/api/v1/comodatos/1/retirada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRADO"));
    }
}
