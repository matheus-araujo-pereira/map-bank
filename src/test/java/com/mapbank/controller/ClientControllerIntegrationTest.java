package com.mapbank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapbank.domain.enums.DocumentType;
import com.mapbank.dto.request.CreateClientRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Integração: Deve cadastrar e consultar cliente titular com sucesso")
    void testCreateAndGetClientEndpoint() throws Exception {
        CreateClientRequest request = new CreateClientRequest(
                "Matheus Araújo Pereira",
                "11122233344",
                DocumentType.PF,
                "matheus.integration@mapbank.com",
                "+5511999990000"
        );

        String responseJson = mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Matheus Araújo Pereira")))
                .andExpect(jsonPath("$.document", is("11122233344")))
                .andReturn().getResponse().getContentAsString();

        // Consulta pelo ID retornado
        Integer clientId = com.jayway.jsonpath.JsonPath.read(responseJson, "$.id");

        mockMvc.perform(get("/api/v1/clients/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("matheus.integration@mapbank.com")));
    }
}
