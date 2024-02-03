package telegram.bot.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@Service
@RequiredArgsConstructor
public class TimeOffService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> allReasons() {

        return jdbcTemplate.queryForList("SELECT * FROM " + SCHEME_NAME + ".time_off_reasons;");
    }

    public String createReason(String name) {

        jdbcTemplate.update("INSERT INTO " + SCHEME_NAME + ".time_off_reasons(name) VALUES (?);", name);

        return "You Successfully Created A " + name + " Reason!";
    }

    public String updateReason(Integer id, String name) {

        jdbcTemplate.update("UPDATE " + SCHEME_NAME + ".time_off_reasons SET name = ? WHERE id = ?;", name, id);

        return "You Successfully Updated A " + name + " Reason!";
    }

    public String deleteReason(Integer id) {

        jdbcTemplate.update("DELETE FROM " + SCHEME_NAME + ".time_off_reasons WHERE id = ?;", id);

        return "You Successfully Deleted A Reason!";
    }
}