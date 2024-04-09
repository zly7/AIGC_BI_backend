package com.yupi.springbootinit.model.dto.chat;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CreateAIAssistantRequest {
    private String name;
    private String instructions;

    //默认type 就是code intepreter
    private List<Map<String, String>> tools = new ArrayList<>(List.of(Map.of("type", "code_interpreter")));

    private String model;

    //需要使用OpenAI 文件
    private String[] file_ids;
}
