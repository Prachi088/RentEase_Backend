package com.rentease.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GroqAiServiceTest {

    private GroqAiService groqAiService;

    @BeforeEach
    void setUp() {
        groqAiService = new GroqAiService();
        // Set API key to empty to test reliable production fallback behavior
        ReflectionTestUtils.setField(groqAiService, "groqApiKey", "");
        ReflectionTestUtils.setField(groqAiService, "groqModel", "llama-3.3-70b-versatile");
        ReflectionTestUtils.setField(groqAiService, "groqBaseUrl", "https://api.groq.com/openai/v1");
    }

    @Test
    @DisplayName("Should return intelligent deterministic assistance if API key is not configured")
    void testGenerateAssistantReply_FallbackWhenKeyMissing() {
        String query = "How does your security deposit escrow system protect renters?";
        String response = groqAiService.generateAssistantReply(query, null);

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        assertThat(response).contains("escrow");
    }

    @Test
    @DisplayName("Should return dispute advice fallback when requested")
    void testAnalyzeDispute_Fallback() {
        String analysis = groqAiService.analyzeDispute("Item was delivered with scratches not in handover report");

        assertThat(analysis).isNotNull();
        assertThat(analysis).contains("Handover");
    }
}
