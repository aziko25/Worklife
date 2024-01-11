package telegram.bot.Service.Admins;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@Service
public class CompanyWorkingTimesService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CompanyWorkingTimesService(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public String insertWorkingTimes(LocalTime start, LocalTime end) {

        String insertSql = "INSERT INTO " + SCHEME_NAME + ".company_working_times(arrival_time, exit_time) VALUES(?, ?);";
        String updateSql = "UPDATE " + SCHEME_NAME + ".company_working_times SET arrival_time = ?, exit_time = ? WHERE id = 1;";

        int rowsUpdated = jdbcTemplate.update(updateSql, start, end);

        if (rowsUpdated == 0) {

            jdbcTemplate.update(insertSql, start, end);

            return "Successfully Set Working Times Of The Company!";
        }
        else {

            return "Successfully Updated Working Times Of The Company!";
        }
    }

    public List<Map<String, Object>> fetchCompanyWorkingTimes() {

        return jdbcTemplate.queryForList("SELECT * FROM " + SCHEME_NAME + ".company_working_times;");
    }
}
