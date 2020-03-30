package in.projecteka.consentmanager.dataflow;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Utils {
    public static Date toDate(String date) throws ParseException {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        return df.parse(date);
    }

    public static Date toDateWithMilliSeconds(String dateExpiryAt) throws ParseException {
        long timeInMillis = Long.parseLong(dateExpiryAt);
        Date date = new Date(timeInMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+0000'");
        return sdf.parse(sdf.format(date));
    }
}