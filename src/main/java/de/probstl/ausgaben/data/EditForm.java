package de.probstl.ausgaben.data;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Values from the edit page
 */
@Data
@NoArgsConstructor
@ToString
public class EditForm {

    /** The format to format and parse the timestamp */
    private static final String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss";

    /** The timezone for parsing the timestamp str */
    private static final String ZONE_DEFAULT = "Europe/Berlin";

    /** The id of the expense */
    private String expenseId;

    /** The entered shop of the expense */
    private String shop;

    /** The entered description of the expense */
    private String description;

    /** The entered amount */
    private String amountStr;

    /** The entered timestamp */
    private String timestampStr;

    /** Is the cash checkbox checked */
    private boolean cash;

    /** The city of the expense */
    private String city;

    /**
     * @return The double amount of the entered value in German locale
     */
    public Double getAmountDouble() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.GERMAN);
        try {
            return Double.valueOf(format.parse(getAmountStr()).doubleValue());
        } catch (ParseException e) {
            LoggerFactory.getLogger(EditForm.class).error("No valid number: " + getAmountStr(), e);
        }
        return Double.valueOf(0);
    }

    /**
     * @param amount the amount as double formatted as string in german locale
     */
    public void setAmountFromDouble(Double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.GERMAN);
        setAmountStr(format.format(amount));
    }

    /**
     * Set the timestamp string based on the given date
     */
    public void setTimestampFromDate(Date date) {
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of(ZONE_DEFAULT));
        setTimestampStr(DateTimeFormatter.ofPattern(DATE_FORMAT).format(dateTime));
    }

    /**
     * @return A local date time from the entered timestamp string
     */
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.parse(getTimestampStr(), DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    /**
     * @return A date for saving the entered timestamp string
     */
    public Date getTimestamp() {
        ZonedDateTime zDateTime = ZonedDateTime.of(getLocalDateTime(), ZoneId.of(ZONE_DEFAULT));
        return Date.from(zDateTime.toInstant());
    }
}
