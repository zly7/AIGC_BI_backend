package com.yupi.springbootinit.manager;

import com.esotericsoftware.kryo.io.Input;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.model.dto.chart.ChatGPTRunsRequest;
import com.yupi.springbootinit.model.dto.chart.CreateAIMessageRequest;
import com.yupi.springbootinit.model.dto.chat.CreateAIAssistantRequest;
import com.yupi.springbootinit.model.dto.chat.CreateAIAssistantResponse;
import com.yupi.springbootinit.utils.GetAIResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.theokanning.openai.service.OpenAiService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@Service
@Slf4j
public class AIAssistantManager {


    @Value("${openai.api.key}")
    private String CHATGPT_KEY;

    @Value("${openai.assistant.url}")
    private String CHATGPT_ASSISTANTS_URL;

    @Value("${openai.thread.url}")
    private String CHATGPT_THREADS_URL;

    @Value("${openai.file.url}")
    private String CHATGPT_FILE_URL;

    @Value("http://localhost:5000/")
    private String flask_url;

    @Qualifier("openaiRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    public String[] uploadFile(MultipartFile multipartFile) throws IOException {
        // call the API with JSON request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(MediaType.MULTIPART_FORM_DATA_VALUE));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        try{

            body.add("purpose", "assistants");
            body.add("file",  multipartFile.getResource());
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }


