package com.manju.platform.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// 导入这个注解后，可以在 Spring 管理的类（如@Controller、@Service、@Component等）中，
// 通过它读取配置文件中的属性值，并赋值给类的成员变量。
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
// RestTemplate是 Spring 提供的同步阻塞式 HTTP 客户端，简化 RESTful API 调用；
// 核心优势是封装 HTTP 底层细节，支持直接解析响应为 Java 对象；

import java.util.*;

@Service
public class AIService {

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;
    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${aliyun.api.url}")
    private String aliyunMultiModalUrl;
    @Value("${aliyun.api.key}")
    private String aliyunApiKey;

    @Value("${video.api.url}")
    private String videoApiUrl;
    @Value("${video.api.key}")
    private String videoApiKey;
    @Value("${video.result.url}")
    private String videoResultUrl;

    @Autowired
    private RestTemplate restTemplate;  // 直接注入配置好的 RestTemplate

    @Autowired
    private ObjectMapper objectMapper; // Jackson 的 JSON 处理器

    /**
     * 测试模式开关（仅用于并发测试）
     * testMode = true 时，AI方法返回固定内容，不调用外部API
     */
    private static boolean testMode = false;
    public static void enableTestMode() {
        testMode = true;
    }
    public static void disableTestMode() {
        testMode = false;
    }

    /**
     * 剧本生成，调用文生文
     * @param messages 前端传来的历史消息（不含系统指令）
     * @return AI 返回的文本内容
     */
    private final String SYSTEM_INSTRUCTION = """
        作为一名专业的互联网短剧编剧，你掌握着短剧的通用结构：
        开头交代看点，主角遇到危机或矛盾，觉醒金手指或侧面交代已有能力，给到信息并确立主线，
        然后主角利用能力的信息差变强打脸，集中主线去安排事件，通过事件来推进剧情，情绪连贯，台词不水。
        开局安排目标，完成目标过程有各种反派捣乱，有的杀了，有的暂时杀不了，
        杀不了的就搞事然后面对危机解决打脸回去，或者由于后续有某个原因暂时不撕破脸，
        中间再加个大佬帮主角让主角不至于被反派干死还能让主角占优势。
        任务1:你需要根据用户提供的描述来构思短剧剧本，每集1-3分钟，共60-80集。
        任务2:第一次给用户输出时，你仅需要输出一句话故事，描述故事梗概并询问用户是否接受这个故事，如果用户接受则进行任务3；
        如果用户不接受则重新输出故事直到用户接受再进行任务3。
        任务3:为用户输出完整的短剧大纲。如果用户认可则进行任务4；若用户不认可则修改大纲直到用户认可再进行任务4。
        任务4:为用户输出第一集剧本。如果用户认可则进行任务5；如果用户不认可则修改直到用户认可再进行任务5。
        任务5:为用户输出第二集剧本。若用户认可则输出下一集剧本，若用户不认可则修改至用户认可再输出下一集剧本。
        之后的任务就是每次输出后面一集的单集剧本，若用户认可则输出下一集，若用户不认可则修改至认可再输出下一集。
        注意每次生成剧本时，要回顾短剧通用结构与之前输出的任务1、2、3，确保输出的剧本不偏离最初的设定。
    """;
    public String generateScript(List<Map<String, String>> messages) {
        if (testMode){
            // 检查用户消息中是否包含 "#timeout" 字符串
            for (Map<String,String>msg: messages){
                if (msg.get("content") != null && msg.get("content").contains("#timeout")){
                    // 模拟超时异常
                    throw new RuntimeException("模拟AI调用超时，DeepSeek响应超时");
                }
            }
            // 正常测试模式：模拟耗时后返回固定内容
            try {
                Thread.sleep(50);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "模拟剧本生成测试";
        }

        // 1. 构建消息列表
        List<Map<String, Object>> fullMessages = new ArrayList<>();
        // 添加系统指令
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_INSTRUCTION);
        fullMessages.add(systemMsg);

        // 添加对话历史
        for (Map<String, String> msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.get("role"));
            m.put("content", msg.get("content"));
            fullMessages.add(m);
        }

