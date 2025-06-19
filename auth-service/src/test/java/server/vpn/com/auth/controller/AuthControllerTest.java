package server.vpn.com.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import server.vpn.com.auth.model.dto.RegisterRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {server.vpn.com.auth.AuthServiceApplication.class, TestConfig.class})
@AutoConfigureWebMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
})
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    void registerUser_ValidRequest_ShouldReturnToken() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        try {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(result -> {
                        System.out.println("Status: " + result.getResponse().getStatus());
                        System.out.println("Content: " + result.getResponse().getContentAsString());
                    })
                    .andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void registerUser_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 