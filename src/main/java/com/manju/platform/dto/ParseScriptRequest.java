package com.manju.platform.dto;
import lombok.Data;
//  拆解请求
@Data
public class ParseScriptRequest {
    private Integer userId;
    private String userScript;      // 剧本内容
}


