package br.com.fiap.ms.comodato.controller;

import br.com.fiap.ms.comodato.client.InventarioIntegrationService;
import br.com.fiap.ms.comodato.client.dto.EquipamentoClientResponseDTO;
import br.com.fiap.ms.comodato.dto.request.ComodatoRequestDTO;
import br.com.fiap.ms.comodato.entity.StatusComodato;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComodatoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventarioIntegrationService inventarioIntegrationService;

    @Test
    void deveCriarRetirarEDevolverComodato() throws Exception {
        when(inventarioIntegrationService.buscarEquipamento(1L)).thenReturn(
                new EquipamentoClientResponseDTO(1L, "EQ-001", "Cadeira", "Desc", "BOA", 1L, "DISPONIVEL"));
        doNothing().when(inventarioIntegrationService).atualizarStatus(anyLong(), anyString());

        ComodatoRequestDTO request = new ComodatoRequestDTO(
                "Carlos", "55566677788", 1L,
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true);

        MvcResult created = mockMvc.perform(post("/api/v1/comodatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StatusComodato.SOLICITADO.name()))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/comodatos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeSolicitante").value("Carlos"));

        mockMvc.perform(patch("/api/v1/comodatos/" + id + "/retirada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StatusComodato.RETIRADO.name()));

        mockMvc.perform(patch("/api/v1/comodatos/" + id + "/devolucao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StatusComodato.DEVOLVIDO.name()));
    }

    @Test
    void createQuandoInventarioIndisponivelDeveRetornar503() throws Exception {
        when(inventarioIntegrationService.buscarEquipamento(2L))
                .thenThrow(new br.com.fiap.ms.comodato.exceptions.ServiceUnavailableException(
                        "Serviço de inventário indisponível. Operação não realizada."));

        ComodatoRequestDTO request = new ComodatoRequestDTO(
                "Carlos", "55566677788", 2L,
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 12, 15), true);

        mockMvc.perform(post("/api/v1/comodatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable());
    }
}
