package telegram.bot.Service.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import static telegram.bot.Service.Users.WorkingTimeService.formattingDates;

@Service
public class TasksService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TasksService(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
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

    private List<Map<String, Object>> formatDate(List<Map<String, Object>> rows) {

        return formatDates(rows);
    }

    public static List<Map<String, Object>> formatDates(List<Map<String, Object>> rows) {

        return formattingDates(rows);
    }

    public void createTask(String schema, String description, String username, MultipartFile audioFile) throws IOException {

        LocalDateTime done = LocalDateTime.now();
        boolean completed = true;

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
                    "INSERT INTO " + schema + ".audio (audio) VALUES (:audio)",
                    parameters,
                    keyHolder
            );

            Map<String, Object> keys = keyHolder.getKeys();
            assert keys != null;
            id = (Integer) keys.get("id");
        }

        jdbcTemplate.update("INSERT INTO " + schema + ".tasks (description, responsible_name, done, completed, creation_date, audio_id) " +
                "VALUES (?, ?, ?, ?, ?, ?);", description, username, done, completed, done, id);
    }

    public List<Map<String, Object>> myTasksToday(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".tasks WHERE responsible_name = ? AND DATE(creation_date) = DATE(current_date) ORDER BY id";

        return selectByUsername(sql, username);
    }


    public List<Map<String, Object>> myTasksWeek(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN date_trunc('week', current_date) AND date_trunc('week', current_date) + interval '6 days' ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> myTasksMonth(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".tasks WHERE responsible_name = ? AND date_trunc('month', creation_date) = date_trunc('month', current_date) ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> myTasksRange(String schema, String username, LocalDate from, LocalDate to) {

        String sql = "SELECT * FROM " + schema + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN ? AND ? ORDER BY id";

        return selectByUsernameAndDateRange(sql, username, from, to);
    }

    public List<Map<String, Object>> allMyTasks(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".tasks WHERE responsible_name = ? ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> myTasksAnyDay(String schema, String username, LocalDate date) {

        String sql = "SELECT * FROM " + schema + ".tasks WHERE responsible_name = ? AND creation_date = ? ORDER BY id";

        return selectByUsernameAndDate(sql, username, date);
    }

    public List<Map<String, Object>> myUncompletedTasks(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".tasks WHERE responsible_name = ? AND completed = false ORDER BY id";

        return selectByUsername(sql, username);
    }
}