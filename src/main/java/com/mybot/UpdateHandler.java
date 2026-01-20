package com.mybot;

// Импортируем классы для работы с Telegram API.
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
// Импортируем утилиты для работы со списками.
import java.util.Arrays;
import java.util.List;

/**
 * Этот класс — мозг бота. Он принимает все обновления от Telegram,
 * анализирует их и решает, какой сервис вызвать.
 */
public class UpdateHandler {
    private final FitnessBot bot;
    private final GeminiService geminiService;
    private final StatisticsService statisticsService;
    private final NotificationService notificationService;
    // --- ИЗМЕНЕНИЕ 1: Добавляем новый сервис ---
    private final ConversationHistoryService conversationHistoryService;
    private final List<String> botNames = Arrays.asList("друг", "тренер", "помощник", "помошник");

    // --- ИЗМЕНЕНИЕ 2: Обновляем конструктор ---
    public UpdateHandler(FitnessBot bot, GeminiService geminiService, StatisticsService statisticsService, NotificationService notificationService, ConversationHistoryService conversationHistoryService) {
        this.bot = bot;
        this.geminiService = geminiService;
        this.statisticsService = statisticsService;
        this.notificationService = notificationService;
        this.conversationHistoryService = conversationHistoryService;
    }

    public void handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatIdLong = update.getMessage().getChatId();
            String chatId = String.valueOf(chatIdLong);
            String messageText = update.getMessage().getText();

            // 1. Извлекаем никнейм (он может быть null, если у пользователя нет @username)
            String username = update.getMessage().getFrom().getUserName();

            notificationService.recordUserActivity(chatId);

            // 2. Передаем username в метод лога (теперь там 3 аргумента)
            bot.logMessage(chatId, username, messageText);

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            boolean shouldSendMessage = false;

            // --- ИЗМЕНЕНИЕ 3: Добавляем обработку новых команд ---
            if ("/start".equals(messageText)) {
                conversationHistoryService.clearHistory(chatIdLong); // Очищаем историю при старте
                message.setText("Привет \uD83E\uDDD8  Я — не настоящий фитнесс-тренер, я здесь чтобы мотивировать заниматься спортом и просто подержать в трудные моменты жизни! Задавай мне любой вопрос, и я дам тебе напутствие. Я помню наш разговор, но если захочешь начать сначала, просто напиши /newchat.");
                shouldSendMessage = true;
            } else if ("/stats".equals(messageText)) {
                message.setText(statisticsService.getStatsMessage(chatId));
                shouldSendMessage = true;
            } else if ("/newchat".equals(messageText)) {
                conversationHistoryService.clearHistory(chatIdLong);
                message.setText("Контекст диалога очищен. Начинаем с чистого листа!");
                shouldSendMessage = true;
            } else {
                String question = getQuestionFromMessage(update);
                if (question != null && !question.isEmpty()) {
                    // --- ИЗМЕНЕНИЕ 4: Логика работы с контекстом ---
                    String systemPrompt = "Ты — не фитнесс-тренер, ты не можешь давать советы по здоровью или занятий спором, но ты мудрый и весёлый проводник, помогающий людям прийти в спорт и поддерживающий их стремления в сложные моменты. Поддерживай их, но не давай рекомендаций или советов про спорт, а если спросят, то ненавязчиво направляй к фитнес-тренеру @Daniella_neuro. Ты отвечаешь на вопросы пользователей. Твои ответы должны быть короткими (одно-два предложения), остроумными, с добрым юмором, вдохновляюще. Говори от первого лица, по-дружески";

                    // Добавляем системный промпт и вопрос пользователя в историю
                    conversationHistoryService.addMessage(chatIdLong, new Message("user", systemPrompt + " Вот вопрос пользователя: \"" + question + "\""));

                    // Получаем всю историю
                    List<Message> history = conversationHistoryService.getHistory(chatIdLong);

                    // Отправляем историю в Gemini
                    String support = geminiService.getSupport(history);

                    // Добавляем ответ модели в историю
                    conversationHistoryService.addMessage(chatIdLong, new Message("model", support));

                    message.setText(support);
                    statisticsService.updateStats(chatId);
                    shouldSendMessage = true;
                }
            }

            if (shouldSendMessage) {
                message.setText(message.getText() + "\uD83C\uDFC6");
                bot.sendMessage(message);
            }
        }
    }

    /**
     * Вспомогательный метод для извлечения "чистого" вопроса из сообщения.
     * @param update Объект обновления.
     * @return Текст вопроса или null, если сообщение не адресовано боту в группе.
     */
    private String getQuestionFromMessage(Update update) {
        String messageText = update.getMessage().getText();
        if (update.getMessage().isGroupMessage() || update.getMessage().isSuperGroupMessage()) {
            String lowerText = messageText.toLowerCase();
            for (String botName : botNames) {
                if (lowerText.startsWith(botName)) {
                    // "Отрезаем" имя бота от начала сообщения.
                    return messageText.substring(botName.length()).trim();
                }
            }
            return null; // В группе сообщение не для нас.
        } else {
            return messageText; // В личных сообщениях любое сообщение — это вопрос.
        }
    }
}