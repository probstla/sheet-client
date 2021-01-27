package de.probstl.ausgaben.data;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

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
            System.err.println("No valid number: " + getAmountStr());
        }
        return Double.valueOf(0);
    }
}
