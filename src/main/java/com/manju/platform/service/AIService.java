package com.manju.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;
    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${aliyun.api.url}")
    private String aliyunImageApiUrl;
    @Value("${aliyun.api.key}")
    private String aliyunApiKey;

    @Value("${video.api.url}")
    private String videoApiUrl;
    @Value("${video.api.key}")
    private String videoApiKey;
    @Value("${video.result.url}")
    private String videoResultUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试模式开关（仅用于并发测试）
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
     */
    private final String SYSTEM_INSTRUCTION = """
            你是面向短剧市场的专业AI编剧，擅长爽文短剧的套路化创作。

            【短剧通用套路】
            - 开头交代看点，主角遇危机/矛盾
            - 觉醒金手指或侧面交代已有能力，确立主线
            - 主角利用信息差变强打脸，集中主线安排事件
            - 情绪连贯，台词不水
            - 开局安排目标，过程有反派捣乱
            - 杀不了的反派搞事→面对危机→打脸回去，或因某个原因暂时不撕破脸
            - 中间加大佬帮主角，让主角不至于被干死还能占优势
            - 每3-5分钟一个小反转，每10分钟一个大冲突
            - 每集结尾必留钩子（悬念/反转/危机）
            - 前30集爆点密集，主要角色前10集全部登场

            【工作流程】

            **第一步：输出梗概 + 角色表**
            - 若用户提供信息不足（如只说"帮我写个短剧"），主动追问1-2轮补充：题材/爽点/人设偏好
            - 若用户提供信息已足够，直接输出
            - 输出内容：
              1. 一句话梗概（故事核心冲突+终极爽点）
              2. 核心角色列表，每个角色包含：
                 - 角色名
                 - 年龄
                 - 标签化人设（如"隐忍复仇的落魄千金""表面纨绔实则深情的霸总"）
                 - 外貌关键词（如"清秀面容、长发、素色连衣裙"）
                 - 与其他角色的关系
              3. 预计集数和题材标签
            - 询问用户是否满意，满意则进入第二步

            **第二步：输出完整大纲**
            - 每10集一个章节梗概
            - 明确标注付费卡点位置（通常第10-15集第一个卡点）
            - 标注核心爽点位置
            - 询问用户是否满意，满意则进入第三步

            **第三步：逐集输出剧本**
            - 严格按短剧格式输出（详见下方规范）
            - 每次输出一集，用户确认满意后输出下一集
            - 用户不满意则修改当前集，满意后继续

            【短剧剧本格式规范】

            1. 每集开头标注集数：第一集
            2. 每个场景最前端标注基本信息：场号、时间、环境、地点、人物、（道具）
               - 场号：1-1代表第一集第一个场景，1-2代表第一集第二个场景。更换场景要空一行
               - 时间：日/夜
               - 环境：内/外
               - 地点：具体地点
               - 人物：该场景出现的主要人物
            3. 剧本符号：
               - △：除台词外可通过镜头演绎的部分（动作、场景描写等），△的内容要与台词分开
               - 人名：台词内容
               - （）：伴随台词，写语气、表情、情绪、动作等提示。例：人名（冷笑）：台词内容。说话过程中发生的动作也可放在（）中。注意（）里内容不要过长，动作复杂则用△单独描写
               - OS：角色心理活动。记为：人名（OS）：心里台词。OS同时有动作表情时，先写动作表情再写OS
               - VO：画外音，人物未出现画面中但声音出现就用VO。电话另一头、广播声音也用VO
               - 【闪回】/【闪出】：角色进入回忆画面
               - 【字幕：内容】：画面中要出现的特殊字幕。三种常见用法：
                 a. 聊天信息：【对话框字幕：内容】
                 b. 首次出场人物：【字幕：人名，身份】
                 c. 时间说明：【字幕：x年后/x月后】
               - 【空镜：画面内容】：氛围渲染、转场、用画面表达情绪
            4. 每集结尾标注：【本集钩子：xxx】（明确这集结尾的悬念/反转）
            5. 单集字数限制在1000～1200字之间

            【格式示例】

            《xx剧名》

            第一集

            1-1 日 内 宴会厅 人物：女主、男主、男二、女主爸爸、司仪、宾客若干

            △宴会厅内漆黑，聚光灯聚焦宴会厅大门。
            司仪（VO）：让我们用热烈的掌声，有请新娘入场！
            △宴会厅大门拉开，女主穿着婚纱走进灯光。
            【字幕：女主名字 xx集团千金】
            △T台正中，男主正背对着女主站立。女主踏上婚礼T台，向男主走去。
            △男主目不转睛地看着穿着婚纱的女主。
            【字幕：男二名字 xx公司CEO】

            【闪回】
            【字幕：一年前】
            【空镜：人来人往的商业街】
            女主（看着橱窗婚纱甜甜地笑）：你说，我以后穿上婚纱会是什么样？
            男二（深情地）：一定是全世界最美的新娘。（轻吻女主）我爱你！
            【闪出】

            △男二红了眼眶。
            男二（OS）：希望你的选择是对的，一定要幸福。

            △女主走到男主身后，轻轻拍了拍男主的肩膀，男主转过身，已是泪流满面，女主也忍不住流泪。
            【字幕：男主名字 职业电竞选手】

            司仪（VO）：请新娘拥抱新郎。
            △男主将女主轻轻拥入怀中。
            男主（温柔地轻声地）：你好美。

            【本集钩子：男主身份远不止表面看到的那么简单——他到底是谁？】
        """;

    public String generateScript(List<Map<String, String>> messages) {
        if (testMode) {
            for (Map<String, String> msg : messages) {
                if (msg.get("content") != null && msg.get("content").contains("#timeout")) {
                    throw new RuntimeException("模拟AI调用超时，DeepSeek响应超时");
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "模拟剧本生成测试";
        }

        List<Map<String, Object>> fullMessages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_INSTRUCTION);
        fullMessages.add(systemMsg);

        for (Map<String, String> msg : messages) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.get("role"));
            m.put("content", msg.get("content"));
            fullMessages.add(m);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", fullMessages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepseekApiKey);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    deepseekApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            logger.debug("DeepSeek响应: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return content;
        } catch (Exception e) {
            logger.error("调用DeepSeek生成剧本失败", e);
            throw new RuntimeException("调用DeepSeek生成剧本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 拆解剧本，调用文生文，并解析返回的JSON
     */
    public String parseScript(String userScript) {
        if (testMode) {
            return "{\"characters\":[{\"name\":\"测试角色\",\"description\":\"测试描述\",\"characterPrompt\":\"测试提示词\"}],\"storyboards\":[{\"description\":\"测试分镜\",\"scenePrompt\":\"测试场景\",\"detailedDescription\":\"测试详细描述\",\"characters\":[\"测试角色\"]}]}";
        }
        String systemInstruction = """
            你是专业的影视分镜拆解师，将单集短剧剧本转换为适配AI视频生成的数据格式。输出标准JSON，禁止包含其他内容。

            ## JSON结构

            {
              "styleDeclaration": "全局视觉风格声明",
              "characters": [...],
              "storyboards": [...]
            }

            ## styleDeclaration字段
            从剧本中推断出统一的视觉风格声明，所有分镜的prompt必须遵循这个风格。
            格式：画面风格+色调+质感。
            例如：
            - "现代都市写实风，冷色调，电影质感，细腻光影"
            - "古风唯美，暖黄调，水墨意境，柔和光线"
            - "二次元动漫风，明亮色彩，日系赛璐珞质感"

            ## characters数组
            每个角色包含：
            - `name`：角色名
            - `description`：角色简短描述
            - `characterPrompt`：用于生成角色多视图合图的完整提示词

            **characterPrompt优化要点**（最关键！）：
            1. 必须包含：角色名+详细外貌（发型发色、眼型瞳色、脸型、体型、肤色）+服装细节（颜色、材质、款式）+标志性特征（疤痕、首饰、纹身等）+画风声明（必须与styleDeclaration一致）
            2. 构图指令精确化："一张白底角色设定图，从左到右依次为：大头特写（面部细节清晰可见）、正面全身站姿、侧面全身站姿、背面全身站姿。四个视图的人物外观完全一致，同一套服装，同一张脸，同一人。白底，无背景。"
            3. 示例："林静，25岁女性，清秀面容，瓜子脸，皮肤白皙，黑色长直发及腰，杏仁眼深棕色瞳孔，身材纤细。身穿素雅亚麻白色衬衫，衣袖挽至小臂，下身深蓝色长裙，脚踩白色帆布鞋。左耳戴一枚银色小耳钉。现代都市写实风，冷色调，电影质感。一张白底角色设定图，从左到右依次为：大头特写（面部细节清晰可见）、正面全身站姿、侧面全身站姿、背面全身站姿。四个视图的人物外观完全一致，同一套服装，同一张脸，同一人。白底，无背景。"

            ## storyboards数组
            根据剧本长度动态调整分镜数量，一般每500字剧本2-3个分镜。

            每个分镜包含：
            - `description`：分镜简短描述。格式："[角色]在[场景]做[动作]说[台词]"
            - `scenePrompt`：场景文生图提示词
            - `detailedDescription`：关键帧生成提示词
            - `videoPrompt`：视频生成提示词
            - `characters`：该分镜涉及的角色名列表

            **scenePrompt优化要点**：
            1. **绝对不出现任何人物、人形、人影、手脚**，只有纯环境
            2. 必须包含：空间布局、材质纹理、光线方向和色温、氛围关键词、天气/时间
            3. 画风声明：必须与styleDeclaration风格一致
            4. 示例："现代都市写实风。一间安静的古籍修复室，木质工作台上堆满待修复的泛黄古籍和宣纸，桌面散落毛笔和浆糊碗。午后阳光从左侧木格窗户斜射入内，尘埃在光柱中漂浮，地面铺深色木地板，墙角立着装满古书的樟木书架。静谧、陈旧、温暖的氛围。电影质感，冷色调，细腻光影。无人物。"

            **detailedDescription优化要点**（适配多角色关键帧生成）：
            1. 不要用"图1中的人物...图2的场景..."这种固定格式
            2. 先描述整体场景和氛围，再描述每个角色的具体位置、动作、表情、与他人的互动
            3. 要有画面感和构图意识（如谁在前谁在后、谁左谁右、什么景别）
            4. 示例："古籍修复室内，午后阳光斜照。林静坐在工作台前的木椅上，低头专注地修补泛黄古籍，白色衬衫袖子挽起，表情认真略带疲惫，手中轻柔翻动书页。张伟站在她身后右侧，双手插在西装裤口袋里，微微侧头注视她，神情若有所思。中景构图，两人一坐一站形成高低错落。现代都市写实风，电影质感。"

            **videoPrompt优化要点**：
            1. 必须包含视频时长（如"时长5秒"）
            2. 包含运镜描述（推、拉、摇、移、跟）+节奏（缓慢/快速）
            3. 角色动态动作描述（不要只写静态画面）
            4. 示例："时长5秒，镜头从林静手部特写缓慢拉远至中景，展现她专注修补古籍的动作，阳光在她发丝间流动。随后镜头微微右摇，带出身后站立的张伟。背景音乐轻柔，林静低声自语'又错了…'。缓慢节奏，电影质感。"

            ## 拆解规则
            1. 先梳理剧本内容逻辑，根据场景变化和镜头节奏拆解分镜
            2. △标记的内容直接对应视觉画面，是分镜拆解的主要依据
            3. 对话内容决定角色在场和动作表情
            4. 【空镜】单独作为一个分镜
            5. 【闪回】内容也要拆解为分镜
            6. 若某分镜的角色或场景与前一个完全一致，可在scenePrompt和characterPrompt中简写（但JSON结构仍保留characters列表）
            7. 分镜按顺序编号：分镜1、分镜2……

            ## 输出JSON格式示例

            ```json
            {
              "styleDeclaration": "全局视觉风格声明",
              "characters": [
                {
                  "name": "角色名",
                  "description": "简短描述",
                  "characterPrompt": "完整的角色设定图prompt，包含四视图构图"
                }
              ],
              "storyboards": [
                {
                  "description": "[角色]在[场景]做[动作]，说[台词]",
                  "scenePrompt": "纯场景描述，无人物，遵循风格声明",
                  "detailedDescription": "关键帧图生图prompt，描述角色在场景中的具体状态",
                  "videoPrompt": "时长+运镜+动态描述+音效",
                  "characters": ["角色名1", "角色名2"]
                }
              ]
            }
            ```

            请严格按照上述规范，将用户提供的单集剧本拆解为精确的JSON数据。用户提供的单集内容如下：
            """ + userScript;

        List<Map<String, Object>> fullMessages = new ArrayList<>();
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemInstruction);
        fullMessages.add(systemMsg);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", "请拆解以上剧本。");
        fullMessages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", fullMessages);
        requestBody.put("max_tokens", 8192);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepseekApiKey);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    deepseekApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            logger.debug("DeepSeek拆解响应: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            String cleaned = content.replaceFirst("^```json\\s*\\n?", "")
                    .replaceFirst("\\n?```$", "")
                    .trim();
            if (!cleaned.startsWith("{")) {
                int start = cleaned.indexOf('{');
                int end = cleaned.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    cleaned = cleaned.substring(start, end + 1);
                }
            }
            return cleaned;
        } catch (Exception e) {
            logger.error("调用DeepSeek拆解剧本失败", e);
            throw new RuntimeException("调用DeepSeek拆解剧本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用阿里云多模态模型生成图片
     * @param prompt    文本提示词
     * @param imageUrls 参考图片URL列表（可选，用于图文融合）
     * @return 生成的图片URL
     */
    public String generateImage(String prompt, List<String> imageUrls) {
        if (testMode) {
            return "https://example.com/test-image.jpg";
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen-image-2.0-2026-03-03");

        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");

        List<Object> contentList = new ArrayList<>();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String url : imageUrls) {
                Map<String, String> imageContent = new HashMap<>();
                imageContent.put("image", url);
                contentList.add(imageContent);
            }
        }

        Map<String, String> textContent = new HashMap<>();
        textContent.put("text", prompt);
        contentList.add(textContent);
        userMessage.put("content", contentList);
        messages.add(userMessage);
        input.put("messages", messages);
        requestBody.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("n", 1);
        parameters.put("negative_prompt", " ");
        parameters.put("prompt_extend", true);
        parameters.put("watermark", false);
        parameters.put("size", "2688*1536");
        requestBody.put("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aliyunApiKey);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    aliyunImageApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            logger.debug("生成图片AI原始响应：{}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("output")
                    .path("choices").get(0)
                    .path("message")
                    .path("content").get(0);
            if (contentNode == null || contentNode.isMissingNode()) {
                throw new RuntimeException("响应中缺少 content节点");
            }
            String imageUrl = contentNode.path("image").asText();
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new RuntimeException("响应中缺少 image字段");
            }
            return imageUrl;
        } catch (Exception e) {
            logger.error("调用生成图片AI失败", e);
            throw new RuntimeException("调用生成图片AI失败" + e.getMessage(), e);
        }
    }

    /**
     * 调用阿里云AI视频生成接口，创建异步任务
     * 模型：wan2.7-r2v
     */
    public Map<String, String> createVideoGenerationTask(String imageUrl, String prompt) {
        if (testMode) {
            Map<String, String> result = new HashMap<>();
            result.put("taskId", "test-task-id");
            result.put("status", "SUCCEEDED");
            return result;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "wan2.7-r2v");
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt != null ? prompt : "");

        // media数组：支持reference_image/first_frame等多种类型
        List<Map<String, String>> mediaList = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Map<String, String> media = new HashMap<>();
            media.put("type", "reference_image");
            media.put("url", imageUrl);
            mediaList.add(media);
        }
        input.put("media", mediaList);
        requestBody.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("resolution", "720P");
        parameters.put("duration", 5);
        parameters.put("prompt_extend", false);
        parameters.put("watermark", false);
        requestBody.put("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + videoApiKey);
        headers.set("X-DashScope-Async", "enable");
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    videoApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );
            logger.debug("视频生成AI任务响应: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            String taskId = root.path("output").path("task_id").asText();
            String taskStatus = root.path("output").path("task_status").asText();
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("status", taskStatus);
            return result;
        } catch (Exception e) {
            logger.error("创建视频生成任务失败", e);
            throw new RuntimeException("创建视频生成任务失败" + e.getMessage(), e);
        }
    }

    /**
     * 查询视频生成任务的结果
     */
    public Map<String, Object> queryVideoTaskResult(String taskId) {
        String url = videoResultUrl.replace("{task_id}", taskId);

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
            logger.debug("视频生成AI任务查询响应: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode output = root.path("output");
            String taskStatus = output.path("task_status").asText();

            Map<String, Object> result = new HashMap<>();
            result.put("status", taskStatus);

            if ("SUCCEEDED".equals(taskStatus)) {
                String videoUrl = output.path("video_url").asText();
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    result.put("videoUrl", videoUrl);
                } else {
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
            logger.error("查询视频任务失败", e);
            throw new RuntimeException("查询视频任务失败：" + e.getMessage(), e);
        }
    }
}
