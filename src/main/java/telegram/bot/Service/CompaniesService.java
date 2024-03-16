package telegram.bot.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompaniesService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CompaniesService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createCompany(String schema_name) {

        try {

            if (schema_name.contains("_")) {

                schema_name = schema_name.replace("_", "");
            }
            if (schema_name.contains(" ")) {

                schema_name = schema_name.replace(" ", "");
            }

            String sql = "CREATE SCHEMA " + schema_name + ";" +
                    "SET SEARCH_PATH TO " + schema_name + ";" +

                    "CREATE TABLE " + schema_name + ".departments (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name varchar unique);" +

                    "CREATE TABLE " + schema_name + ".employees (" +
                    "id SERIAL PRIMARY KEY, " +
                    "workly_code int unique, " +
                    "workly_password int, " +
                    "username varchar unique," +
                    "password varchar, " +
                    "role varchar," +
                    "chat_id bigint," +
                    "deleted boolean default false," +
                    "lastname varchar," +
                    "middlename varchar," +
                    "firstname varchar," +
                    "mail varchar unique," +
                    "birthdate date, " +
                    "image_link varchar," +
                    "arrival_time TIME, " +
                    "exit_time TIME," +
                    "department_name varchar references " + schema_name + ".departments(name) ON UPDATE CASCADE ON DELETE SET NULL;);" +

                    "CREATE TABLE " + schema_name + ".audio (" +
                    "id SERIAL PRIMARY KEY," +
                    "audio BYTEA" +
                    ");" +

                    "CREATE TABLE " + schema_name + ".tasks (" +
                    "id SERIAL PRIMARY KEY," +
                    "description varchar," +
                    "responsible_name varchar references " + schema_name + ".employees(username) ON UPDATE CASCADE ON DELETE CASCADE," +
                    "done timestamp," +
                    "completed boolean," +
                    "appointed varchar references " + schema_name + ".employees(username) ON UPDATE CASCADE ON DELETE CASCADE," +
                    "beingChecked boolean," +
                    "grade int," +
                    "audio_id int references " + schema_name + ".audio," +
                    "creation_date timestamp);" +

                    "CREATE TABLE " + schema_name + ".devices (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username varchar UNIQUE, " +
                    "fcm_token varchar);" +

                    "CREATE TABLE " + schema_name + ".location (" +
                    "id SERIAL PRIMARY KEY, " +
                    "longitude DOUBLE PRECISION," +
                    "latitude DOUBLE PRECISION);" +

                    "CREATE TABLE " + schema_name + ".company_working_times (" +
                    "id SERIAL PRIMARY KEY," +
                    "arrival_time time," +
                    "exit_time time, " +
                    "company_name varchar);" +

                    "CREATE TABLE " + schema_name + ".time_off_reasons (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR UNIQUE);" +

                    "CREATE TABLE " + schema_name + ".working_time (" +
                    "id SERIAL PRIMARY KEY," +
                    "date timestamp," +
                    "arrived_time timestamp," +
                    "exited_time timestamp," +
                    "img_link varchar," +
                    "exit_img_link varchar," +
                    "late int," +
                    "overtime int," +
                    "arrival_radius int," +
                    "exit_radius int," +
                    "time_off boolean, " +
                    "time_off_reason_id int references " + schema_name + ".time_off_reasons," +
                    "expected_arrival_time timestamp," +
                    "expected_exit_time timestamp," +
                    "time_off_comments varchar," +
                    "employee_name varchar references " + schema_name + ".employees(username) ON UPDATE CASCADE ON DELETE CASCADE);" +

                    "CREATE TABLE " + schema_name + ".workly_controller (" +
                    "id SERIAL PRIMARY KEY," +
                    "battery_level int" +
                    ");";

            jdbcTemplate.update(sql);

            String insertSql = "INSERT INTO " + schema_name + ".employees (username, password, role) VALUES (?, ?, ?)";
            String defaultValues1 = "INSERT INTO " + schema_name + ".location(longitude, latitude) VALUES('41.345482', '69.284800');";
            String defaultValues2 = "INSERT INTO " + schema_name + ".company_working_times(arrival_time, exit_time) VALUES('08:00', '19:00');";
            String defaultValues3 = "INSERT INTO " + schema_name + ".workly_controller(battery_level) VALUES(100);";

            jdbcTemplate.update(insertSql, schema_name + "_admin", "admin", "ROLE_ADMIN");
            jdbcTemplate.update(defaultValues1);
            jdbcTemplate.update(defaultValues2);
            jdbcTemplate.update(defaultValues3);
        }
        catch (Exception e) {

            System.out.println(e.getMessage());
        }
    }

    public void deleteCompany(String schema_name) {

        try {

            String sql = "DROP SCHEMA " + schema_name + " CASCADE;";

            jdbcTemplate.update(sql);
        }
        catch (Exception e) {

            System.out.println(e.getMessage());
        }
    }

    public List<String> fetchAllCompanies() {

        String sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name != 'information_schema' AND schema_name != 'pg_catalog' AND schema_name != 'pg_toast' AND schema_name != 'public' AND schema_name != 'bot';";

        return jdbcTemplate.queryForList(sql, String.class);
    }
}
