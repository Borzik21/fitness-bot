package com.mybot;

// Импортируем необходимые классы.
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Главный класс бота. Его единственные задачи:
 * 1. Инициализировать все сервисы.
 * 2. Получать обновления от Telegram и передавать их обработчику.
 * 3. Предоставлять другим сервисам метод для отправки сообщений.
 */
public class FitnessBot extends TelegramLongPollingBot {

    private final GeminiService geminiService;
    private final StatisticsService statisticsService;
    private final NotificationService notificationService;
    private final ConversationHistoryService conversationHistoryService;
    private final UpdateHandler updateHandler;

    public FitnessBot() {
        System.out.println("Инициализация сервисов бота...");

        this.geminiService = new GeminiService();
        this.statisticsService = new StatisticsService();
        // --- ИЗМЕНЕНИЕ 2: Инициализируем новый сервис ---
        this.conversationHistoryService = new ConversationHistoryService();
        this.notificationService = new NotificationService(this, geminiService);
        // --- ИЗМЕНЕНИЕ 3: Передаем новый сервис в UpdateHandler ---
        this.updateHandler = new UpdateHandler(this, geminiService, statisticsService, notificationService, conversationHistoryService);

        this.notificationService.start();
        System.out.println("Бот готов к работе!");
    }

    @Override
    public String getBotUsername() {
        return System.getenv("BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
    }

    /**
     * Этот метод вызывается библиотекой каждый раз, когда приходит новое сообщение.
     * Мы просто передаем всю работу нашему специализированному обработчику.
     */
    @Override
    public void onUpdateReceived(Update update) {
        updateHandler.handleUpdate(update);
    }

    /**
     * Публичный метод для отправки сообщений.
     * Его могут вызывать другие наши классы (например, NotificationService).
     * @param message Объект сообщения для отправки.
     */
    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (Exception e) {
            System.out.println("Ошибка при отправке сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Публичный метод для логирования.
     * @param chatId Чат, в котором произошло событие.
     * @param messageText Текст сообщения.
     */
    public void logMessage(String chatId, String username, String messageText) {
        String textToLog = (messageText == null) ? "[сообщение без текста]" : messageText;
        // Проверяем, есть ли ник, так как у некоторых пользователей его может не быть
        String userLog = (username != null) ? "@" + username : "[без ника]";

        System.out.println("Лог - ChatId: " + chatId + ", User: " + userLog + ", Сообщение: " + textToLog + ", Время: " + new java.util.Date());
    }
}