package telegram.bot.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.MainBot;

@Configuration
public class Config {

    @Bean
    public TelegramBotsApi telegramBotsApi(MainBot mainBot) throws TelegramApiException {

        var api = new TelegramBotsApi(DefaultBotSession.class);

        api.registerBot(mainBot);

        return api;
    }
}