package telegram.bot.Service.WorkingTimes;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@Service
@RequiredArgsConstructor
public class WorkingTimesService {

    private final JdbcTemplate jdbcTemplate;

    public SortedMap<LocalDate, Map<String, Integer>> lateAndAtTimeGraphForDateRanges(LocalDate start, LocalDate end) {

        String fetchData = "SELECT date, COUNT(*) as came, SUM(CASE WHEN late > 0 THEN 1 ELSE 0 END) as late, SUM(CASE WHEN late <= 0 THEN 1 ELSE 0 END) as onTime FROM " + SCHEME_NAME + ".working_time WHERE date BETWEEN ? AND ? GROUP BY date;";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(fetchData, start, end);

        SortedMap<LocalDate, Map<String, Integer>> output = new TreeMap<>();

        for (Map<String, Object> result : results) {

            LocalDate date = ((Timestamp) result.get("date")).toLocalDateTime().toLocalDate();
            Integer total = ((Long) result.get("came")).intValue();
            Integer late = ((Long) result.get("late")).intValue();
            Integer onTime = ((Long) result.get("onTime")).intValue();

            Map<String, Integer> data = new LinkedHashMap<>();

            data.put("came", total);
            data.put("late", late);
            data.put("onTime", onTime);

            output.put(date, data);
        }

        Integer fetchEmployeesCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + SCHEME_NAME + ".employees WHERE role IN ('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_USER');", Integer.class);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            if (!output.containsKey(date)) {

                Map<String, Integer> data = new LinkedHashMap<>();

                data.put("came", 0);
                data.put("late", 0);
                data.put("onTime", 0);
                data.put("absent", fetchEmployeesCount);

                output.put(date, data);
            }
            else {

                Map<String, Integer> data = output.get(date);

                data.put("absent", fetchEmployeesCount - data.get("came"));
            }
        }

        return output;
    }
}