package telegram.bot.Controller.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/device_fetch")
public class FetchDevicesController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FetchDevicesController(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public String fetchDevices(@RequestParam String username, @RequestParam String fcm_token) {

        int index = username.lastIndexOf('_');

        String schema_name = username.substring(0, index);

        String sql = "SET SEARCH_PATH TO " + schema_name + ";";

        try {

            jdbcTemplate.update(sql);

            sql = "INSERT INTO " + schema_name + ".devices(username, fcm_token) VALUES (?, ?) ON CONFLICT (username) DO UPDATE SET fcm_token = EXCLUDED.fcm_token;";

            jdbcTemplate.update(sql, username, fcm_token);

            return "Device Successfully Fetched!";
        }
        catch (DataAccessException e) {

            return "Error: " + e.getMessage();
        }
    }
}
