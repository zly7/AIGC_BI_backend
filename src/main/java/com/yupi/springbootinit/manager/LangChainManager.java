package com.yupi.springbootinit.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.springbootinit.model.dto.chart.GiveLangChainManagerDataPackage;
import com.fasterxml.jackson.core.type.TypeReference;

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
import java.util.Map;

@Service
public class LangChainManager {
    @Autowired
    private RestTemplate restTemplate;

    @Value("http://localhost:5000/all_llm_process")
    private String lcApiUrl;
    public String doLcChat(GiveLangChainManagerDataPackage giveLangChainManagerDataPackage){

        // Convert the requestBody to JSON
        ObjectMapper objectSendMapper = new ObjectMapper();
        String jsonRequest;
        try {
            jsonRequest = objectSendMapper.writeValueAsString(giveLangChainManagerDataPackage);
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

