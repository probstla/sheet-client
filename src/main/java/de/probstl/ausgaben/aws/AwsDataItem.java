package de.probstl.ausgaben.aws;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * POJO that holds the item of the DynamoDB entry
 */
@Data
public class AwsDataItem {
    
    /** The shop of the expense */
    private String shop;

    /** The message of the expense */
    private String message;

    /** The city of the expense */
    private String city;

    /** Timestamp of the expense */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SS'Z'", timezone = "UTC")
    private Date timestamp;

    /** Amount of the expense */
    private Double amount;

    /** The budget the expense has been assigned to */
    @JsonIgnore
    private String budget;

    /** Was the expense cash or card */
    private boolean cash;

    /** The unique id of the expense */
    @JsonProperty("Id")
    private String id;

}
