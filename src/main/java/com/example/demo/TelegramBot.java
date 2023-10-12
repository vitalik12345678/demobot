package com.example.demo;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;

    private static Map<Long, Map<String, Float>> cryptoMap = new HashMap<>();
    private static Map<Long, Map<String, Float>> initialCryptoRates = new HashMap<>();
    private static final long FIXED_DELAY_MS = 60000; // Update rates every 60 seconds
    private static final float PERCENTAGE_CHANGE_THRESHOLD = 0.000001f; // Set your desired percentage change threshold (e.g., 0.0001%)



    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if ("/start".equals(messageText)) {
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                try {
                    initialCryptoRates.put(chatId, CurrencyService.getCurrencyRates());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!" + "\n";
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            handleTelegramApiError(chatId);
        }
    }

    @Scheduled(fixedDelay = FIXED_DELAY_MS)
    public void updateCurrencyRates() {
        // Send "Rates updating" message once before the loop
        for (Long chatId : initialCryptoRates.keySet()) {
            sendMessage(chatId, "Rates updating");
        }

        for (Long chatId : initialCryptoRates.keySet()) {
            try {
                cryptoMap.put(chatId, CurrencyService.getCurrencyRates());
                checkAndNotifyUsers(chatId);
            } catch (IOException e) {
                handleCurrencyServiceError(chatId);
            }
        }
    }

    private void checkAndNotifyUsers(Long chatId) {
        Map<String, Float> currentRates = cryptoMap.get(chatId);
        Map<String, Float> initialRates = initialCryptoRates.get(chatId);

        if (initialRates != null) {
            for (String symbol : currentRates.keySet()) {
                float currentRate = currentRates.get(symbol);
                float initialRate = initialRates.get(symbol);

                float percentageChange = Math.abs((currentRate - initialRate) / initialRate);

                if (percentageChange > PERCENTAGE_CHANGE_THRESHOLD) {
                    String message = "Price change alert for " + symbol + ": " +
                            (currentRate > initialRate ? "increased" : "decreased") +
                            " by " + (percentageChange * 100) + "% since start.";
                    sendMessage(chatId, message);
                }
            }
        }
    }
    private void handleTelegramApiError(Long chatId) {
        String errorMessage = "An error occurred while processing your request. Please try again later.";
        sendMessage(chatId, errorMessage);
    }

    private void handleCurrencyServiceError(Long chatId) {
        String errorMessage = "An error occurred while retrieving currency rates. Please try again later.";
        sendMessage(chatId, errorMessage);
    }

}