package telegram.bot.Service.Workly;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

import static telegram.bot.Controller.Admins.Login.*;

@Service
@RequiredArgsConstructor
public class WorklyService {

    private final JdbcTemplate jdbcTemplate;

    public Map<String, Object> getWorklyControllerBattery() {

        return jdbcTemplate.queryForMap("SELECT * FROM " + SCHEME_NAME + ".workly_controller;");
    }

    public String storeWorklyControllerBattery(int batteryLevel) {

        if (batteryLevel > 0 && batteryLevel <= 100) {

            jdbcTemplate.update("UPDATE " + SCHEME_NAME + ".workly_controller SET battery_level = ?;", batteryLevel);

            return "You Successfully Updated Battery Status!";
        }
        else {

            throw new IllegalArgumentException("Battery Level Must Be From 0-100!");
        }
    }

    public String createWorklyAccount(String username, String password) {

        try {

            if (username.contains(SCHEME_NAME + "_")) {

                String sqlCreate = "INSERT INTO " + SCHEME_NAME + ".employees(username, password, role) Values(?, ?, ?);";
                jdbcTemplate.update(sqlCreate, username, password, "ROLE_WORKLY");

                return "You Successfully Created Workly Account!";
            }
            else {

                throw new IllegalArgumentException("Username Should Start With " + SCHEME_NAME + "_{username}. \nExample: " + SCHEME_NAME + "_Akiko18");
            }
        }
        catch (DataIntegrityViolationException e) {

            throw new IllegalArgumentException("This Username Already Exists!");
        }
    }
}