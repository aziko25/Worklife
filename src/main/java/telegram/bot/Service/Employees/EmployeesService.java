package telegram.bot.Service.Employees;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import telegram.bot.Models.Employees;

import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;
import static telegram.bot.Controller.Admins.Login.USERNAME;

@Service
@RequiredArgsConstructor
public class EmployeesService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public String updateWholeEmployeesData(Integer employeeId, Employees employee) throws SQLException {

        String username = jdbcTemplate.queryForObject("SELECT username FROM " + SCHEME_NAME + ".employees WHERE id = ?;",
                String.class, employeeId);

        if (employee.getRole() == null || (!employee.getRole().equalsIgnoreCase("ROLE_USER") &&
                !employee.getRole().equalsIgnoreCase("ROLE_ADMIN"))) {

            throw new IllegalArgumentException("Role Should Be ROLE_ADMIN or ROLE_USER!");
        }

        String sql = "UPDATE " + SCHEME_NAME + ".employees SET " +
                "workly_code = ?, workly_password = ?, password = ?, " +
                "role = ?, deleted = ?, lastname = ?, middlename = ?, firstname = ?, " +
                "mail = ?, birthdate = ?, arrival_time = ?, exit_time = ?, department_name = ? " +
                "WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setObject(1, employee.getWorklyCode(), Types.INTEGER);
            ps.setObject(2, employee.getWorklyPass(), Types.INTEGER);
            ps.setObject(3, employee.getPassword(), Types.VARCHAR);
            ps.setObject(4, employee.getRole().toUpperCase(), Types.VARCHAR);
            ps.setObject(5, employee.getDeleted(), Types.BOOLEAN);
            ps.setObject(6, employee.getLastname(), Types.VARCHAR);
            ps.setObject(7, employee.getMiddlename(), Types.VARCHAR);
            ps.setObject(8, employee.getFirstname(), Types.VARCHAR);
            ps.setObject(9, employee.getMail(), Types.VARCHAR);
            ps.setObject(10, employee.getBirthdate(), Types.DATE);
            ps.setObject(11, employee.getArrivalTime(), Types.TIME);
            ps.setObject(12, employee.getExitTime(), Types.TIME);
            ps.setObject(13, employee.getDepartmentName(), Types.VARCHAR);
            ps.setObject(14, employeeId, Types.INTEGER);

