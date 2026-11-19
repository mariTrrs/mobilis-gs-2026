package br.com.fiap.ms.inventario.controller;

import br.com.fiap.ms.inventario.dto.request.EquipamentoRequestDTO;
import br.com.fiap.ms.inventario.dto.request.EquipamentoStatusRequestDTO;
import br.com.fiap.ms.inventario.dto.request.InstituicaoRequestDTO;
import br.com.fiap.ms.inventario.entity.StatusEquipamento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EquipamentoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long instituicaoId;

    @BeforeEach
    void setUp() throws Exception {
        InstituicaoRequestDTO instituicao = new InstituicaoRequestDTO(
                "ONG Teste", "00.000.000/0001-00", "Rua A, 1", "Centro", "Sao Paulo",
                "11999999999", -23.5, -46.6);
        MvcResult result = mockMvc.perform(post("/api/v1/instituicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(instituicao)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        instituicaoId = body.get("id").asLong();
    }

    @Test
    void deveCriarConsultarEAtualizarStatus() throws Exception {
        EquipamentoRequestDTO request = new EquipamentoRequestDTO(
                "EQ-IT-001", "Andador", "Andador teste", "BOA", instituicaoId, StatusEquipamento.DISPONIVEL);

        MvcResult created = mockMvc.perform(post("/api/v1/equipamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/equipamentos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoTag").value("EQ-IT-001"));

        EquipamentoStatusRequestDTO statusRequest = new EquipamentoStatusRequestDTO(StatusEquipamento.RESERVADO);
        mockMvc.perform(patch("/api/v1/equipamentos/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVADO"));
    }
}
