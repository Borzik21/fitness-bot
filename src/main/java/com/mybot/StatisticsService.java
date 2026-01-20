package com.mybot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class StatisticsService {
    private static final String STATS_FILE = "user_stats.json";
    private Map<String, Integer> questionCounts;
    private int totalQuestions;

    public StatisticsService() {
        loadStats();
    }

    public void updateStats(String chatId) {
        questionCounts.put(chatId, questionCounts.getOrDefault(chatId, 0) + 1);
        totalQuestions++;
        saveStats();
    }

    public String getStatsMessage(String chatId) {
        int userCount = questionCounts.getOrDefault(chatId, 0);
        return "Статистика: ты задал " + userCount + " вопросов. Всего вопросов в боте: " + totalQuestions;
    }

    private synchronized void saveStats() {
        try (Writer writer = new FileWriter(STATS_FILE)) {
            Gson gson = new Gson();
            Map<String, Object> data = new HashMap<>();
            data.put("totalQuestions", totalQuestions);
            data.put("questionCounts", questionCounts);
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загружает статистику из JSON-файла. (ИСПРАВЛЕННАЯ ВЕРСИЯ)
     */
    private void loadStats() {
        try (Reader reader = new FileReader(STATS_FILE)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(reader, type);

            if (data != null) {

                // 1. Безопасно читаем totalQuestions как Number и преобразуем в int.
                Number total = (Number) data.getOrDefault("totalQuestions", 0);
                this.totalQuestions = total.intValue();

                // 2. Безопасно читаем questionCounts.
                // Gson вернет Map<String, Double>, поэтому мы должны преобразовать его вручную.
                Map<String, Double> rawCounts = (Map<String, Double>) data.getOrDefault("questionCounts", new HashMap<>());
                this.questionCounts = new HashMap<>();
                for (Map.Entry<String, Double> entry : rawCounts.entrySet()) {
                    // Преобразуем каждое значение Double в Integer перед добавлением в нашу карту.
                    this.questionCounts.put(entry.getKey(), entry.getValue().intValue());
                }
            } else {
                resetStats();
            }
        } catch (FileNotFoundException e) {
            System.out.println("Файл статистики не найден. Создаю новую статистику.");
            resetStats();
        } catch (Exception e) { // Ловим более общую ошибку на всякий случай
            System.err.println("Ошибка при чтении файла статистики, сбрасываю статистику.");
            e.printStackTrace();
            resetStats();
        }
    }

    private void resetStats() {
        this.questionCounts = new HashMap<>();
        this.totalQuestions = 0;
    }
}