        HttpEntity<MultiValueMap> entity = new HttpEntity<>(body, headers);
        HttpEntity<FileSystemResource> requestEntity = new HttpEntity<>(new FileSystemResource(String.valueOf(multipartFile)));
        String response;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
//            ResponseEntity<String> responseEntity = restTemplate.exchange(CHATGPT_FILE_URL, HttpMethod.POST, entity, String.class);
            response = this.restTemplate.postForObject(CHATGPT_FILE_URL, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            // Extract the value of the "id" field

            // Return the extracted id
            String [] stringArr = new String[1];
            stringArr[0] = jsonNode.get("id").asText();
            return stringArr;

        } catch (RestClientException e) {
            // Handle RestClientException
            if (e instanceof HttpStatusCodeException) {
                HttpStatusCodeException ex = (HttpStatusCodeException) e;
                response = ex.getResponseBodyAsString();
            } else {
                response = "Error calling the API";
            }
            return null;
        }
    }

    public String createAssistant(String name, String model, String instructions, MultipartFile multipartFile) throws IOException {

        log.info("createAssistant started. name: {} instructions: {} model: {}", name, instructions, model);


        CreateAIAssistantRequest chatGPTCreateAssistantRequest = new CreateAIAssistantRequest();
        chatGPTCreateAssistantRequest.setModel(model);
        chatGPTCreateAssistantRequest.setName(name);
        chatGPTCreateAssistantRequest.setInstructions(instructions);

        // 调用OpenAI 传文件API
        String[] file_id = uploadFile(multipartFile);
        if(file_id!=null){
            chatGPTCreateAssistantRequest.setFile_ids(file_id);

        }else {
            throw new IOException();
        }

        log.info("createAssistant started. url: {}", CHATGPT_ASSISTANTS_URL);

        HttpPost post = new HttpPost(CHATGPT_ASSISTANTS_URL);
        post.addHeader("Content-Type", "application/json");
        post.addHeader("Authorization", "Bearer " + CHATGPT_KEY);
        post.addHeader("OpenAI-Beta", "assistants=v1");

        Gson gson = new Gson();

        String body = gson.toJson(chatGPTCreateAssistantRequest);

        log.info("createAssistant post body: {}", body);

        try {
            final StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
            post.setEntity(entity);

            try (CloseableHttpClient httpClient = HttpClients.custom().build();
                 CloseableHttpResponse response = httpClient.execute(post)) {

                String responseBody = EntityUtils.toString(response.getEntity());


                log.info("createAssistant got a status: {}", response.getStatusLine());
                log.info("createAssistant got a responseBody: {}", responseBody);

                CreateAIAssistantResponse chatGPTCreateAssistantResponse = gson.fromJson(responseBody, CreateAIAssistantResponse.class);

                log.info("createAssistant got a response: {}", chatGPTCreateAssistantResponse);
                log.info("createAssistant got id: {}", chatGPTCreateAssistantResponse.getId());

                Map<String, Object> map = new HashMap<>();

                map.put("status", "Success");
                map.put("assistantId", chatGPTCreateAssistantResponse.getId());

                //返回Assistant ID
                return chatGPTCreateAssistantResponse.getId();



            } catch (Exception e) {
                log.info("createAssistant exception A e: {}", e.getMessage());
                throw new IOException();

            }
        }
        catch (Exception e) {
            log.info("createAssistant exception B e: {}", e.getMessage());

            throw new IOException();
        }


    }

    public String createThread() {

        log.info("createThread started.");

        log.info("createThread started. url: {}", CHATGPT_THREADS_URL);

        HttpPost post = new HttpPost(CHATGPT_THREADS_URL);
        post.addHeader("Content-Type", "application/json");
        post.addHeader("Authorization", "Bearer " + CHATGPT_KEY);
        post.addHeader("OpenAI-Beta", "assistants=v1");

        Gson gson = new Gson();

        String body = "";

        log.info("createThread post body: {}", body);

        try {
            final StringEntity entity = new StringEntity(body);
            post.setEntity(entity);

            try (CloseableHttpClient httpClient = HttpClients.custom().build();
                 CloseableHttpResponse response = httpClient.execute(post)) {

                String responseBody = EntityUtils.toString(response.getEntity());


                log.info("createThread got a status: {}", response.getStatusLine());
                log.info("createThread got a responseBody: {}", responseBody);

                CreateAIAssistantResponse chatGPTCreateThreadResponse = gson.fromJson(responseBody, CreateAIAssistantResponse.class);

                log.info("createThread got a response: {}", chatGPTCreateThreadResponse);
                log.info("createThread got id: {}", chatGPTCreateThreadResponse.getId());

                Map<String, Object> map = new HashMap<>();

                map.put("status", "Success");
                map.put("threadId", chatGPTCreateThreadResponse.getId());

                return chatGPTCreateThreadResponse.getId();



            } catch (Exception e) {
                log.info("createThread exception A e: {}", e.getMessage());

                Map<String, Object> map = new HashMap<>();

                map.put("status", "Failed");
                map.put("content", e.getMessage());

                return null;
            }
        }
        catch (Exception e) {
            log.info("createThread exception B e: {}", e.getMessage());

            Map<String, Object> map = new HashMap<>();

            map.put("status", "Failed");
            map.put("content", e.getMessage());

            return null;
        }


    }

    public String sendMessageToThread(String message, String assistantId, String threadId) {


        log.info("sendMessageToThread started. message: {} assistantId: {} threadId: {}", message, assistantId, threadId);

        String url = CHATGPT_THREADS_URL + "/" + threadId + "/messages";

        log.info("sendMessageToThread started. url: {}", url);

        CreateAIMessageRequest chatGPTCreateMessageRequest = new CreateAIMessageRequest();

        // Note: Only user role is currently supported
        chatGPTCreateMessageRequest.setRole("user");
        chatGPTCreateMessageRequest.setContent(message);


        HttpPost post = new HttpPost(url);
        post.addHeader("Content-Type", "application/json");
        post.addHeader("Authorization", "Bearer " + CHATGPT_KEY);
        post.addHeader("OpenAI-Beta", "assistants=v1");

        Gson gson = new Gson();

        String body = gson.toJson(chatGPTCreateMessageRequest);

        log.info("sendMessageToThread post body: {}", body);

        try {
            final StringEntity entity = new StringEntity(body);
            post.setEntity(entity);

            try (CloseableHttpClient httpClient = HttpClients.custom().build();
                 CloseableHttpResponse response = httpClient.execute(post)) {

                String responseBody = EntityUtils.toString(response.getEntity());

                Map<String, Object> messageResponse = gson.fromJson(responseBody, Map.class);

                String messageId = (String)messageResponse.get("id");

                log.info("sendMessageToThread got a status: {}", response.getStatusLine());
                log.info("sendMessageToThread got a responseBody: {}", responseBody);

                String url2 = CHATGPT_THREADS_URL + "/" + threadId + "/runs";

                log.info("sendMessageToThread run. url: {}", url2);


                HttpPost runPost = new HttpPost(url2);
                runPost.addHeader("Content-Type", "application/json");
                runPost.addHeader("Authorization", "Bearer " + CHATGPT_KEY);
                runPost.addHeader("OpenAI-Beta", "assistants=v1");

                ChatGPTRunsRequest chatGPTRunsRequest = new ChatGPTRunsRequest();
                chatGPTRunsRequest.setAssistantId(assistantId);

                String body2 = gson.toJson(chatGPTRunsRequest);

                final StringEntity entity2 = new StringEntity(body2);
                runPost.setEntity(entity2);

                log.info("sendMessageToThread post run body2: {}", body2);


                // Retrieve run status for thread
                try (CloseableHttpClient httpClient2 = HttpClients.custom().build();
                     CloseableHttpResponse response2 = httpClient.execute(runPost)) {

                    String responseBody2 = EntityUtils.toString(response2.getEntity());


                    log.info("sendMessageToThread got a status2: {}", response2.getStatusLine());
                    log.info("sendMessageToThread got a responseBody2: {}", responseBody2);

                    Map<String, Object> messageResponse2 = gson.fromJson(responseBody2, Map.class);

                    String runId = (String)messageResponse2.get("id");

                    String url3 = CHATGPT_THREADS_URL + "/" + threadId + "/runs/" + runId;

                    log.info("sendMessageToThread Poll runs. url: {}", url3);

                    HttpGet runPollGet = new HttpGet(url2);
                    runPollGet.addHeader("Authorization", "Bearer " + CHATGPT_KEY);
                    runPollGet.addHeader("OpenAI-Beta", "assistants=v1");

                    boolean completed = false;
                    int loopCount = 0;

                    // Poll run status for thread using runId
                    do {
                        log.info("sendMessageToThread Start of polling..........");

                        loopCount++;

                        try (CloseableHttpClient httpClient3 = HttpClients.custom().build();
                             CloseableHttpResponse response3 = httpClient.execute(runPollGet)) {

                            String responseBody3 = EntityUtils.toString(response3.getEntity());

                            log.info("sendMessageToThread got a status3: {}", response3.getStatusLine());
                            log.info("sendMessageToThread got a responseBody3: {}", responseBody3);

                            Map<String, Object> runPollResponse = gson.fromJson(responseBody3, Map.class);

                            List<Object> dataItems = (List<Object>)runPollResponse.get("data");

                            if (dataItems != null && dataItems.size() > 0) {
                                Map<String, Object> item0 = (Map<String, Object>) dataItems.get(0);

                                String runStatus = (String) item0.get("status");

                                if (runStatus.equals("completed")) {
                                    completed = true;
                                    log.info("sendMessageToThread run completed..........Get Last Message");
                                    break;
                                }

                            }


                            if (loopCount > 15) {
                                break;
                            }

                            // Wait if not ready
                            try {
                                log.info("sendMessageToThread not ready, wait 1 second.....");

                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e) {

                            }


                        }
                        catch(Exception e) {

                        }


                    }
                    while (true);

                    log.info("We can go get the messages line");

                    if (completed) {

                        String lastMessage = getLastAssistantMessage(threadId);

                        return lastMessage;

                    }
                    else {
                        Map<String, Object> map = new HashMap<>();

                        map.put("status", "Failed to get Messages");

                        return "Failed to get Messages";

                    }



                } catch (Exception e) {
                    log.info("sendMessageToThread exception A e: {}", e.getMessage());

                    return e.getMessage();
                }


//                ChatGPTCreateThreadResponse chatGPTCreateThreadResponse = gson.fromJson(responseBody, ChatGPTCreateThreadResponse.class);
//
//                log.info("sendMessageToThread got a response: {}", chatGPTCreateThreadResponse);
//                log.info("sendMessageToThread got id: {}", chatGPTCreateThreadResponse.getId());
//
//                Map<String, Object> map = new HashMap<>();
//
//                map.put("status", "Success");
//                map.put("threadId", chatGPTCreateThreadResponse.getId());
//
//                return map;



            } catch (Exception e) {
                log.info("sendMessageToThread exception A e: {}", e.getMessage());


                return e.getMessage();
            }
        }
        catch (Exception e) {
            log.info("sendMessageToThread exception B e: {}", e.getMessage());


            return  e.getMessage();
        }


    }

    public String getLastAssistantMessage(String threadId) {

        log.info("getLastAssistantMessage started. threadId: {}", threadId);


        String url = CHATGPT_THREADS_URL + "/" + threadId + "/messages";

        log.info("getLastAssistantMessage started. url: {}", url);


        HttpGet get = new HttpGet(url);
        get.addHeader("Authorization", "Bearer " + CHATGPT_KEY);
        get.addHeader("OpenAI-Beta", "assistants=v1");

        Gson gson = new Gson();


        try {

            try (CloseableHttpClient httpClient = HttpClients.custom().build();
                 CloseableHttpResponse response = httpClient.execute(get)) {

                String responseBody = EntityUtils.toString(response.getEntity());


                log.info("getLastAssistantMessage got a status: {}", response.getStatusLine());
                log.info("getLastAssistantMessage got a responseBody: {}", responseBody);

                Map<String, Object> resp = gson.fromJson(responseBody, Map.class);

                List<Object> data = (List<Object>)resp.get("data");

                log.info("getLastAssistantMessage got data");

                Object firstMessageObject = data.get(0);

                log.info("getLastAssistantMessage firstMessageObject {}", firstMessageObject);

                Map<String, Object> firstMessage = (Map<String, Object>)firstMessageObject;

                List<Object> contents = (List<Object>)firstMessage.get("content");

                log.info("getLastAssistantMessage contents {}", contents);


                Map<String, Object> content = (Map<String, Object>)contents.get(0);

                Map<String, Object> textElement = (Map<String, Object>)content.get("text");


                log.info("getLastAssistantMessage textElement {}", textElement);

                String value = (String) textElement.get("value");

                Map<String, Object> map = new HashMap<>();

                map.put("status", "Success");
                map.put("value", value);

                log.info("getLastAssistantMessage value {}", value);

                return value;



            } catch (Exception e) {
                log.info("getLastAssistantMessage exception A e: {}", e.getMessage());

                e.printStackTrace();
                Map<String, Object> map = new HashMap<>();

                map.put("status", "Failed");
                map.put("content", e.getMessage());

                return null;
            }
        }
        catch (Exception e) {
            log.info("getLastAssistantMessage exception B e: {}", e.getMessage());

            Map<String, Object> map = new HashMap<>();

            map.put("status", "Failed");
            map.put("content", e.getMessage());

            return null;
        }


    }

    public Map<String, Object> getMessages(String threadId) {

        log.info("getMessages started. threadId: {}", threadId);


        String url = CHATGPT_THREADS_URL + "/" + threadId + "/messages";

        log.info("getMessages started. url: {}", url);


        HttpGet get = new HttpGet(url);
        get.addHeader("Authorization", "Bearer " + CHATGPT_KEY);
        get.addHeader("OpenAI-Beta", "assistants=v1");

        Gson gson = new Gson();


        try {

            try (CloseableHttpClient httpClient = HttpClients.custom().build();
                 CloseableHttpResponse response = httpClient.execute(get)) {

                String responseBody = EntityUtils.toString(response.getEntity());


                log.info("getMessages got a status: {}", response.getStatusLine());
                log.info("getMessages got a responseBody: {}", responseBody);

                Map<String, Object> resp = gson.fromJson(responseBody, Map.class);

                List<Object> messages = new ArrayList<>();

                // Get Data
                List<Object> dataList = (List<Object>)resp.get("data");

                for (Object obj : dataList) {

                    Map<String, Object> item = (Map<String, Object>)obj;

                    Map<String, Object> msg = new HashMap<>();

                    msg.put("created_at", item.get("created_at"));
                    msg.put("thread_id", item.get("thread_id"));
                    msg.put("role", item.get("role"));

                    List<Object> contents = (List<Object>)item.get("content");
                    Map<String, Object> content = (Map<String, Object>)contents.get(0);
                    Map<String, Object> textElement = (Map<String, Object>)content.get("text");
                    String value = (String) textElement.get("value");

                    msg.put("value", value);


                    messages.add(msg);
                }


                Map<String, Object> map = new HashMap<>();

                map.put("status", "Success");
                map.put("messages", messages);

                return map;



            } catch (Exception e) {
                log.info("getMessages exception A e: {}", e.getMessage());

                Map<String, Object> map = new HashMap<>();

                map.put("status", "Failed");
                map.put("content", e.getMessage());

                return map;
            }
        }
        catch (Exception e) {
            log.info("getMessages exception B e: {}", e.getMessage());

            Map<String, Object> map = new HashMap<>();

            map.put("status", "Failed");
            map.put("content", e.getMessage());

            return map;
        }


    }


}