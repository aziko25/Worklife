package telegram.bot.Service.Employees;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@Service
@RequiredArgsConstructor
public class EmployeesService {

    private final JdbcTemplate jdbcTemplate;

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
}