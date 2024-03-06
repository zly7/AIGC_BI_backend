package com.yupi.springbootinit.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class GetAIResponseUtils {

    public static String getResponse(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonResponse = objectMapper.readTree(response); // Parse the response string into a JSON object
            JsonNode choicesNode = jsonResponse.get("choices"); // Get the "choices" array
            if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
                // Access the first choice object directly
                JsonNode firstChoiceNode = choicesNode.get(0);
                // Access the message of the first choice
                String message = firstChoiceNode.get("message").get("content").asText();
                return message;
            } else {
                // Handle case where "choices" array is empty or not present in the response
                return "No choices found in the response";
            }
        } catch (IOException e) {
            // Handle JSON parsing error
            e.printStackTrace();
            return "Error processing the response";
        }
    }
}
