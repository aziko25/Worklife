package telegram.bot.Service.Admins;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@Service
public class CompanyLocationService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CompanyLocationService(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public String insertLocation(Double longitude, Double latitude) {

        String insertQuery = "INSERT INTO " + SCHEME_NAME + ".location(longitude, latitude) VALUES (?, ?);";

        String updateQuery = "UPDATE " + SCHEME_NAME + ".location SET longitude = ?, latitude = ? WHERE id = 1;";

        int updatedRows = jdbcTemplate.update(updateQuery, longitude, latitude);

        if (updatedRows == 0) {

            jdbcTemplate.update(insertQuery, longitude, latitude);

            return "Successfully Inserted";
        }
        else {

            return "Successfully Updated";
        }
    }

    public List<Map<String, Object>> fetchCompanyLocation() {

        return jdbcTemplate.queryForList("SELECT * FROM " + SCHEME_NAME + ".location");
    }
}