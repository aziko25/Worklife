package telegram.bot.Service.Users;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.USERNAME;

@Service
public class ArrivalExitTimes {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ArrivalExitTimes(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public static double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {

        final int R = 6371000; // Radius of the earth in meters

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public double getRadius(String schema_name, Double longitude, Double latitude) {

        String companyCoordinates = "SELECT longitude, latitude FROM " + schema_name + ".location WHERE id = 1;";

        Map<String, Object> coordinates = jdbcTemplate.queryForMap(companyCoordinates);

        Double companyLongitude = (Double) coordinates.get("longitude");
        Double companyLatitude = (Double) coordinates.get("latitude");

        return calculateDistanceInMeters(companyLatitude, companyLongitude, latitude, longitude);
    }

    public void setArrivalTime(String schema_name, String username, String imagePath, Double longitude, Double latitude) {

        String checkSql = "SELECT COUNT(*) FROM " + schema_name + ".working_time WHERE date = current_date AND employee_name = ? AND expected_arrival_time IS NOT NULL;";
        int count = jdbcTemplate.queryForObject(checkSql, Integer.class, username);

        if (count == 0) {

            String oneMoreCheckSql = "SELECT COUNT(*) FROM " + schema_name + ".working_time WHERE date = current_date AND employee_name = ? AND arrived_time IS NULL AND expected_arrival_time IS NULL;";
            int checkCount = jdbcTemplate.queryForObject(oneMoreCheckSql, Integer.class, username);

            LocalDateTime now = LocalDateTime.now();
            Timestamp currentTimestamp = Timestamp.valueOf(now);

            if (checkCount >= 1) {

                try {

                    String sqlUserArrivalTime = "SELECT arrival_time FROM " + schema_name + ".employees WHERE username = ?;";
                    LocalTime userArrivalTime = jdbcTemplate.queryForObject(sqlUserArrivalTime, LocalTime.class, username);

                    assert userArrivalTime != null;
                    long late = Duration.between(userArrivalTime, now.toLocalTime()).toMinutes();

                    String sql = "UPDATE " + schema_name + ".working_time SET arrived_time = ? AND late = ? WHERE date = current_date AND employee_name = ?;";

                    jdbcTemplate.update(sql, currentTimestamp, late, username);
                }
                catch (EmptyResultDataAccessException ignored) {

                    String companyWorkingTime = "SELECT arrival_time FROM " + schema_name + ".company_working_times WHERE id = 1;";
                    LocalTime companyArrivalTime = jdbcTemplate.queryForObject(companyWorkingTime, new SingleColumnRowMapper<>(LocalTime.class));

                    assert companyArrivalTime != null;
                    long late = Duration.between(companyArrivalTime, now.toLocalTime()).toMinutes();

                    String sql = "UPDATE " + schema_name + ".working_time SET arrived_time = ? AND late = ? WHERE date = current_date AND employee_name = ?;";

                    jdbcTemplate.update(sql, currentTimestamp, late, username);
                }
            }
            else {

                try {

                    String sqlUserArrivalTime = "SELECT arrival_time FROM " + schema_name + ".employees WHERE username = ?;";
                    LocalTime userArrivalTime = jdbcTemplate.queryForObject(sqlUserArrivalTime, LocalTime.class, username);

                    double arrivalRadius = getRadius(schema_name, longitude, latitude);

                    assert userArrivalTime != null;
                    long late = Duration.between(userArrivalTime, now.toLocalTime()).toMinutes();

                    String sql = "INSERT INTO " + schema_name + ".working_time (date, arrived_time, employee_name, img_link, late, arrival_radius) VALUES (current_date, ?, ?, ?, ?, ?)";

                    jdbcTemplate.update(sql, currentTimestamp, username, imagePath, late, arrivalRadius);
                }
                catch (EmptyResultDataAccessException ignored) {

                    String companyWorkingTime = "SELECT arrival_time FROM " + schema_name + ".company_working_times WHERE id = 1;";
                    LocalTime companyArrivalTime = jdbcTemplate.queryForObject(companyWorkingTime, new SingleColumnRowMapper<>(LocalTime.class));

                    double arrivalRadius = getRadius(schema_name, longitude, latitude);

                    assert companyArrivalTime != null;
                    long late = Duration.between(companyArrivalTime, now.toLocalTime()).toMinutes();

                    String sql = "INSERT INTO " + schema_name + ".working_time (date, arrived_time, employee_name, img_link, late, arrival_radius) VALUES (current_date, ?, ?, ?, ?, ?)";

                    jdbcTemplate.update(sql, currentTimestamp, username, imagePath, late, arrivalRadius);
                }
            }
        }
        else {

            String checkNullSql = "SELECT COUNT(*) FROM " + schema_name + ".working_time WHERE date = current_date AND employee_name = ? AND arrived_time IS NULL AND expected_arrival_time IS NOT NULL ";
            int nullCount = jdbcTemplate.queryForObject(checkNullSql, Integer.class, username);

            if (nullCount > 0) {

                String sqlExpectedArrivalTime = "SELECT expected_arrival_time FROM " + schema_name + ".working_time WHERE date = current_date AND employee_name = ? AND arrived_time IS NULL;";
                LocalTime userExpectedArrivalTime = jdbcTemplate.queryForObject(sqlExpectedArrivalTime, LocalTime.class, username);

                LocalDateTime now = LocalDateTime.now();
                Timestamp currentTimestamp = Timestamp.valueOf(now);

                long late = Duration.between(userExpectedArrivalTime, now.toLocalTime()).toMinutes();

                String updateSql = "UPDATE " + schema_name + ".working_time SET arrived_time = ?, late = ? WHERE date = current_date AND employee_name = ?";
                jdbcTemplate.update(updateSql, currentTimestamp, late, username);
            }
            else {
                throw new RuntimeException("Already Arrived Today");
            }
        }
    }

    public void setExitTime(String schema_name, String imagePath, String username, Double longitude, Double latitude) {

        LocalDateTime now = LocalDateTime.now();
        Timestamp currentTimestamp = Timestamp.valueOf(now);

        String checkSql = "SELECT * FROM " + schema_name + ".working_time WHERE date = current_date AND employee_name = ? AND arrived_time IS NOT NULL;";

        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(checkSql, username);

        if (resultList.isEmpty()) {

            throw new RuntimeException("You Didn't Arrive Yet!");
        }

        Map<String, Object> result = resultList.get(0);

        if (result.get("exited_time") != null) {

            throw new RuntimeException("You Already Exited Today!");
        }

        double exitRadius = getRadius(schema_name, longitude, latitude);

        if (result.get("expected_exit_time") != null) {

            LocalTime userExpectedExitTime = (LocalTime) result.get("expected_exit_time");

            long overtime = Duration.between(userExpectedExitTime, now.toLocalTime()).toMinutes();

            String sql = "UPDATE " + schema_name + ".working_time SET exited_time = ?, exit_img_link = ?, overtime = ?, exit_radius = ? WHERE employee_name = ? AND arrived_time IS NOT NULL AND date = current_date";

            jdbcTemplate.update(sql, currentTimestamp, imagePath, overtime, exitRadius, username);
        }
        else {

            try {

                LocalTime userExitTime = jdbcTemplate.queryForObject("SELECT exit_time FROM " + schema_name + ".employees WHERE username = ?;", LocalTime.class, username);

                assert userExitTime != null;
                long overtime = Duration.between(userExitTime, now.toLocalTime()).toMinutes();

                String sql = "UPDATE " + schema_name + ".working_time SET exited_time = ?, exit_img_link = ?, overtime = ?, exit_radius = ? WHERE employee_name = ? AND date = current_date AND arrived_time IS NOT NULL;";

                jdbcTemplate.update(sql, currentTimestamp, imagePath, overtime, exitRadius, username);
            }
            catch (EmptyResultDataAccessException ignored) {

                LocalTime exitTime = jdbcTemplate.queryForObject("SELECT exit_time FROM " + schema_name + ".company_working_times WHERE id = 1;", LocalTime.class);

                assert exitTime != null;
                long overtime = Duration.between(exitTime, now.toLocalTime()).toMinutes();

                String sql = "UPDATE " + schema_name + ".working_time SET exited_time = ?, exit_img_link = ?, overtime = ?, exit_radius = ? WHERE employee_name = ? AND date = current_date AND arrived_time IS NOT NULL;";

                jdbcTemplate.update(sql, currentTimestamp, imagePath, overtime, exitRadius, username);
            }
        }
    }
}