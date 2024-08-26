package org.dflib.jjava.jupyter.messages;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A lazy date parser
 */
public class KernelTimestamp {
    public static KernelTimestamp now() {
        return new KernelTimestamp(new Date());
    }

    private static final ThreadLocal<DateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    });

    private String serialized;
    private Date date;

    public KernelTimestamp(String serialized) {
        this.serialized = serialized;
    }

    public KernelTimestamp(Date date) {
        this.date = date;
    }

    public Date getDate() {
        try {
            return date != null ? date : (date = DATE_FORMAT.get().parse(serialized));
        } catch (ParseException e) {
            throw new RuntimeException("Invalid date string '" + serialized + "'", e);
        }
    }

    public String getDateString() {
        return serialized != null ? serialized : (serialized = DATE_FORMAT.get().format(date));
    }
}
