package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.CompaniesService;

import java.util.List;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;
import static telegram.bot.Controller.Admins.Login.isAuthorized;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/admins/Companies")
public class Companies {

    private final CompaniesService companiesService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public Companies(CompaniesService companiesService, JdbcTemplate jdbcTemplate) {
        this.companiesService = companiesService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/allCompanies")
    public List<String> allCompanies() {

        return companiesService.fetchAllCompanies();
    }

    @PostMapping("/create")
    public ResponseEntity<?> createCompany(@RequestParam String schema_name) {

        if (schema_name.matches("[а-яА-ЯёЁ]+")) {

            return new ResponseEntity<>("Company Name Cannot Be In Russian!", HttpStatus.BAD_REQUEST);
        }
        else {

            String checkSql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";
            List<String> results = jdbcTemplate.queryForList(checkSql, String.class, schema_name);

            if (!results.isEmpty()) {

                return new ResponseEntity<>("Company already exists", HttpStatus.BAD_REQUEST);
            }
            else {

                if (schema_name.contains(" ")) {

                    schema_name = schema_name.replace(" ", "");
                }
                if (schema_name.contains("_")) {

                    schema_name = schema_name.replace("_", "");
                }
                if (schema_name.contains("-")) {

                    schema_name = schema_name.replace("-", "_");
                }

                schema_name = schema_name.toLowerCase();

                companiesService.createCompany(schema_name);

                return new ResponseEntity<>(schema_name + " Company created. Admins Username: " + schema_name + "_admin, Password: admin", HttpStatus.OK);
            }
        }
    }


    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCompany() {

        String checkSql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";

        List<String> results = jdbcTemplate.queryForList(checkSql, String.class, SCHEME_NAME);

        if (results.isEmpty()) {

            return new ResponseEntity<>("Company Does Not Exist", HttpStatus.NOT_FOUND);
        }
        else {

            companiesService.deleteCompany(SCHEME_NAME);

            return new ResponseEntity<>(SCHEME_NAME + " Company Deleted", HttpStatus.OK);
        }
    }
}