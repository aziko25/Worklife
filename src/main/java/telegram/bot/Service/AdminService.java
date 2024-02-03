package telegram.bot.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;
import static telegram.bot.Controller.Admins.Login.USERNAME;
import static telegram.bot.Service.Users.TasksService.formatDates;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final JdbcTemplate jdbcTemplate;
    //private final TelegramLongPollingBot telegramLongPollingBot;


    // EMPLOYEES

    public List<Map<String, Object>> seeEmployees(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".employees WHERE deleted = false ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeDeletedEmployees(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".employees WHERE deleted = true ORDER BY id";

        return selectAll(sql);
    }

    public void createEmployee(String schema_name, String username, String password, String role, int worklyCode, int worklyPass,
                               LocalTime arrivalTime, LocalTime exitTime) {

        String checkSql = "SELECT COUNT(*) FROM " + schema_name + ".employees WHERE username = ? AND workly_code = ?;";

        Integer count = jdbcTemplate.query(checkSql, new Object[]{username, worklyCode}, rs -> rs.next() ? rs.getInt(1) : 0);

        if (count != null && count > 0) {

            throw new IllegalArgumentException("Username Or Workly Code already exists");
        }
        else {

            String insertSql = "INSERT INTO " + schema_name + ".employees (username, password, role, workly_code, workly_password, arrival_time, exit_time) VALUES (?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSql, username, password, role, worklyCode, worklyPass, arrivalTime, exitTime);
        }
    }

    public void deleteEmployee(String schema_name, String username) {

        String checkSql = "SELECT COUNT(*) FROM " + schema_name + ".employees WHERE username = ?";

        Integer count = jdbcTemplate.queryForObject(checkSql, new Object[]{username}, Integer.class);

        if (count == null || count == 0) {

            throw new IllegalArgumentException("Username Does Not Exist");
        }
        else {

            String sql = "DELETE FROM " + schema_name + ".employees WHERE username = ?";

            jdbcTemplate.update(sql, username);
        }
    }


    public void giveAdmin(String schema_name, String username) throws Exception {

        String sql = "UPDATE " + schema_name + ".employees SET role = 'ROLE_ADMIN' WHERE username = ?";

        int rowsAffected = jdbcTemplate.update(sql, username);

        if (rowsAffected == 0) {

            throw new Exception("No Such Username: " + username);
        }
    }


    public void changeUsernameAndPassword(String schema_name, String username, String newUsername, String newPassword) {

        String sql = "UPDATE " + schema_name + ".employees SET username = ?, password = ? WHERE username = ?";

        jdbcTemplate.update(sql, newUsername, newPassword, username);
    }

    // TASKS

    public List<Map<String, Object>> seeAllTasksToday(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE date_trunc('day', creation_date) = current_date ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeAllTasksThisWeek(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE creation_date BETWEEN date_trunc('week', current_date + interval '1 day') - interval '1 day' AND date_trunc('week', current_date + interval '1 day') + interval '5 days' ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeAllTasksThisMonth(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE creation_date BETWEEN date_trunc('month', current_date) AND date_trunc('month', current_date) + interval '1 month' - interval '1 day' ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeAllTasksAnyDay(String schema_name, LocalDate done) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE date_trunc('day', creation_date) = ? ORDER BY id";

        return selectAllByLocalDate(sql, done);
    }

    public List<Map<String, Object>> seeAllTasksRange(String schema_name, LocalDate start, LocalDate end) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE date_trunc('day', creation_date) >= ? AND date_trunc('day', creation_date) <= ? ORDER BY id";

        return selectByUsernameAndDateRange(sql, start, end);
    }

    public List<Map<String, Object>> seeAllTasks(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeAudioById(String schema_name, int id) {

        String sql = "SELECT * FROM " + schema_name + ".audio WHERE id = ?;";

        List<Map<String, Object>> audio = jdbcTemplate.queryForList(sql, id);

        if (audio.isEmpty()) {

            throw new NoSuchElementException("No audio found with id: " + id);
        }

        return audio;
    }

    public List<Map<String, Object>> seeAllTasksOfEmployeeToday(String schema_name, String responsible_name) {

        try {

            String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND date_trunc('day', creation_date) = current_date ORDER BY id";

            List<Map<String, Object>> result = selectByUsername(sql, responsible_name);

            if (result.isEmpty()) {

                throw new Exception("No tasks found for responsible name: " + responsible_name);
            }

            return result;
        }
        catch (Exception e) {

            e.printStackTrace();

            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> seeAllTasksOfEmployeeThisWeek(String schema_name, String responsible_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN date_trunc('week', current_date + interval '1 day') - interval '1 day' AND date_trunc('week', current_date + interval '1 day') + interval '5 days' ORDER BY id";

        return selectByUsername(sql, responsible_name);
    }

    public List<Map<String, Object>> seeAllTasksOfEmployeeThisMonth(String schema_name, String responsible_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN date_trunc('month', current_date) AND date_trunc('month', current_date) + interval '1 month' - interval '1 day' ORDER BY id";

        return selectByUsername(sql, responsible_name);
    }

    public List<Map<String, Object>> seeAllTasksOfEmployeeAnyDay(String schema_name, String responsible_name, LocalDate done) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND date_trunc('day', creation_date) = ? ORDER BY id";

        return selectByUsernameAndDate(sql, responsible_name, done);
    }

    public List<Map<String, Object>> seeAllTasksOfEmployeeRange(String schema_name, String responsible_name, LocalDate start, LocalDate end) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND date_trunc('day', creation_date) >= ? AND date_trunc('day', creation_date) <= ? ORDER BY id";

        return selectByUsernameAndDateRange(sql, responsible_name, start, end);
    }

    public List<Map<String, Object>> seeAllUnCompletedTasksOfEmployee(String schema_name, String responsible_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND completed = false ORDER BY id";

        return selectByUsername(sql, responsible_name);
    }

    public List<Map<String, Object>> seeAllUnCompletedTasksOfAllEmployees(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE completed = false OR done IS NULL ORDER BY id";

        return selectAll(sql);
    }

    public void createTask(String schema_name, String description, String responsible_name, MultipartFile audioFile) throws IOException {

        String sql = "SELECT COUNT(*) FROM " + schema_name + ".employees WHERE username = ?";

        Integer count = jdbcTemplate.queryForObject(sql, new Object[]{responsible_name}, Integer.class);

        if (count == null || count == 0) {
            throw new IllegalArgumentException("Username Does Not Exist");
        }

        LocalDate time = LocalDate.now();
        boolean completed = false;

        byte[] audioData = null;
        if (audioFile != null && !audioFile.isEmpty()) {
            audioData = audioFile.getBytes();
        }

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(Objects.requireNonNull(jdbcTemplate.getDataSource()));

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        Integer id = null;

        if (audioData != null) {
            parameters.addValue("audio", new SqlLobValue(audioData), Types.BLOB);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(
                    "INSERT INTO " + schema_name + ".audio (audio) VALUES (:audio)",
                    parameters,
                    keyHolder
            );

            Map<String, Object> keys = keyHolder.getKeys();
            assert keys != null;
            id = (Integer) keys.get("id");
        }

        sql = "INSERT INTO " + schema_name + ".tasks (description, responsible_name, completed, appointed, creation_date, audio_id) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, description, responsible_name, completed, USERNAME, time, id);

        sql = "SELECT chat_id FROM " + schema_name + ".employees WHERE username = ?";

        /*Long chat_id = jdbcTemplate.queryForObject(sql, new Object[]{responsible_name}, Long.class);

        if (chat_id != null) {

            String messageText = "Вам Поставлена Новая Задача: " + description + "\nОтветственный: " + responsible_name;

            SendMessage message = new SendMessage();

            message.setChatId(chat_id);
            message.setText(messageText);

            try {

                telegramLongPollingBot.execute(message);
            }
            catch (TelegramApiException e) {

                e.printStackTrace();
            }
        }*/
    }

    public String updateTask(int id, String description, MultipartFile audioFile) throws Exception {

        if (description != null && !description.isEmpty()) {

            String sql = "UPDATE " + SCHEME_NAME + ".tasks SET description = ? WHERE id = ?";

            int rowsAffected = jdbcTemplate.update(sql, description, id);

            if (rowsAffected == 0) {

                throw new Exception("Task with id " + id + " not found.");
            }
        }

        if (audioFile != null && !audioFile.isEmpty()) {

            String query_id = "SELECT audio_id FROM " + SCHEME_NAME + ".tasks WHERE id = ?";
            Integer audio_id;

            try {

                audio_id = jdbcTemplate.queryForObject(query_id, new SingleColumnRowMapper<>(Integer.class), id);
            }
            catch (EmptyResultDataAccessException e) {

                throw new Exception("Task Not found.");
            }

            // Convert MultipartFile to byte array
            byte[] audioBytes = audioFile.getBytes();

            String sql = "UPDATE " + SCHEME_NAME + ".audio SET audio = ? WHERE id = ?";

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setBytes(1, audioBytes);
                ps.setInt(2, audio_id);
                return ps;
            });

        }

        return "Task " + id + " Successfully Updated!";
    }


    // WORKING TIME
    public List<Map<String, Object>> seeWorkingTimeToday(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date_trunc('day', date) = current_date ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> seeWorkingTimeThisWeek(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date BETWEEN date_trunc('week', current_date + interval '1 day') - interval '1 day' AND date_trunc('week', current_date + interval '1 day') + interval '5 days' ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> seeWorkingTimeThisMonth(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date BETWEEN date_trunc('month', current_date) AND date_trunc('month', current_date) + interval '1 month' - interval '1 day' ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> seeWorkingTimeAnyDay(String schema_name, String username, LocalDate date) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date_trunc('day', date) = ? ORDER BY id";

        return selectByUsernameAndDate(sql, username, date);
    }

    public List<Map<String, Object>> seeWorkingTimeBetweenDates(String schema_name, String username, LocalDate startDate, LocalDate endDate) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date_trunc('day', date) >= ? AND date_trunc('day', date) <= ? ORDER BY id";

        return selectByUsernameAndDateRange(sql, username, startDate, endDate);
    }

    public List<Map<String, Object>> seeWorkingTimeOfAllEmployeesToday(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE date_trunc('day', date) = current_date ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeWorkingTimeOfAllEmployeesThisWeek(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE date BETWEEN date_trunc('week', current_date + interval '1 day') - interval '1 day' AND date_trunc('week', current_date + interval '1 day') + interval '5 days' ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeWorkingTimeOfAllEmployeesThisMonth(String schema_name) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE date BETWEEN date_trunc('month', current_date) AND date_trunc('month', current_date) + interval '1 month' - interval '1 day' ORDER BY id";

        return selectAll(sql);
    }

    public List<Map<String, Object>> seeWorkingTimeOfAllEmployeesAnyDay(String schema_name, LocalDate date) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE date_trunc('day', date) = ? ORDER BY id";

        return selectAllByLocalDate(sql, date);
    }

    public List<Map<String, Object>> seeWorkingTimeOfAllEmployeesBetweenDates(String schema_name, LocalDate startDate, LocalDate endDate) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE date_trunc('day', date) >= ? AND date_trunc('day', date) <= ? ORDER BY id";

        return selectByUsernameAndDateRange(sql, startDate, endDate);
    }

    public String startCheckTask(String schema_name, int task_id) throws Exception {

        String sql = "UPDATE " + schema_name + ".tasks SET beingChecked = true WHERE id = ? AND completed = true;";

        int rows = jdbcTemplate.update(sql, task_id);

        if (rows == 0) {

            throw new Exception("Id Not Found or Task Not Completed Yet!");
        }

        return "You Started Checking Task";
    }

    public void gradeTask(String schema_name, int grade, int id) throws Exception {

        String sql = "UPDATE " + schema_name + ".tasks SET grade = ? WHERE id = ? AND completed = true";

        int rows = jdbcTemplate.update(sql, grade, id);

        if (rows == 0) {

            throw new Exception("Id Not Found or Task Not Completed Yet!");
        }
    }

    public String getFcmToken(String schemeName, String username) {

        String sql = "SELECT fcm_token FROM " + schemeName + ".devices WHERE username = ?";

        try {

            return jdbcTemplate.queryForObject(sql, new Object[]{username}, String.class);
        }
        catch (EmptyResultDataAccessException e) {

            return null;
        }
    }

    public List<Map<String, Object>> selectByUsername(String sql, String username) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, username);

        return formatDate(rows);
    }

    public List<Map<String, Object>> selectByUsernameAndDate(String sql, String username, LocalDate date) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, username, date);

        return formatDate(rows);
    }

    public List<Map<String, Object>> selectByUsernameAndDateRange(String sql, String username, LocalDate startDate, LocalDate endDate) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, username, startDate, endDate);

        return formatDate(rows);
    }

    public List<Map<String, Object>> selectByUsernameAndDateRange(String sql, LocalDate startDate, LocalDate endDate) {

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startDate, endDate);

        return formatDate(rows);
    }

    private List<Map<String, Object>> selectAll(String sql) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return formatDate(rows);
    }

    private List<Map<String, Object>> selectAllByLocalDate(String sql, LocalDate date) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, date);
        return formatDate(rows);
    }

    private List<Map<String, Object>> formatDate(List<Map<String, Object>> rows) {

        return formatDates(rows);
    }
}