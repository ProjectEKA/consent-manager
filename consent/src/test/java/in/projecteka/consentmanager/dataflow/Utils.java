package in.projecteka.consentmanager.dataflow;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    public static LocalDateTime toDate(String date) {
        String pattern = "yyyy-MM-dd['T'HH[:mm][:ss][.SSS][+0000]]";
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime toDateWithMilliSeconds(String dateExpiryAt) {
        return new Timestamp(Long.parseLong(dateExpiryAt)).toLocalDateTime();
    }
}