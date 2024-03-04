package com.yupi.springbootinit.manager;

import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
@Service
public class AiManager {

    @Resource
    private YuCongMingClient client;
    public String doChat(long modelId, String messagePrompt){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(messagePrompt);
        BaseResponse<DevChatResponse> response = client.doChat(devChatRequest);
//        System.out.println(response.getData());
        return response.getData().getContent();
    }
}
