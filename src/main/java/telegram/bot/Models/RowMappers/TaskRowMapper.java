package telegram.bot.Models.RowMappers;

import org.springframework.jdbc.core.RowMapper;
import telegram.bot.Models.Tasks;

public class TaskRowMapper implements RowMapper<Tasks> {

    @Override
    public Tasks mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {

        Tasks task = new Tasks();

        task.setId(rs.getInt("id"));
        task.setDescription(rs.getString("description"));
        task.setResponsible_name(rs.getString("responsible_name"));
        task.setDone(rs.getDate("done"));
        task.setComplete(rs.getBoolean("completed"));
        task.setCreation_date(rs.getDate("creation_date"));

        return task;
    }
}
