package telegram.bot.Controller.Admins;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.Users.ArrivalExitTimes;
import telegram.bot.Service.Workly.WorklyService;

import java.io.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/workly")
@CrossOrigin(maxAge = 3600)
public class WorklyController {

    private final JdbcTemplate jdbcTemplate;
    private final ArrivalExitTimes arrivalExitTimes;
    private final WorklyService worklyService;

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/batteryStatus")
    public ResponseEntity<?> batteryStatus() {

        return ResponseEntity.ok(worklyService.getWorklyControllerBattery());
    }

    @Authorization(requiredRoles = {"ROLE_WORKLY"})
    @PutMapping("/updateBattery")
    public ResponseEntity<?> updateBatteryStatus(@RequestParam int batteryLevel) {

        return ResponseEntity.ok(worklyService.storeWorklyControllerBattery(batteryLevel));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/createAccount")
    public ResponseEntity<?> createWorklyAccount(@RequestParam String username, @RequestParam String password) {

        return ResponseEntity.ok(worklyService.createWorklyAccount(username, password));
    }

    @Authorization(requiredRoles = {"ROLE_WORKLY"})
    @PostMapping("/verifyCode")
    public ResponseEntity<?> verifyCode(@RequestParam int code, @RequestParam int password) {

        try {

            String sql = "SELECT username from " + SCHEME_NAME + ".employees Where workly_code = ? and workly_password = ?;";

            String username = jdbcTemplate.queryForObject(sql, String.class, code, password);

            return ResponseEntity.ok("User Found: " + username);
        }
        catch (EmptyResultDataAccessException e) {

            throw new IllegalArgumentException("User Not Found!");
        }
    }

    @Authorization(requiredRoles = {"ROLE_WORKLY"})
    @PostMapping("/setArrival")
    public ResponseEntity<?> setArrivalTime(@RequestParam int code, @RequestParam("image") MultipartFile imageFile) {

        try {

            String sqlToFetchUsername = "SELECT username from " + SCHEME_NAME + ".employees Where workly_code = ?;";
            String username = jdbcTemplate.queryForObject(sqlToFetchUsername, String.class, code);

            LocalDateTime now = LocalDateTime.now();
            Timestamp currentTimestamp = Timestamp.valueOf(now);

            String sql = "SELECT longitude, latitude FROM " + SCHEME_NAME + ".location WHERE id = 1;";

            Map<String, Object> map = jdbcTemplate.queryForMap(sql);

            Double longitude = (Double) map.get("longitude");
            Double latitude = (Double) map.get("latitude");

            if (imageFile.isEmpty()) {

                return new ResponseEntity<>("Image Is Empty", HttpStatus.BAD_REQUEST);
            }
            else {

                ZoneId zoneId = ZoneId.of("Asia/Tashkent");
                ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
                LocalDate currentDate = zonedDateTime.toLocalDate();

                String filename = username + "_" + currentDate + "_arrival.";
                String imagePath = "/var/www/html/images/" + filename + FilenameUtils.getExtension(imageFile.getOriginalFilename());

                try {

                    arrivalExitTimes.setArrivalTime(SCHEME_NAME, username, imagePath, longitude, latitude);
                }
                catch (RuntimeException e) {

                    if (e.getMessage().equals("Already Arrived Today")) {

                        return new ResponseEntity<>("Arrival Time Already Set", HttpStatus.CONFLICT);
                    }
                }

                try {

                    imageFile.transferTo(new File(imagePath));
                }
                catch (IOException e) {

                    new ResponseEntity<>("Error While Saving Image, Exception: " + e, HttpStatus.BAD_REQUEST);
                }

                return ResponseEntity.ok("Arrival Time Set Successfully: " + currentTimestamp);
            }
        }
        catch (EmptyResultDataAccessException e) {

            throw new IllegalArgumentException("User Not Found!");
        }
    }

    @Authorization(requiredRoles = {"ROLE_WORKLY"})
    @PostMapping("/setExit")
    public ResponseEntity<?> setExitedTime(@RequestParam int code, @RequestParam("image") MultipartFile imageFile) {

        try {

            String sqlToFetchUsername = "SELECT username from " + SCHEME_NAME + ".employees Where workly_code = ?;";
            String username = jdbcTemplate.queryForObject(sqlToFetchUsername, String.class, code);

            LocalDateTime now = LocalDateTime.now();
            Timestamp currentTimestamp = Timestamp.valueOf(now);

            String sql = "SELECT longitude, latitude FROM " + SCHEME_NAME + ".location WHERE id = 1;";

            Map<String, Object> map = jdbcTemplate.queryForMap(sql);

            Double longitude = (Double) map.get("longitude");
            Double latitude = (Double) map.get("latitude");

            if (imageFile.isEmpty()) {

                return new ResponseEntity<>("Image Is Empty", HttpStatus.BAD_REQUEST);
            }
            else {

                ZoneId zoneId = ZoneId.of("Asia/Tashkent");
                ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
                LocalDate currentDate = zonedDateTime.toLocalDate();


                String filename = username + "_" + currentDate + "_exit.";
                String imagePath = "/var/www/html/images/" + filename + FilenameUtils.getExtension(imageFile.getOriginalFilename());

                try {

                    arrivalExitTimes.setExitTime(SCHEME_NAME, imagePath, username, longitude, latitude);
                    imageFile.transferTo(new File(imagePath));

                    return ResponseEntity.ok("Exited Time Set Successfully: " + currentTimestamp);
                }
                catch (RuntimeException e) {

                    return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
                }
                catch (IOException e) {

                    return new ResponseEntity<>("Error While Saving Image, Exception: " + e, HttpStatus.BAD_REQUEST);
                }
            }
        }
        catch (EmptyResultDataAccessException e) {

            throw new IllegalArgumentException("User Not Found!");
        }
    }


    @Authorization(requiredRoles = {"ROLE_WORKLY"})
    @PostMapping("/uploadBulkRecords")
    public ResponseEntity<?> uploadBulkRecords(@RequestBody List<BulkRecord> records) {

        List<String> results = new ArrayList<>();

        for (BulkRecord record : records) {
            try {
                String username = fetchUsername(record.getCode());

                String imagePath = saveImage(record.getImage(), username, record.getArrivalTime(), record.getExitTime());

                if (record.getArrivalTime() != null) {
                    arrivalExitTimes.setArrivalTime(SCHEME_NAME, username, imagePath, 0.0, 0.0);
                }
                if (record.getExitTime() != null) {
                    arrivalExitTimes.setExitTime(SCHEME_NAME, imagePath, username, 0.0, 0.0);
                }

                results.add("Record for user " + username + " processed successfully.");
            }
            catch (Exception e) {
                results.add("Error processing record for code " + record.getCode() + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(results);
    }

    private String fetchUsername(int code) {
        String sqlToFetchUsername = "SELECT username from " + SCHEME_NAME + ".employees Where workly_code = ?;";
        return jdbcTemplate.queryForObject(sqlToFetchUsername, String.class, code);
    }

    private String saveImage(String base64Image, String username, LocalDateTime time, LocalDateTime exitTime) throws IOException {
        // Decode Base64 to image
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        InputStream in = new ByteArrayInputStream(imageBytes);

        ZoneId zoneId = ZoneId.of("Asia/Tashkent");
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
        LocalDate currentDate = zonedDateTime.toLocalDate();

        String timestampPart = (time != null) ? "_arrival" : (exitTime != null) ? "_exit" : "";
        String filename = username + "_" + currentDate + timestampPart + ".png"; // Assuming PNG format

        String imagePath = "/var/www/html/images/" + filename;

        File file = new File(imagePath);
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(imageBytes);
        }

        return imagePath;
    }

    @Getter
    @Setter
    public static class BulkRecord {
        private int code;
        private String image;
        private LocalDateTime arrivalTime;
        private LocalDateTime exitTime;
    }
}