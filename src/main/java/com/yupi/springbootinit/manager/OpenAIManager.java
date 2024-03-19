package com.yupi.springbootinit.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.springbootinit.model.dto.chat.ChatRequest;
import com.yupi.springbootinit.utils.GetAIResponseUtils;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAIManager {

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("http://localhost:5000/process_prompt")
    private String lcApiUrl;
    public String doChat(String modelId, String messagePrompt){
        ChatRequest request = new ChatRequest(modelId, messagePrompt);

        // Convert ChatRequest to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonRequest;
        try {
            jsonRequest = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // Handle JSON serialization error
            return "Error serializing ChatRequest to JSON";
        }

        // call the API with JSON request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);
        String response;
        try {
            response = this.restTemplate.postForObject(apiUrl, entity, String.class);
            response = GetAIResponseUtils.getResponse(response);
        } catch (RestClientException e) {
            // Handle RestClientException
            if (e instanceof HttpStatusCodeException) {
                HttpStatusCodeException ex = (HttpStatusCodeException) e;
                response = ex.getResponseBodyAsString();
            } else {
                response = "Error calling the API";
            }
            return response;
        }

        if (response == null || response.isEmpty()) {
            return "No response";
        }

        return response;
    }
    public String doLcChat(String modelId, String messagePrompt) {
        // Prepare the request with the model ID and message prompt
        // 使用HashMap构建请求体以便转换成JSON格式
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model_id", modelId);
        requestBody.put("prompt", messagePrompt);

        // Convert the requestBody to JSON
        ObjectMapper objectSendMapper = new ObjectMapper();
        String jsonRequest;
        try {
            jsonRequest = objectSendMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            return "Error serializing request to JSON: " + e.getMessage();
        }

        // Setup headers to indicate we are sending and expecting JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(jsonRequest, headers);

        // Attempt to call the API with the JSON request
        String response;
        try {
            String jsonResponse = this.restTemplate.postForObject(lcApiUrl, entity, String.class);
            ObjectMapper objectGetMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectGetMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>(){});
            response = (String) responseMap.get("response");
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException) {
                HttpStatusCodeException ex = (HttpStatusCodeException) e;
                response = "API Error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString();
            } else {
                response = "Error calling the API: " + e.getMessage();
            }
        }catch (JsonProcessingException e) {
            // Handle JsonProcessingException when parsing the JSON response
            response = "Error parsing JSON response: " + e.getMessage();
        }

        return response != null ? response : "No response received";
    }
}

