package com.mybot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {
    private static final String ACTIVITY_FILE = "user_activity.json";
    private final Map<String, Long> userLastActivity = new ConcurrentHashMap<>();
    private final FitnessBot bot;
    private final GeminiService geminiService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public NotificationService(FitnessBot bot, GeminiService geminiService) {
        this.bot = bot;
        this.geminiService = geminiService;
        loadActivity();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAndSendNotifications, 1, 24, TimeUnit.HOURS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    public void recordUserActivity(String chatId) {
        userLastActivity.put(chatId, System.currentTimeMillis());
        saveActivity();
    }

    private void checkAndSendNotifications() {
        System.out.println("Проверка пользователей для отправки ежеедельных уведомлений...");
        long sixDaysAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(144);

        for (Map.Entry<String, Long> entry : userLastActivity.entrySet()) {
            if (entry.getValue() < sixDaysAgo) {
                String chatId = entry.getKey();
                String prompt = "Ты — воодушевляющий помошник в спорте. Придумай короткое (одно-два предложения), дружелюбное и немного загадочное сообщение для пользователя, который давно не заходил. Твоя цель — мягко напомнить о себе и вовлечь его в диалог, задав открытый вопрос о его планах, целях или мечтах. Начинай сразу с обращения, без приветствий.";

                List<Message> singleMessageHistory = List.of(new Message("user", prompt));
                String encouragement = geminiService.getSupport(singleMessageHistory);

                SendMessage message = new SendMessage(chatId, encouragement + "🎱");
                bot.sendMessage(message);
                recordUserActivity(chatId);
            }
        }
    }

    private synchronized void saveActivity() {
        try (Writer writer = new FileWriter(ACTIVITY_FILE)) {
            new Gson().toJson(userLastActivity, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadActivity() {
        try (Reader reader = new FileReader(ACTIVITY_FILE)) {
            Type type = new TypeToken<ConcurrentHashMap<String, Long>>(){}.getType();
            Map<String, Long> loadedMap = new Gson().fromJson(reader, type);
            if (loadedMap != null) {
                userLastActivity.putAll(loadedMap);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Файл активности не найден. Будет создан новый.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}