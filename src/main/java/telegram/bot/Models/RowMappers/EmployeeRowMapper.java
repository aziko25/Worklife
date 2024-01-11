package telegram.bot.Models.RowMappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import telegram.bot.Models.Employees;

public class EmployeeRowMapper implements RowMapper<Employees> {

    @Override
    public Employees mapRow(ResultSet rs, int rowNum) throws SQLException {

        Employees employee = new Employees();

        employee.setId(rs.getInt("id"));
        employee.setUsername(rs.getString("username"));
        employee.setPassword(rs.getString("password"));
        employee.setRole(rs.getString("role"));
        employee.setChat_id(rs.getLong("chat_id"));

        return employee;
    }
}
