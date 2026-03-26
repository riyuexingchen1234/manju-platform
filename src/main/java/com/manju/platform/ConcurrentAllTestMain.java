//package com.manju.platform;
//
//import com.manju.platform.dto.*;
//import com.manju.platform.service.*;
//import org.springframework.boot.SpringApplication;
//import org.springframework.context.ConfigurableApplicationContext;
//
//import java.util.*;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class ConcurrentAllTestMain {
//
//    public static void main(String[] args) throws InterruptedException {
//        // 1. 启动 Spring 容器
//        ConfigurableApplicationContext context = SpringApplication.run(ManjuPlatformApplication.class, args);
//
//        // 2. 开启测试模式（AIService 中需要有静态开关）
//        AIService.enableTestMode();
//
//        // 3. 获取所有 Service
//        ScriptService scriptService = context.getBean(ScriptService.class);
//        ParseService parseService = context.getBean(ParseService.class);
//        CharacterService characterService = context.getBean(CharacterService.class);
//        SceneService sceneService = context.getBean(SceneService.class);
//        KeyframeService keyframeService = context.getBean(KeyframeService.class);
//        VideoService videoService = context.getBean(VideoService.class);
//
//        // 测试用户ID（确保数据库中有该用户，且积分充足）
//        int userId = 1;
//
//        // 每个功能的并发线程数
//        int threadCountPerFunc = 5;
//        int callsPerThread = 3;
//
//        // 统计计数器
//        AtomicInteger scriptSuccess = new AtomicInteger();
//        AtomicInteger scriptFail = new AtomicInteger();
//        AtomicInteger parseSuccess = new AtomicInteger();
//        AtomicInteger parseFail = new AtomicInteger();
//        AtomicInteger characterSuccess = new AtomicInteger();
//        AtomicInteger characterFail = new AtomicInteger();
//        AtomicInteger sceneSuccess = new AtomicInteger();
//        AtomicInteger sceneFail = new AtomicInteger();
//        AtomicInteger keyframeSuccess = new AtomicInteger();
//        AtomicInteger keyframeFail = new AtomicInteger();
//        AtomicInteger videoSuccess = new AtomicInteger();
//        AtomicInteger videoFail = new AtomicInteger();
//
//        ExecutorService executor = Executors.newFixedThreadPool(threadCountPerFunc * 6);
//        CountDownLatch latch = new CountDownLatch(threadCountPerFunc * 6);
//
//        long start = System.currentTimeMillis();
//
//        // 剧本生成
//        for (int i = 0; i < threadCountPerFunc; i++) {
//            executor.submit(() -> {
//                for (int j = 0; j < callsPerThread; j++) {
//                    try {
//                        List<Map<String, String>> messages = new ArrayList<>();
//                        Map<String, String> userMsg = new HashMap<>();
//                        userMsg.put("role", "user");
//                        userMsg.put("content", "测试提示词");
//                        messages.add(userMsg);
//                        scriptService.generateScript(userId, messages);
//                        scriptSuccess.incrementAndGet();
//                    } catch (Exception e) {
//                        scriptFail.incrementAndGet();
//                    }
//                }
//                latch.countDown();
//            });
//        }
//
//        // 拆解剧本
//        for (int i = 0; i < threadCountPerFunc; i++) {
//            executor.submit(() -> {
//                for (int j = 0; j < callsPerThread; j++) {
//                    try {
//                        parseService.parseScript(userId, "测试剧本");
//                        parseSuccess.incrementAndGet();
//                    } catch (Exception e) {
//                        parseFail.incrementAndGet();
//                    }
//                }
//                latch.countDown();
//            });
//        }
//
//        // 角色生成
//        for (int i = 0; i < threadCountPerFunc; i++) {
//            executor.submit(() -> {
//                for (int j = 0; j < callsPerThread; j++) {
//                    try {
//                        CharacterGenerateRequest req = new CharacterGenerateRequest();
//                        req.setUserId(userId);
//                        req.setCharacterName("测试角色");
//                        req.setCharacterPrompt("测试提示词");
//                        characterService.generateCharacter(req);
//                        characterSuccess.incrementAndGet();
//                    } catch (Exception e) {
//                        characterFail.incrementAndGet();
//                    }
//                }
//                latch.countDown();
//            });
//        }
//
//        // 场景生成
//        for (int i = 0; i < threadCountPerFunc; i++) {
//            executor.submit(() -> {
//                for (int j = 0; j < callsPerThread; j++) {
//                    try {
//                        SceneGenerateRequest req = new SceneGenerateRequest();
//                        req.setUserId(userId);
//                        req.setScenePrompt("测试场景");
//                        sceneService.generateScene(req);
//                        sceneSuccess.incrementAndGet();
//                    } catch (Exception e) {
//                        sceneFail.incrementAndGet();
//                    }
//                }
//                latch.countDown();
//            });
//        }
//
//        // 关键帧生成
//        for (int i = 0; i < threadCountPerFunc; i++) {
//            executor.submit(() -> {
//                for (int j = 0; j < callsPerThread; j++) {
//                    try {
//                        KeyframeGenerateRequest req = new KeyframeGenerateRequest();
//                        req.setUserId(userId);
//                        req.setStoryboardDescription("测试分镜");
//                        req.setCharacterImageUrl("https://example.com/char.jpg");
//                        req.setSceneImageUrl("https://example.com/scene.jpg");
//                        keyframeService.generateKeyframe(req);
//                        keyframeSuccess.incrementAndGet();
//                    } catch (Exception e) {
//                        keyframeFail.incrementAndGet();
//                    }
//                }
//                latch.countDown();
//            });
//        }
//
//        // 视频生成
//        for (int i = 0; i < threadCountPerFunc; i++) {
//            executor.submit(() -> {
//                for (int j = 0; j < callsPerThread; j++) {
//                    try {
//                        VideoGenerateRequest req = new VideoGenerateRequest();
//                        req.setUserId(userId);
//                        req.setKeyframeImageUrl("https://example.com/keyframe.jpg");
//                        req.setDescription("测试视频");
//                        videoService.generateVideo(req);
//                        videoSuccess.incrementAndGet();
//                    } catch (Exception e) {
//                        videoFail.incrementAndGet();
//                    }
//                }
//                latch.countDown();
//            });
//        }
//
//        latch.await();
//        executor.shutdown();
//        long end = System.currentTimeMillis();
//
//        System.out.println("========== 并发测试结果 ==========");
//        System.out.println("总耗时: " + (end - start) + " ms");
//        System.out.println("剧本生成: 成功 " + scriptSuccess.get() + "，失败 " + scriptFail.get());
//        System.out.println("拆解剧本: 成功 " + parseSuccess.get() + "，失败 " + parseFail.get());
//        System.out.println("角色生成: 成功 " + characterSuccess.get() + "，失败 " + characterFail.get());
//        System.out.println("场景生成: 成功 " + sceneSuccess.get() + "，失败 " + sceneFail.get());
//        System.out.println("关键帧生成: 成功 " + keyframeSuccess.get() + "，失败 " + keyframeFail.get());
//        System.out.println("视频生成: 成功 " + videoSuccess.get() + "，失败 " + videoFail.get());
//
//        // 关闭测试模式
//        AIService.disableTestMode();
//        context.close();
//    }
//}