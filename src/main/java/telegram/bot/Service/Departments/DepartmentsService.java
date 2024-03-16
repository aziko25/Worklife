package telegram.bot.Service.Departments;


import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@Service
@RequiredArgsConstructor
public class DepartmentsService {

    private final JdbcTemplate jdbcTemplate;

    public String createDepartment(String name) {

        try {

            jdbcTemplate.update("INSERT INTO " + SCHEME_NAME + ".departments (name) VALUES(?);", name);

            return "You Successfully Created " + name + " Department!";
        }
        catch (DataIntegrityViolationException e) {

            throw new IllegalArgumentException("Department With This Name Already Exists!");
        }
    }

    public List<Map<String, Object>> allDepartments() {

        return jdbcTemplate.queryForList("SELECT * FROM " + SCHEME_NAME + ".departments;");
    }

    public String updateDepartment(Integer id, String name) {

        try {

            int rowsAffected = jdbcTemplate.update("UPDATE " + SCHEME_NAME + ".departments SET name = ? WHERE id = ?;", name, id);

            if (rowsAffected == 0) {

                throw new IllegalArgumentException("Department with ID " + id + " Not found!");
            }

            return "You Successfully Updated Department!";
        }
        catch (DataIntegrityViolationException e) {

            throw new IllegalArgumentException("Department With This Name Already Exists!");
        }
    }

    public String deleteDepartment(Integer id) {

        int rowsAffected = jdbcTemplate.update("DELETE FROM " + SCHEME_NAME + ".departments WHERE id = ?;", id);

        if (rowsAffected == 0) {

            throw new IllegalArgumentException("Department With ID " + id + " Not Found!");
        }

        return "You Successfully Deleted " + id + " Department!";
    }
}