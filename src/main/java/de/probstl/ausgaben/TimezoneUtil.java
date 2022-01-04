package de.probstl.ausgaben;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Util class for timezone converting
 */
public final class TimezoneUtil {

    /**
     * Private constructor to avoid instances
     */
    private TimezoneUtil() {
        // Empty
    }

    /**
     * @return The system timezone the server (JVM) is running
     */
    public static ZoneId getSystem() {
        return ZoneId.of("UTC");
    }

    /**
     * @return The home timezone (Germany)
     */
    public static ZoneId getHome() {
        return ZoneId.of("Europe/Berlin");
    }

    /**
     * Convert a timestamp from a Document to the Date in a form
     * 
     * @param originalTimestamp The timestamp from the document
     * @return Date for the form to be displayed on the page
     */
    public static Date fromDocument(Instant originalTimestamp) {
        LocalDateTime ldt = LocalDateTime.ofInstant(originalTimestamp, getHome());
        ZonedDateTime zdt = ZonedDateTime.of(ldt, getSystem());
        return Date.from(zdt.toInstant());
    }

}