        // 2.构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", fullMessages);

        // 3. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepseekApiKey);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        // 4. 发送请求
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    deepseekApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            System.out.println("DeepSeek响应: " + response.getBody());

            // 5. 解析响应，提取 AI 生成的文本 content
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return content;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用DeepSeek生成剧本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 拆解剧本，调用文生文，并解析返回的JSON
     * @param userScript 用户输入的剧本内容
     * @return 解析后的结构体
     */
    // 拆解剧本的提示词
    public String parseScript(String userScript) {
        if (testMode){
            return "{\"characters\":[{\"name\":\"测试角色\",\"description\":\"测试描述\",\"characterPrompt\":\"测试提示词\"}],\"storyboards\":[{\"description\":\"测试分镜\",\"scenePrompt\":\"测试场景\",\"detailedDescription\":\"测试详细描述\",\"characters\":[\"测试角色\"]}]}";
        }
        String systemInstruction = """
            你现在被赋予「专业影视分镜拆解师」的专属角色。你的核心任务是将用户提供的单集小说原文或剧本，拆解为适配AI视频生成的专业数据。所有输出必须严格遵循以下规则，并返回严格的JSON格式。
            ## 一、核心要求
            1. 先梳理用户提供的单集内容逻辑，将其拆解为 **8-15 个节奏适配的分镜头**（每个分镜对应 5-15 秒的视频内容，覆盖单集全部核心情节，无内容遗漏）。
            2. 每个分镜必须独立，按「分镜1、分镜2……」序号区分。
            3. 若某分镜的角色或场景与之前完全一致，可在输出中省略重复的角色/场景提示词（但JSON结构仍保留角色名列表）。
            ## 二、输出字段说明
            ### 1. 全局角色定义（characters 数组）
            根据剧本内容，提取所有主要角色，每个角色包含：
            - `name`: 角色名（简洁，如“林静”）
            - `description`: 角色简短描述（如“25岁，面容清秀，眼神疏离”）
            - `characterPrompt`: **用于生成角色多视图合图的完整提示词**。**必须根据剧本风格定制（如古风、科幻、二次元、写实等），不可固定为“写实拍摄”**。提示词应包含：角色名、外貌特征、服装、姿态、画风。**要求生成正面、侧面、背面、大头照四张图合在一张白底图中**，例如：“林静，25岁，清秀面容，眼神疏离，身穿素雅亚麻衬衫，头发扎起，文艺电影风格，柔和光线，四视图合在白底图上，全身”。
            ### 2. 分镜定义（storyboards 数组）
            每个分镜包含以下字段：
            - `description`: **分镜简短描述**。格式：“[角色]在[场景]做[动作]说[台词]”。例如：“林静在古籍修复室专注修补书页，自语‘又错了…’”
            - `scenePrompt`: **场景文生图提示词**。**仅描述场景本身，不包含任何角色**。必须精确描述环境、光线、氛围等。例如：“安静的古籍修复室，工作台上堆满待修复的泛黄古籍，柔和光线从窗户射入，尘埃漂浮，静谧陈旧。”
            - `detailedDescription`: **关键帧生成提示词**。用于图生图生成关键帧图片。**必须引用角色和场景，描述角色在场景中的具体动作、表情、构图，但不要包含运镜描述**。格式：“图1中的人物（角色名）在图2的场景中做[动作]，表情[表情]，采用[构图]”。例如：“林静戴着白色手套，轻柔修补书页，采用手部特写切入，后拉至中景，展现专注侧脸，表情认真略带疲惫。”
            - `videoPrompt`: **视频生成提示词**。**必须包含视频时长（例如“时长4秒”）**，以及镜头运动（推、拉、摇、移、跟）、节奏、音效等动态描述。可融合分镜描述中的动作、台词等。例如：“时长4秒，林静在古籍修复室中专注修补书页，镜头从手部特写缓慢拉远至中景，背景音乐轻柔，她低声自语‘又错了…’。”
            - `characters`: 该分镜涉及的角色名列表（数组），从全局角色中选取。
            ## 三、输出规范
            所有内容必须严格按以下JSON格式输出，不要包含任何其他文字：
            {
              "characters": [
                {
                  "name": "角色名",
                  "description": "角色简短描述",
                  "characterPrompt": "完整的角色多视图合图提示词（四视图+白底）"
                }
              ],
              "storyboards": [
                {
                  "description": "分镜简短描述（角色+动作+台词）",
                  "scenePrompt": "场景图生成提示词（仅场景）",
                  "detailedDescription": "关键帧生成提示词（引用角色图与场景图，描述动作、构图）",
                  "videoPrompt": "视频生成提示词（包含时长、运镜、描述）",
                  "characters": ["角色名1", "角色名2"]
                }
              ]
            }
            请严格按照上述要求，将用户提供的单集内容拆解为JSON。确保所有提示词精准、无冗余，风格统一。
            用户提供的单集内容如下：
            """ + userScript;

        // 构建消息列表（将系统指令作为 system 角色）
        List<Map<String, Object>> fullMessages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content",systemInstruction);
        fullMessages.add(systemMsg);

        // 添加用户消息（简单确认）
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "请拆解以上剧本。");
        fullMessages.add(userMsg);

