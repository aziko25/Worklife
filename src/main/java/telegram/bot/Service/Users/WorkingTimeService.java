package telegram.bot.Service.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
public class WorkingTimeService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public WorkingTimeService(JdbcTemplate jdbcTemplate) {

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

    public static List<Map<String, Object>> formattingDates(List<Map<String, Object>> rows) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tashkent"));

        String[] keys = {"done", "creation_date", "arrived_time", "exited_time", "date"};

        for (Map<String, Object> row : rows) {

            for (String key : keys) {

                if (row.containsKey(key)) {

                    Object value = row.get(key);

                    if (value != null) {

                        Timestamp timestamp = (Timestamp) value;
                        long timeInMillis = timestamp.getTime();

                        Date date = new Date(timeInMillis);
                        String formattedDate;

                        if (key.equals("date")) {

                            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                            formattedDate = sdf1.format(date);
                        }
                        else {

                            formattedDate = sdf.format(date);
                        }

                        row.put(key, formattedDate);
                    }
                }
            }
        }

        return rows;
    }

    public List<Map<String, Object>> myWorkingTimeToday(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".working_time WHERE employee_name = ? AND date = current_date ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> myWorkingTimeWeek(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".working_time WHERE employee_name = ? AND date BETWEEN date_trunc('week', current_date) AND date_trunc('week', current_date) + interval '6 days' ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> myWorkingTimeMonth(String schema, String username) {

        String sql = "SELECT * FROM " + schema + ".working_time WHERE employee_name = ? AND date_trunc('month', date) = date_trunc('month', current_date) ORDER BY id";

        return selectByUsername(sql, username);
    }

    public List<Map<String, Object>> myWorkingTimeAnyDay(String schema, String username, LocalDate date) {

        String sql = "SELECT * FROM " + schema + ".working_time WHERE employee_name = ? AND date = ? ORDER BY id";

        return selectByUsernameAndDate(sql, username, date);
    }

    public List<Map<String, Object>> myWorkingTimeRange(String schema, String username, LocalDate from, LocalDate to) {

        String sql = "SELECT * FROM " + schema + ".working_time WHERE employee_name = ? AND date BETWEEN ? AND ? ORDER BY id";

        return selectByUsernameAndDateRange(sql, username, from, to);
    }
}