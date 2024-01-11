package telegram.bot.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import telegram.bot.Models.Tasks;
import telegram.bot.Models.WorkingTime;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // TASKS

    public List<Tasks> myTasksToday(String schema_name, String username) {

        LocalDate now = LocalDate.now();

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND date_trunc('day', creation_date) = ?";

        return getTasksDate(username, sql, now);
    }



    private List<Tasks> getTasks(String username, String sql) {

        List<Tasks> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/worklife", "postgres", "bestuser");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            getTask(tasks, stmt);
        }
        catch (SQLException e) {

            e.printStackTrace();
        }

        return tasks;
    }

    private List<Tasks> getTasksDate(String username, String sql, LocalDate done) {

        List<Tasks> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/worklife", "postgres", "bestuser");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setObject(2, done);

            getTask(tasks, stmt);

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return tasks;
    }

    private void getTask(List<Tasks> tasks, PreparedStatement stmt) throws SQLException {

        try (ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Tasks task = new Tasks();

                task.setId(rs.getInt("id"));
                task.setDescription(rs.getString("description"));
                task.setResponsible_name(rs.getString("responsible_name"));
                task.setDone(rs.getDate("done"));
                task.setComplete(rs.getBoolean("completed"));
                task.setCreation_date(rs.getDate("creation_date"));

                tasks.add(task);
            }
        }
    }

    public List<Tasks> myTasksThisWeek(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN date_trunc('week', current_date + interval '1 day') - interval '1 day' AND date_trunc('week', current_date + interval '1 day') + interval '5 days'";

        return getTasks(username, sql);
    }


    public List<Tasks> myTasksThisMonth(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN date_trunc('month', current_date) AND date_trunc('month', current_date) + interval '1 month' - interval '1 day'";

        return getTasks(username, sql);
    }

    public List<Tasks> myTasksThisYear(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN date_trunc('year', current_date) AND date_trunc('year', current_date) + interval '1 year' - interval '1 day'";

        return getTasks(username, sql);
    }

    public List<Tasks> myTasksAnyDay(String schema_name, String username, LocalDate done) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND date_trunc('day', creation_date) = ?";

        return getTasksDate(username, sql, done);
    }

    public List<Tasks> myTasksBetweenDates(String schema_name, String username, LocalDate startDate, LocalDate endDate) {

        String sql = "SELECT * FROM " + schema_name + ".tasks WHERE responsible_name = ? AND creation_date BETWEEN ? AND ?";

        return getTasksBetweenDates(username, sql, startDate, endDate);
    }

    private List<Tasks> getTasksBetweenDates(String username, String sql, LocalDate startDate, LocalDate endDate) {

        List<Tasks> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/worklife", "postgres", "bestuser");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setObject(2, startDate);
            stmt.setObject(3, endDate);

            getTask(tasks, stmt);

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return tasks;
    }


    public void createTask(String schema_name, String description, String responsible_name, LocalDateTime now, Boolean completed, LocalDateTime now1) {

        String sql = "INSERT INTO " + schema_name + ".tasks (description, responsible_name, done, completed, creation_date) VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql, description, responsible_name, now, completed, now1);
    }

    public void completeTask(String schema_name, LocalDateTime done, int id, String username) {

        String sql = "UPDATE " + schema_name + ".tasks SET completed = true, done = ? WHERE id = ? AND responsible_name = ?";

        jdbcTemplate.update(sql, done, id, username);
    }

    // WORKING TIME

    public List<WorkingTime> myWorkingTimeToday(String schema_name, String username) {

        LocalDate now = LocalDate.now();

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date_trunc('day', date) = ?";

        return getWorkingTimeDate(sql, username, now);
    }

    private List<WorkingTime> getWorkingTimeDate(String sql, String username, LocalDate date) {

        List<WorkingTime> workingTimes = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/worklife", "postgres", "bestuser");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setObject(2, date);

            getTime(workingTimes, stmt);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return workingTimes;
    }

    public List<WorkingTime> myWorkingTimeThisWeek(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date BETWEEN date_trunc('week', current_date + interval '1 day') - interval '1 day' AND date_trunc('week', current_date + interval '1 day') + interval '5 days'";

        return getWorkingTimeThisWeek(sql, username);
    }

    public List<WorkingTime> getWorkingTimeThisWeek(String sql, String username) {

        List<WorkingTime> workingTimes = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/worklife", "postgres", "bestuser");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {

                ZoneId zoneId = ZoneId.of("Asia/Tashkent");

                while (rs.next()) {

                    Timestamp dateTimestamp = rs.getTimestamp("date");
                    ZonedDateTime date1 = dateTimestamp != null ? dateTimestamp.toLocalDateTime().atZone(zoneId) : null;

                    Timestamp arrivalTimeTimestamp = rs.getTimestamp("arrived_time");
                    ZonedDateTime arrivalTime = arrivalTimeTimestamp != null ? arrivalTimeTimestamp.toLocalDateTime().atZone(zoneId) : null;

                    Timestamp exitTimeTimestamp = rs.getTimestamp("exited_time");
                    ZonedDateTime exitTime = exitTimeTimestamp != null ? exitTimeTimestamp.toLocalDateTime().atZone(zoneId) : null;

                    WorkingTime workingTime = new WorkingTime(date1, arrivalTime, exitTime);
                    workingTimes.add(workingTime);
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return workingTimes;
    }

    public List<WorkingTime> myWorkingTimeThisMonth(String schema_name, String username) {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date BETWEEN date_trunc('month', current_date) AND date_trunc('month', current_date) + interval '1 month' - interval '1 day'";

        return getWorkingTimeThisWeek(sql, username);
    }

    public List<WorkingTime> myWorkingTimeRange(String schema_name, String username, LocalDate start, LocalDate end) throws SQLException {

        String sql = "SELECT * FROM " + schema_name + ".working_time WHERE employee_name = ? AND date BETWEEN ? AND ?";

        return selectRangeByUsernameAndDate(sql, username, start, end);
    }

    public void setArrivalTime(String schema_name, String username) {

        LocalDateTime now = LocalDateTime.now();
        Timestamp currentTimestamp = Timestamp.valueOf(now);

        String sql = "INSERT INTO " + schema_name + ".working_time (date, arrived_time, employee_name) VALUES (current_date, ?, ?)";

        jdbcTemplate.update(sql, currentTimestamp, username);
    }

    public void setExitTime(String schema_name, String username) {

        String sql = "UPDATE " + schema_name + ".working_time SET exited_time = current_timestamp WHERE employee_name = ? AND date = current_date";

        jdbcTemplate.update(sql, username);
    }

    public List<WorkingTime> selectRangeByUsernameAndDate(String sql, String username, LocalDate start, LocalDate end) throws SQLException {

        List<WorkingTime> workingTimes = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/worklife", "postgres", "bestuser");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setDate(2, Date.valueOf(start));
            stmt.setDate(3, Date.valueOf(end));

            getTime(workingTimes, stmt);
        }

        return workingTimes;
    }

    private void getTime(List<WorkingTime> workingTimes, PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                LocalDateTime date = rs.getTimestamp("date").toLocalDateTime();
                LocalDateTime arrivalTime = rs.getTimestamp("arrived_time") != null ? rs.getTimestamp("arrived_time").toLocalDateTime() : null;
                LocalDateTime exitTime = rs.getTimestamp("exited_time") != null ? rs.getTimestamp("exited_time").toLocalDateTime() : null;

                WorkingTime workingTime = new WorkingTime(date, arrivalTime, exitTime);

                workingTimes.add(workingTime);
            }
        }
    }
}