        // 2.构建请求体
        Map<String,Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", fullMessages);
        requestBody.put("max_tokens",8192); // DeepSeek 支持的最大值

        // 3. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepseekApiKey);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        // 4. 发送请求
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    deepseekApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            System.out.println("DeepSeek拆解响应: " + response.getBody());

            // 5. 解析响应，提取 AI 生成的文本 content
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // 清理 Markdown
            String cleaned = content.replaceFirst("^```json\\s*\\n?", "")
                    .replaceFirst("\\n?```$", "")
                    .trim();
            // 如果清理后还是以 { 开头，说明可能有效；否则尝试取第一个 { 到最后一个 }
            if (!cleaned.startsWith("{")) {
                int start = cleaned.indexOf('{');
                int end = cleaned.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    cleaned = cleaned.substring(start, end + 1);
                }
            }
            return cleaned;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用DeepSeek拆解剧本失败: " + e.getMessage(), e);
        }
    }



    /**
     * 调用阿里云多模态模型生成图片
     * @param prompt 文本提示词（必需）
     * @param imageUrls 参考图片URL列表（可选，用于图文融合）
     * @return 生成的图片URL
     */
    public String generateImageFromMultimodal(String prompt, List<String> imageUrls){
        if (testMode){
            return "https://example.com/test-image.jpg";
        }
        // 1.构建请求体
        Map<String , Object> requestBody = new HashMap<>();
        requestBody.put("model","qwen-image-2.0-2026-03-03");

        // 构建 input.messages
        Map<String,Object> input = new HashMap<>();     // 输入的基本信息。
        List<Map<String,Object>> messages = new ArrayList<>();   // 请求内容数组。当前仅支持单轮对话，数组内有且只有一个元素。
        Map<String ,Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Object> contentList = new ArrayList<>();

        // 如果有参考图片，先添加图片,如果 imageUrls 非空，则构建图文混合输入（用于关键帧）；否则只发送文本（用于角色/场景）。
        if (imageUrls != null && !imageUrls.isEmpty()){
            for (String url : imageUrls){
                Map<String ,String > imageContent = new HashMap<>();
                imageContent.put("image",url);
                contentList.add(imageContent);
            }
        }

        // 添加文本提示词
        Map<String,String> textContent = new HashMap<>();
        textContent.put("text",prompt);
        contentList.add(textContent);
        userMessage.put("content",contentList);
        messages.add(userMessage);
        input.put("messages",messages);
        requestBody.put("input",input);

        // 构建 parameters 图像处理参数。
        Map<String,Object>parameters = new HashMap<>();
        parameters.put("n" , 1);    // 输出图像的数量，默认值为1。对于qwen-image-2.0系列模型，可选择输出1-6张图片。
        parameters.put("negative_prompt", " ");     // 反向提示词
        parameters.put("prompt_extend", true);      // 是否开启 Prompt（提示词）智能改写功能。
        parameters.put("watermark", false);         // 是否在图像右下角添加 "Qwen-Image" 水印
        parameters.put("size", "2688*1536");    //  512*512至2048*2048之间 2688*1536 ：16:9，1536*2688 ：9:16，2048*2048（默认值）：1:1
        requestBody.put("parameters", parameters);

        // 2. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aliyunApiKey);
        HttpEntity<Map<String,Object>>httpEntity = new HttpEntity<>(requestBody,headers);

        // 3. 发送请求
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    aliyunMultiModalUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            System.out.println("生成图片AI原始响应："+ response.getBody());

            // 4. 解析响应，提取生成的图片URL
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("output")
                                  .path("choices").get(0)
                                  .path("message")
                                  .path("content").get(0);
            if (contentNode == null || contentNode.isMissingNode()){
                throw new RuntimeException("响应中缺少 content节点");
            }
            String imageUrl =contentNode.path("image").asText();
            if (imageUrl == null || imageUrl.isEmpty()){
                throw new RuntimeException("响应中缺少 image字段");
            }
            return imageUrl;
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("调用生成图片AI失败"+ e.getMessage(),e);
        }
    }

    /**
     * 调用阿里云AI视频生成接口，创建异步任务
     * @param imageUrl 关键帧图片URL
     * @param prompt 文字提示（可选，文档说image_url和prompt不能同时为空，所以如果传了image_url，prompt可为空）
     * @return 返回的任务对象（包含taskId等）
     */
    public Map<String,String> createVideoGenerationTask(String imageUrl,String prompt) {
        if (testMode){
            Map<String,String> result = new HashMap<>();
            result.put("taskId", "test-task-id");
            result.put("status", "SUCCEEDED");
            return result;
        }
        // 1. 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "wan2.6-r2v-flash");
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt != null ? prompt : "");
        List<String> referenceUrls = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            referenceUrls.add(imageUrl);
        }
        input.put("reference_urls", referenceUrls);
        requestBody.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("size", "1280*720");
        parameters.put("shot_type", "multi"); // 多镜头
        requestBody.put("parameters", parameters);

        // 2. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + videoApiKey);
        headers.set("X-DashScope-Async", "enable");  // 异步任务标识
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        // 3. 发送请求
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    videoApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            System.out.println("视频生成AI任务响应: " + response.getBody());

            // 4. 解析响应，提取task_id
            JsonNode root = objectMapper.readTree(response.getBody());
            String taskId = root.path("output").path("task_id").asText();   //生成的任务ID，调用请求结果接口时使用此ID。
            String taskStatus = root.path("output").path("task_status").asText();
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", taskStatus);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建视频生成任务失败" + e.getMessage(), e);
        }
    }

    /**
     * 查询视频生成任务的结果
     * @param taskId 任务ID
     * @return 任务状态和结果（如果成功，包含视频URL）
     */
    public Map<String,Object> queryVideoTaskResult(String taskId){
        String url = videoResultUrl.replace("{task_id}",taskId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + videoApiKey);

        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    httpEntity,
                    String.class
            );
            System.out.println("视频生成AI任务查询响应: " + response.getBody());

            // 解析响应
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode output = root.path("output");
            // PENDING（排队中）→ RUNNING（处理中）→ SUCCEEDED（成功）/ FAILED（失败）
            String taskStatus = output.path("task_status").asText();

            Map<String, Object> result = new HashMap<>();
            result.put("status", taskStatus);

            if ("SUCCEEDED".equals(taskStatus)) {
                // 如果成功，提取视频URL。
                String videoUrl = output.path("video_url").asText();
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    result.put("videoUrl", videoUrl);
                } else {
                    // 兼容可能有其他结构的情况
                    JsonNode results = output.path("results");
                    if (results.isArray() && results.size() > 0) {
                        videoUrl = results.get(0).path("video_url").asText();
                        result.put("videoUrl", videoUrl);
                    }
                }
            } else if ("FAILED".equals(taskStatus)) {
                String errorMsg = output.path("fail_reason").asText("未知错误");
                result.put("error", errorMsg);
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("查询视频任务失败：" + e.getMessage(), e);
        }
    }



}
