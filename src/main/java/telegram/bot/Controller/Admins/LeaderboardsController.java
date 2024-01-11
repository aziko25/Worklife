package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/leaderboards")
public class LeaderboardsController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public LeaderboardsController(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/grades")
    public ResponseEntity<?> gradesLeaderboards(HttpServletRequest request) {

        String sql = "SET SEARCH_PATH TO " + SCHEME_NAME + ";";

        return ResponseEntity.ok("Here Are The Leaderboards");
    }
}
