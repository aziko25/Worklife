package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.Users.ArrivalExitTimes;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static telegram.bot.Controller.Admins.Login.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/setTimes")
@RequiredArgsConstructor
public class ArrivalExitTimesController {

    private final ArrivalExitTimes arrivalExitTimes;
    private final JdbcTemplate jdbcTemplate;

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @PostMapping("/setArrival")
    public ResponseEntity<?> setArrivalTime(@RequestParam("image") MultipartFile imageFile,
                                            @RequestParam Double longitude, @RequestParam Double latitude) {

        LocalDateTime now = LocalDateTime.now();
        Timestamp currentTimestamp = Timestamp.valueOf(now);

        if (imageFile.isEmpty()) {

            return new ResponseEntity<>("Image Is Empty", HttpStatus.BAD_REQUEST);
        } else {

            ZoneId zoneId = ZoneId.of("Asia/Tashkent");
            ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
            LocalDate currentDate = zonedDateTime.toLocalDate();

            String filename = USERNAME + "_" + currentDate + "_arrival.";
            String imagePath = "/var/www/html/images/" + filename + FilenameUtils.getExtension(imageFile.getOriginalFilename());

            try {

                arrivalExitTimes.setArrivalTime(SCHEME_NAME, USERNAME, imagePath, longitude, latitude);
            }
            catch (RuntimeException e) {

                if (e.getMessage().equals("Already Arrived Today")) {

                    return new ResponseEntity<>("Arrival Time Already Set", HttpStatus.CONFLICT);
                }
            }

            try {

                imageFile.transferTo(new File(imagePath));
            } catch (IOException e) {

                new ResponseEntity<>("Error While Saving Image, Exception: " + e, HttpStatus.BAD_REQUEST);
            }

            return ResponseEntity.ok("Arrival Time Set Successfully: " + currentTimestamp);
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @PostMapping("/setExit")
    public ResponseEntity<?> setExitedTime(@RequestParam("image") MultipartFile imageFile,
                                           @RequestParam Double longitude, @RequestParam Double latitude) {

        LocalDateTime now = LocalDateTime.now();
        Timestamp currentTimestamp = Timestamp.valueOf(now);


        if (imageFile.isEmpty()) {

            return new ResponseEntity<>("Image Is Empty", HttpStatus.BAD_REQUEST);
        } else {

            ZoneId zoneId = ZoneId.of("Asia/Tashkent");
            ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
            LocalDate currentDate = zonedDateTime.toLocalDate();


            String filename = USERNAME + "_" + currentDate + "_exit.";
            String imagePath = "/var/www/html/images/" + filename + FilenameUtils.getExtension(imageFile.getOriginalFilename());

            try {

                arrivalExitTimes.setExitTime(SCHEME_NAME, imagePath, USERNAME, longitude, latitude);
                imageFile.transferTo(new File(imagePath));

                return ResponseEntity.ok("Exited Time Set Successfully: " + currentTimestamp);
            }
            catch (RuntimeException e) {

                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            } catch (IOException e) {

                return new ResponseEntity<>("Error While Saving Image, Exception: " + e, HttpStatus.BAD_REQUEST);
            }
        }
    }
}