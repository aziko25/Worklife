package telegram.bot.Models.RowMappers;

import org.springframework.jdbc.core.RowMapper;
import telegram.bot.Models.WorkingTime;

/*public class WorkingTimeRowMapper implements RowMapper<WorkingTime> {

    @Override
    public WorkingTime mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {

        WorkingTime workingTime = new WorkingTime();

        workingTime.setId(rs.getInt("id"));
        workingTime.setDate(rs.getDate("date"));
        workingTime.setArrived_time(rs.getTime("arrived_time"));
        workingTime.setExited_time(rs.getTime("exited_time"));
        workingTime.setEmployee_name(rs.getString("employee_name"));
        workingTime.setImg_link(rs.getString("img_link"));

        return workingTime;
    }
}
*/