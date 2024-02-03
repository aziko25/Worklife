package telegram.bot;
/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import telegram.bot.Models.Tasks;
import telegram.bot.Models.WorkingTime;
import telegram.bot.Service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MainBot extends TelegramLongPollingBot {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean waitingForUsername = false;
    private boolean waitingForPassword = false;
    private boolean waitingForTasks = false;
    private boolean waitingForWorkingTime = false;
    private boolean waitingForTaskId = false;
    private boolean waitingForTaskDescription = false;
    private boolean waitingForTaskDate = false;
    private boolean waitingForTaskDateRange = false;
    private boolean waitingForWorkingTimeRange = false;

    private Map<Long, String> usernames = new HashMap<>();
    private Map<Long, String> passwords = new HashMap<>();

    private String username;
    private String password;
    public static String schemeName;

    @Override
    public void onUpdateReceived(Update update) {

        if ((update.hasMessage() && update.getMessage().hasText()) || update.hasMessage() && update.getMessage().hasPhoto()) {

            String messageText = update.getMessage().getText();

            if (messageText == null) {

                messageText = String.valueOf(update.getMessage());
            }

            long chatId = update.getMessage().getChatId();

            System.out.println("messageText: " + messageText);

            if ("/start".equals(messageText)) {

                start(chatId);
            }
            else if (!messageText.isEmpty() && waitingForUsername) {

                usernames.put(chatId, messageText);
                askForPassword(chatId);
            }
            else if (!messageText.isEmpty() && waitingForPassword) {

                passwords.put(chatId, messageText);
                checkCredentials(chatId);
            }
            else if ((messageText.equals("Задачи") && waitingForTasks) ||
                    (messageText.equals("Время Работы") && waitingForWorkingTime) ||
                    messageText.equals("Выполнить Задачу") ||
                    messageText.equals("Добавить Свою Выполненную Задачу") ||
                    messageText.equals("Назад") ||
                    messageText.equals("Пришел") ||
                    messageText.equals("Ушел") ||
                    messageText.equals("Диапазон Рабочего Времени") ||
                    messageText.equals("Часы Работы Сегодня") ||
                    messageText.equals("Часы Работы За Неделю") ||
                    messageText.equals("Часы Работы За Месяц")) {

                handleMenuSelection(chatId, messageText);
            }
            else if (messageText.equals("Задачи Сегодня") ||
                    messageText.equals("Задачи За Неделю") ||
                    messageText.equals("Задачи За Месяц") ||
                    messageText.equals("Задачи За Год") ||
                    messageText.equals("Задачи За Любой День") ||
                    messageText.equals("Диапазон Задач")) {

                handleMenuSelection(chatId, messageText);
            }
            else if (waitingForTaskId) {

                completeSelectedTask(chatId, messageText);
            }
            else if (waitingForTaskDescription) {

                addCompletedTask(chatId, messageText);
            }
            else if (waitingForTaskDate) {

                showTasksForDate(chatId, messageText);
            }
            else if (waitingForTaskDateRange) {

                showTasksForDateRange(chatId, messageText);
            }
            else if (waitingForWorkingTimeRange) {

                showWorkingTimeForDateRange(chatId, messageText);
            }
        }
    }

    public void start(long chatId) {

        SendMessage message = new SendMessage();

        waitingForUsername = false;
        waitingForPassword = false;
        waitingForTasks = false;
        waitingForWorkingTime = false;

        message.setChatId(chatId);
        askForUsername(chatId);
    }

    public void askForUsername(long chatId) {

        waitingForUsername = true;

        SendMessage message = new SendMessage();

        keyBoardRemove(message);

        message.setChatId(chatId);
        message.setText("Введите Имя Пользователя:");

        exception(message);
    }

    public void askForPassword(long chatId) {

        waitingForUsername = false;
        waitingForPassword = true;

        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText("Введите Пароль:");

        exception(message);
    }

    public void checkCredentials(long chatId) {

        waitingForPassword = false;

        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        username = usernames.get(chatId);
        password = passwords.get(chatId);

        if (checkUserInDatabase(chatId, username, password)) {

            message.setText("Успешная Авторизация!");
            exception(message);

            showMenu(chatId);
        }
        else {
            message.setText("Неправильный Логин Или Пароль");
            exception(message);

            askForUsername(chatId);
        }
    }

    public void showMenu(long chatId) {

        username = usernames.get(chatId);

        String sql = "UPDATE " + schemeName + ".employees SET chat_id = ? WHERE username = ?";
        jdbcTemplate.update(sql, chatId, username);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Задачи");
        row.add("Время Работы");

        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText("Выберите Одно:");
        message.setReplyMarkup(keyboardMarkup);

        waitingForTasks = true;
        waitingForWorkingTime = true;

        exception(message);
    }

    @Autowired
    private UserService userService;

    public void handleMenuSelection(long chatId, String selection) {

        switch (selection) {

            case "Задачи" -> {

                username = usernames.get(chatId);

                List<Tasks> tasks = userService.myTasksToday(schemeName, username);

                StringBuilder sb = new StringBuilder();

                sb.append("Ваши Задачи За Сегодня:\n\n");

                int taskNumber = 1;
                for (Tasks task : tasks) {

                    sb.append("----------------------\n");
                    sb.append(taskNumber).append(") ID: ");
                    sb.append(task.getId()).append("\nЗадача: ");
                    sb.append(task.getDescription()).append("\nВыполнена: ");
                    sb.append(task.getComplete() ? "True" : "False").append("\nВыполненное Время: ");
                    sb.append(task.getDone()).append("\nВремя Постановления Задачи: ");
                    sb.append(task.getCreation_date());
                    sb.append("\n----------------------\n\n");

                    taskNumber++;
                }

                SendMessage message = new SendMessage();

                message.setChatId(chatId);
                message.setText(sb.toString());

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();

                row.add("Выполнить Задачу");
                row.add("Добавить Свою Выполненную Задачу");
                keyboard.add(row);

                KeyboardRow row2 = new KeyboardRow();

                row2.add("Задачи Сегодня");
                row2.add("Задачи За Неделю");
                row2.add("Задачи За Месяц");
                keyboard.add(row2);

                KeyboardRow row3 = new KeyboardRow();

                row3.add("Задачи За Год");
                row3.add("Задачи За Любой День");
                row3.add("Диапазон Задач");
                keyboard.add(row3);

                KeyboardRow row4 = new KeyboardRow();

                row4.add("Назад");
                keyboard.add(row4);

                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);

                exception(message);
            }
            case "Выполнить Задачу" -> {

                username = usernames.get(chatId);

                List<Tasks> tasks = userService.myTasksToday(schemeName, username);

                StringBuilder sb = new StringBuilder();

                int count = 0;

                for (Tasks task : tasks) {

                    if (!task.getComplete()) {

                        sb.append("----------------------\n");
                        sb.append("ID: ");
                        sb.append(task.getId()).append("\nЗадача: ");
                        sb.append(task.getDescription()).append("\nВыполнена: ");
                        sb.append(task.getComplete() ? "True" : "False").append("\nВыполненное Время: ");
                        sb.append(task.getDone()).append("\nВремя Постановления Задачи: ");
                        sb.append(task.getCreation_date());
                        sb.append("\n----------------------\n\n");

                        count++;
                    }
                }


                SendMessage message = new SendMessage();

                message.setChatId(chatId);
                message.setText(sb.toString());

                exception(message);

                if (count > 0) {

                    message.setText("\nВпишите ID Задачи, Которую Хотите Выполнить: ");

                    waitingForTaskId = true;
                }
                else {

                    message.setText("Вы Выполнили Все Постановленные Задачи!");
                }

                exception(message);
            }
            case "Добавить Свою Выполненную Задачу" -> {

                SendMessage message = new SendMessage();

                message.setChatId(chatId);
                message.setText("Напишите О Задаче, Которую Выполнили: ");

                exception(message);

                waitingForTaskDescription = true;
            }
            case "Назад" -> {

                showMenu(chatId);
            }
            case "Время Работы" -> {

                SendMessage message = new SendMessage();

                message.setChatId(chatId);
                message.setText("Часы Работы");

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                keyboardMarkup.setResizeKeyboard(true);
                List<KeyboardRow> keyboard = new ArrayList<>();

                KeyboardRow row2 = new KeyboardRow();
                row2.add("Часы Работы Сегодня");
                row2.add("Часы Работы За Неделю");
                row2.add("Часы Работы За Месяц");
                keyboard.add(row2);

                KeyboardRow row4 = new KeyboardRow();

                row4.add("Диапазон Рабочего Времени");
                keyboard.add(row4);

                KeyboardRow row3 = new KeyboardRow();

                row3.add("Назад");

                keyboard.add(row3);


                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);

                exception(message);
            }
            case "Часы Работы Сегодня" -> {

                username = usernames.get(chatId);

                List<WorkingTime> workingTimes = userService.myWorkingTimeToday(schemeName, username);
                showWorkingTime(chatId, workingTimes, "Сегодня");
            }
            case "Часы Работы За Неделю" -> {

                username = usernames.get(chatId);

                List<WorkingTime> workingTimes = userService.myWorkingTimeThisWeek(schemeName, username);
                showWorkingTime(chatId, workingTimes, "За Эту Неделю");
            }
            case "Часы Работы За Месяц" -> {

                username = usernames.get(chatId);

                List<WorkingTime> workingTimes = userService.myWorkingTimeThisMonth(schemeName, username);
                showWorkingTime(chatId, workingTimes, "За Этот Месяц");
            }
            case "Диапазон Рабочего Времени" -> {

                SendMessage message = new SendMessage();

                message.setChatId(chatId);
                message.setText("""
                        Напишите Период, За Который Хотите Увидеть Свои Рабочи Часы:\s
                        (ГГГГ-ММ-ДД до ГГГГ-ММ-ДД)
                        Пример: 2023-01-01 до 2024-01-01""");

                exception(message);

                waitingForWorkingTimeRange = true;
            }
            case "Задачи Сегодня" -> {

                username = usernames.get(chatId);

                List<Tasks> tasks = userService.myTasksToday(schemeName, username);
                displayTasks(chatId, tasks, "Сегодня");
            }
            case "Задачи За Неделю" -> {

                username = usernames.get(chatId);

                List<Tasks> tasks = userService.myTasksThisWeek(schemeName, username);
                displayTasks(chatId, tasks, "За Эту Неделю");
            }
            case "Задачи За Месяц" -> {

                username = usernames.get(chatId);

                List<Tasks> tasks = userService.myTasksThisMonth(schemeName, username);
                displayTasks(chatId, tasks, "За Этот Месяц");
            }
            case "Задачи За Год" -> {

                username = usernames.get(chatId);

                List<Tasks> tasks = userService.myTasksThisYear(schemeName, username);
                displayTasks(chatId, tasks, "За Этот Год");
            }
            case "Задачи За Любой День" -> {

                SendMessage message = new SendMessage();

                message.setChatId(chatId);
                message.setText("Напишите День, За Который Хотите Увидеть Задачи\n (ГГГГ-ММ-ДД). \nПример: 2023-08-16 ");

                exception(message);

                waitingForTaskDate = true; // Set waitingForTaskDate to true
            }
            case "Диапазон Задач" -> {

                SendMessage message = new SendMessage();

                message.setChatId(chatId);
                message.setText("""
                        Напишите Период, За Который Хотите Увидеть Свои Задачи:\s
                        (ГГГГ-ММ-ДД до ГГГГ-ММ-ДД)
                        Пример: 2023-01-01 до 2024-01-01""");

                exception(message);

                waitingForTaskDateRange = true;
            }
        }
    }

    public void showWorkingTime(long chatId, List<WorkingTime> workingTimes, String timePeriod) {

        StringBuilder sb = new StringBuilder();

        sb.append("Ваши Рабочие Часы ").append(timePeriod).append(":\n\n");

        int workingTimeNumber = 1;

        for (WorkingTime workingTime : workingTimes) {

            sb.append("----------------------\nNo: ").append(workingTimeNumber);
            sb.append("\nДата: ");

            if (workingTime.getDate() != null) {

                sb.append(workingTime.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
            else {
                sb.append("null");
            }

            sb.append("\nПриход: ");

            if (workingTime.getArrived_time() != null) {

                sb.append(workingTime.getArrived_time().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            else {
                sb.append("null");
            }

            sb.append("\nУход: ");

            if (workingTime.getExited_time() != null) {

                sb.append(workingTime.getExited_time().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            else {

                sb.append("null");
            }

            sb.append("\n----------------------\n\n");

            workingTimeNumber++;
        }

        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(sb.toString());

        exception(message);
    }

    public void displayTasks(long chatId, List<Tasks> tasks, String timePeriod) {

        StringBuilder sb = new StringBuilder();

        sb.append("Ваши Задачи ").append(timePeriod).append(":\n\n");
        int taskNumber = 1;

        for (Tasks task : tasks) {

            sb.append("----------------------\n");
            sb.append(taskNumber).append(") ID: ");
            sb.append(task.getId()).append("\nЗадача: ");
            sb.append(task.getDescription()).append("\nВыполнена: ");
            sb.append(task.getComplete() ? "True" : "False").append("\nВыполенное Время: ");
            sb.append(task.getDone()).append("\nВремя Постановления Задачи: ");
            sb.append(task.getCreation_date());
            sb.append("\n----------------------\n\n");

            taskNumber++;
        }

        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(sb.toString());

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("Выполнить Задачу");
        row.add("Добавить Свою Выполненную Задачу");

        KeyboardRow row2 = new KeyboardRow();

        row2.add("Задачи Сегодня");
        row2.add("Задачи За Неделю");
        row2.add("Задачи За Месяц");

        KeyboardRow row3 = new KeyboardRow();

        row3.add("Задачи За Год");
        row3.add("Задачи За Любой День");
        row3.add("Диапазон Задач");

        KeyboardRow row4 = new KeyboardRow();

        row4.add("Назад");

        keyboard.add(row);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);

        message.setReplyMarkup(keyboardMarkup);

        exception(message);
    }

    public void showTasksForDate(long chatId, String date) {

        username = usernames.get(chatId);

        try {

            LocalDate taskDate = LocalDate.parse(date);

            List<Tasks> tasks = userService.myTasksAnyDay(schemeName, username, taskDate);

            displayTasks(chatId, tasks, "За " + date);

            waitingForTaskDate = false;
        }
        catch (DateTimeParseException e) {

            SendMessage message = new SendMessage();

            message.setChatId(chatId);
            message.setText("Неверный формат даты. Пожалуйста, введите дату в формате ГГГГ-ММ-ДД. \nПример: 2023-08-16\nИли Впишите 'Назад' Чтобы Вернуться:");

            exception(message);
        }
    }

    public void showTasksForDateRange(long chatId, String dateRange) {

        try {

            username = usernames.get(chatId);

            String[] dates = dateRange.split(" до ");

            LocalDate startDate = LocalDate.parse(dates[0]);
            LocalDate endDate = LocalDate.parse(dates[1]);

            List<Tasks> tasks = userService.myTasksBetweenDates(schemeName, username, startDate, endDate);

            displayTasks(chatId, tasks, "Между " + dates[0] + " и " + dates[1]);

            waitingForTaskDateRange = false;
        }
        catch (Exception e) {

            SendMessage message = new SendMessage();

            message.setChatId(chatId);
            message.setText("""
                    Напишите Период, За Который Хотите Увидеть Свои Задачи:\s
                        (ГГГГ-ММ-ДД до ГГГГ-ММ-ДД)
                        Пример: 2023-01-01 до 2024-01-01
                        Или Напишите 'Назад', Чтобы Вернуться В Главное Меню.""");


            exception(message);
        }
    }

    public void showWorkingTimeForDateRange(long chatId, String dateRange) {

        try {

            username = usernames.get(chatId);

            String[] dates = dateRange.split(" до ");

            LocalDate startDate = LocalDate.parse(dates[0]);
            LocalDate endDate = LocalDate.parse(dates[1]);

            List<WorkingTime> workingTimes = userService.myWorkingTimeRange(schemeName, username, startDate, endDate);

            showWorkingTime(chatId, workingTimes, "Между " + dates[0] + " и " + dates[1]);

            waitingForWorkingTimeRange = false;
        }
        catch (Exception e) {

            SendMessage message = new SendMessage();

            message.setChatId(chatId);
            message.setText("""
                    Напишите Период, За Который Хотите Увидеть Свое Рабочее Время:\s
                        (ГГГГ-ММ-ДД до ГГГГ-ММ-ДД)
                        Пример: 2023-01-01 до 2024-01-01
                        Или Напишите 'Назад', Чтобы Вернуться В Главное Меню.""");

            exception(message);
        }
    }

    public void completeSelectedTask(long chatId, String taskId) {

        try {

            username = usernames.get(chatId);

            int id = Integer.parseInt(taskId);

            userService.completeTask(schemeName, LocalDateTime.now(), id, username);

            SendMessage message = new SendMessage();

            message.setChatId(chatId);
            message.setText("Задача С ID " + taskId + " Успешно Выполнена.");

            exception(message);

            waitingForTaskId = false;
        }
        catch (Exception e) {

            SendMessage message = new SendMessage();

            message.setChatId(chatId);
            message.setText("Впишите Правильное ID.");

            exception(message);
        }
    }

    public void addCompletedTask(long chatId, String description) {

        keyBoardRemove(new SendMessage());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime now1 = LocalDateTime.now();

        boolean completed = true;

        username = usernames.get(chatId);

        userService.createTask(schemeName, description, username, now, completed, now1);

        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText("Ваша Выполненная Задача: '" + description + "' Была Успешно Добавлена.");

        exception(message);

        waitingForTaskDescription = false;
    }

    public boolean checkUserInDatabase(long chatId, String username, String password) {

        username = usernames.get(chatId);

        if (username.contains("_")) {

            schemeName = username.substring(0, username.indexOf('_'));
            System.out.println(schemeName + " " + username + " " + password);

            String sql = "SELECT COUNT(*) FROM " + schemeName + ".employees WHERE username = ? AND password = ?";

            try {

                Integer count = jdbcTemplate.queryForObject(sql, new Object[]{username, password}, Integer.class);

                return count != null && count > 0;
            }
            catch (DataAccessException e) {

                return false;
            }
        }
        else {
            return false;
        }
    }

    public void exception(SendMessage message) {

        try {

            execute(message);
        }
        catch (TelegramApiException e) {

            e.printStackTrace();
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    public void keyBoardRemove(SendMessage message) {

        ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();

        keyboardRemove.setRemoveKeyboard(true);
        message.setReplyMarkup(keyboardRemove);
    }

    public MainBot(@Value("${bot.token}") String botToken) {

        super(botToken);
    }

    @Override
    public String getBotUsername() {

        //return "worklife_imaj_bot";
        //return "azizkhuja_test_bot";
        return "loko_loko_bot";
    }
}*/