            ps.executeUpdate();
        }

        return "You Successfully Updated " + username + " Info!";
    }


    public List<Map<String, Object>> allEmployeesBirthdaysIn5Days() {

        LocalDate today = LocalDate.now();
        LocalDate fiveDaysLater = today.plusDays(5);

        String sql = "SELECT * FROM " + SCHEME_NAME + ".employees WHERE " +
                "EXTRACT(MONTH FROM birthdate) BETWEEN ? AND ? AND " +
                "EXTRACT(DAY FROM birthdate) BETWEEN ? AND ? ORDER BY id";

        return jdbcTemplate.queryForList(sql, today.getMonthValue(), fiveDaysLater.getMonthValue(), today.getDayOfMonth(), fiveDaysLater.getDayOfMonth());
    }

    public List<Map<String, Object>> allEmployeesBirthdaysByDateRange(LocalDate start, LocalDate end) {

        String sql = "SELECT * FROM " + SCHEME_NAME + ".employees WHERE " +
                "(EXTRACT(MONTH FROM birthdate) > ? OR (EXTRACT(MONTH FROM birthdate) = ? AND EXTRACT(DAY FROM birthdate) >= ?)) AND " +
                "(EXTRACT(MONTH FROM birthdate) < ? OR (EXTRACT(MONTH FROM birthdate) = ? AND EXTRACT(DAY FROM birthdate) <= ?))";

        return jdbcTemplate.queryForList(sql, start.getMonthValue(), start.getMonthValue(), start.getDayOfMonth(), end.getMonthValue(), end.getMonthValue(), end.getDayOfMonth());
    }

    public String setEmployeesBirthday(int id, LocalDate birthdate) {

        jdbcTemplate.update("UPDATE " + SCHEME_NAME + ".employees SET birthdate = ? WHERE id = ?;", birthdate, id);

        return "You Successfully Updated Employees Birthday!";
    }

    public String setEmployeesWorkingTimesGraphic(List<Integer> ids, LocalTime start, LocalTime end) {
        try {
            String idsString = ids.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            String updateSql = "UPDATE " + SCHEME_NAME + ".employees SET arrival_time = ?, exit_time = ? WHERE id IN (" + idsString + ");";
            jdbcTemplate.update(updateSql, start, end);

            return "You Successfully Updated Employees Working Times!";
        }
        catch (DataAccessException e) {
            throw new IllegalArgumentException("Employee Not Found!");
        }
    }

    @Transactional
    public String giveEmployeeTimeOff(TimeOff timeOff) {

        boolean done = false;

        if (timeOff.getEmployeeId() == null) {
            throw new IllegalArgumentException("Specify Employee Id!");
        }

        if (timeOff.getReasonId() == null) {
            throw new IllegalArgumentException("Specify Reason Id!");
        }

        String employeeIdCheckSql = "SELECT username FROM " + SCHEME_NAME + ".employees WHERE id = ?;";
        String employeeUsername = jdbcTemplate.queryForObject(employeeIdCheckSql, String.class, timeOff.getEmployeeId());

        if (employeeUsername == null || employeeUsername.isEmpty()) {
            throw new IllegalArgumentException("Employee Not Found!");
        }

        String reasonIdCheckSql = "SELECT * FROM " + SCHEME_NAME + ".time_off_reasons WHERE id = ?;";
        Map<String, Object> reason = jdbcTemplate.queryForObject(reasonIdCheckSql, new ColumnMapRowMapper(), timeOff.getReasonId());

        if (reason == null || reason.isEmpty()) {
            throw new IllegalArgumentException("Reason Is Not Found!");
        }

        String comments = null;
        if (timeOff.getComments() != null) {

            comments = timeOff.getComments();
        }

        if (timeOff.getStartDate() == null || timeOff.getEndDate() == null) {
            throw new IllegalArgumentException("Start Date And End Date Should Be Present!");
        }

        long numOfDays = ChronoUnit.DAYS.between(timeOff.getStartDate().toLocalDate(), timeOff.getEndDate().toLocalDate());

        for (long i = 0; i <= numOfDays; i++) {

            LocalDateTime currentDate = timeOff.getStartDate().plusDays(i);

            if (timeOff.getDayOffs() != null && timeOff.getDayOffs() && timeOff.getTimeOffs() != null && !timeOff.getTimeOffs()) {

                String insertSql = "INSERT INTO " + SCHEME_NAME + ".working_time(date, employee_name, time_off, time_off_reason_id, time_off_comments) VALUES (?, ?, ?, ?, ?);";
                jdbcTemplate.update(insertSql, currentDate, employeeUsername, true, reason.get("id"), comments);

                done = true;
            }

            if (timeOff.getTimeOffs() != null && timeOff.getTimeOffs() && timeOff.getDayOffs() != null && !timeOff.getDayOffs()) {

                if (timeOff.getDayArrivalTime() == null || timeOff.getDayExitTime() == null) {

                    throw new IllegalArgumentException("Specify Arrival Time And Exit Time Of Employee!");
                }

                String insertSql = "INSERT INTO " + SCHEME_NAME + ".working_time(date, employee_name, time_off, " +
                        "time_off_reason_id, expected_arrival_time, expected_exit_time, time_off_comments) VALUES (?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql, currentDate, employeeUsername, true, reason.get("id"),
                        timeOff.getDayArrivalTime(), timeOff.getDayExitTime(), comments);

                done = true;
            }
        }

        if (done) {
            return "You Successfully Set Time Off For " + employeeUsername;
        }
        else {
            throw new IllegalArgumentException("Instability Of Days And Times Offs!");
        }
    }

    public List<Map<String, Object>> employeesTimeOffs(Integer employeeId) {

        String username = jdbcTemplate.queryForObject("SELECT username FROM " + SCHEME_NAME + ".employees WHERE id = ?;", String.class, employeeId);

        String sql = "SELECT * FROM " + SCHEME_NAME + ".working_time WHERE employee_name = ? AND date >= current_date AND time_off = true;";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, username);

        // Adjust the time zone of the date field in each result
        for (Map<String, Object> result : results) {

            Timestamp timestamp = (Timestamp) result.get("date");

            LocalDateTime localDateTime = timestamp.toLocalDateTime();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String formattedDateTime = localDateTime.format(formatter);

            result.put("date", formattedDateTime);

            if (result.get("arrived_time") != null) {

                Timestamp timestamp1 = (Timestamp) result.get("arrived_time");

                LocalDateTime localDateTime1 = timestamp1.toLocalDateTime();

                String formattedDateTime1 = localDateTime1.format(formatter);

                result.put("arrived_time", formattedDateTime1);
            }

            if (result.get("exited_time") != null) {

                Timestamp timestamp2 = (Timestamp) result.get("exited_time");

                LocalDateTime localDateTime2 = timestamp2.toLocalDateTime();

                String formattedDateTime2 = localDateTime2.format(formatter);

                result.put("exited_time", formattedDateTime2);
            }
        }

        return results;
    }

    public String deleteEmployeesTimeOffs(Integer employeeId, List<LocalDateTime> dates) {

        if (dates.isEmpty()) {

            throw new IllegalArgumentException("Dates Can't Be Empty!");
        }

        String username = jdbcTemplate.queryForObject("SELECT username FROM " + SCHEME_NAME + ".employees WHERE id = ?;", String.class, employeeId);

        String inClause = String.join(",", Collections.nCopies(dates.size(), "?"));

        jdbcTemplate.update(
                "DELETE FROM " + SCHEME_NAME + ".working_time WHERE date IN (" + inClause + ") AND employee_name = ? AND time_off = true;",
                Stream.concat(dates.stream(), Stream.of(username)).toArray()
        );

        return "You Successfully Deleted " + username + " Time Offs!";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeOff {

        private Integer employeeId;
        private Integer reasonId;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startDate;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endDate;

        private Boolean dayOffs;
        private Boolean timeOffs;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime dayArrivalTime;

        @JsonFormat(pattern = "HH:mm")
        private LocalTime dayExitTime;

        private String comments;
    }
}