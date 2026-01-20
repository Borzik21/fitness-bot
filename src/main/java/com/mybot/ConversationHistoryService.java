package com.mybot;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConversationHistoryService {
    // Ограничение на количество сообщений в истории для одного пользователя.
    private static final int HISTORY_LIMIT = 20;

    // Map для хранения истории: chatId -> список сообщений.
    private final Map<Long, LinkedList<Message>> histories = new HashMap<>();

    /**
     * Добавляет новое сообщение в историю диалога для указанного пользователя.
     * Если история превышает лимит, самое старое сообщение удаляется.
     * @param chatId ID чата пользователя.
     * @param message Объект сообщения (роль + текст).
     */
    public void addMessage(long chatId, Message message) {
        // Если для этого пользователя еще нет истории, создаем ее.
        LinkedList<Message> userHistory = histories.computeIfAbsent(chatId, k -> new LinkedList<>());

        // Добавляем новое сообщение в конец.
        userHistory.add(message);

        // Если история стала слишком длинной, удаляем самое первое (самое старое) сообщение.
        if (userHistory.size() > HISTORY_LIMIT) {
            userHistory.removeFirst();
        }
    }

    /**
     * Возвращает полную историю диалога для пользователя.
     * @param chatId ID чата пользователя.
     * @return Список сообщений.
     */
    public List<Message> getHistory(long chatId) {
        // Возвращаем историю или пустой список, если истории нет.
        return histories.getOrDefault(chatId, new LinkedList<>());
    }

    /**
     * Полностью очищает историю диалога для пользователя.
     * @param chatId ID чата пользователя.
     */
    public void clearHistory(long chatId) {
        histories.remove(chatId);
    }
}