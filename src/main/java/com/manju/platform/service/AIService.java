package com.manju.platform.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.manju.platform.dto.ChatRequest;
import com.manju.platform.dto.Message;
import com.manju.platform.dto.Thinking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// 导入这个注解后，可以在 Spring 管理的类（如@Controller、@Service、@Component等）中，
// 通过它读取配置文件中的属性值，并赋值给类的成员变量。
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
// RestTemplate是 Spring 提供的同步阻塞式 HTTP 客户端，简化 RESTful API 调用；
// 核心优势是封装 HTTP 底层细节，支持直接解析响应为 Java 对象；

import java.util.Arrays;
import java.util.List;

@Service
public class AIService {

    @Value("${ai.api.url}")
    private String apiUrl;
    @Value("${ai.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;  // 直接注入配置好的 RestTemplate

    @Autowired
    private ObjectMapper objectMapper; // Jackson 的 JSON 处理器

    /**
     * 调用 AI 生成文本
     *
     * @param prompt 用户输入的提示词
     * @return AI 返回的文本内容
     */
    public String generateText(String prompt) {
//        // 测试用：如果 prompt 包含 "#失败测试"，则模拟超时
//        if (prompt.contains("#失败测试")){
//            throw new RuntimeException("模拟AI调用超时");
//        }
        // 1. 构建消息列表
        // 将长指令和用户输入合并成一条 user 消息（避免两个连续 user）
        String systemInstruction = "作为一名专业的互联网短剧编剧，你掌握着短剧的通用结构：" +
                "开头交代看点，主角遇到危机或矛盾，觉醒金手指或侧面交代已有能力，给到信息并确立主线，" +
                "然后主角利用能力的信息差变强打脸，集中主线去安排事件，通过事件来推进剧情，情绪连贯，台词不水。\n" +
                "开局安排目标，完成目标过程有各种反派捣乱，有的杀了，有的暂时杀不了，" +
                "杀不了的就搞事然后面对危机解决打脸回去，或者由于后续有某个原因暂时不撕破脸，" +
                "中间再加个大佬帮主角让主角不至于被反派干死还能让主角占优势。\n" +
                "任务1:你需要根据用户提供的描述来构思短剧剧本，每集1-3分钟，共60-80集。\n" +
                "任务2:第一次给用户输出时，你仅需要输出一句话故事，描述故事梗概并询问用户是否接受这个故事，如果用户接受则进行任务3；" +
                "如果用户不接受则重新输出故事直到用户接受再进行任务3。\n" +
                "任务3:为用户输出完整的短剧大纲。如果用户认可则进行任务4；若用户不认可则修改大纲直到用户认可再进行任务4。\n" +
                "任务4:为用户输出第一集剧本。如果用户认可则进行任务5；如果用户不认可则修改直到用户认可再进行任务5。\n" +
                "任务5:为用户输出第二集剧本。若用户认可则输出下一集剧本，若用户不认可则修改至用户认可再输出下一集剧本。\n" +
                "之后的任务就是每次输出后面一集的单集剧本，若用户认可则输出下一集，若用户不认可则修改至认可再输出下一集。\n" +
                "注意每次生成剧本时，要回顾短剧通用结构与之前输出的任务1、2、3，确保输出的剧本不偏离最初的设定。\n\n" +
                "用户需求：" + prompt;

        Message userMessage = new Message("user", systemInstruction);
        List<Message> messages = Arrays.asList(userMessage);

        // 2. 构建 thinking 对象
        Thinking thinking = new Thinking("enable");

        // 3. 构建请求对象
        ChatRequest request = new ChatRequest(
                "glm-4.7-flash",
                messages,
                thinking,
                1024,
                1.0
        );

        // 4. 将请求对象序列化为 JSON 字符串
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("构建请求 JSON 失败", e);
        }

        // 5. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<String> httpEntity = new HttpEntity<>(requestBody, headers);

        // 6. 发送请求
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            // 7. 解析响应，提取 AI 生成的文本
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choice = root.path("choices").get(0);
            if (choice == null) {
                throw new RuntimeException("响应中缺少 choices 字段");
            }
            String content = choice.path("message").path("content").asText();
            return content;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("AI服务调用失败：" + e.getMessage(), e);
        }
    }
}
