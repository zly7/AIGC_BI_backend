package com.yupi.springbootinit.model.dto.chart;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ChatGPTRunsRequest {
    @SerializedName(value="assistant_id")
    private String assistantId;
}