package com.gastonheaps.goodforit.model;

import android.content.Context;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gaston on 6/5/2016.
 */
public class Loan {
    public String person;
    public Integer type;
    public Integer amount;
    public Long date;
    public String notes;
    public String uid;
    public Object timestamp;

    public Loan() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Loan(String person, Integer type, Integer amount, Long date, String notes, String uid) {
        this.person = person;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
        this.uid = uid;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public String getPerson() {
        return person;
    }

    public Integer getType() {
        return type;
    }

    public Integer getAmount() {
        return amount;
    }

    public Long getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }

    public String getUid() {
        return uid;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    @Exclude
    public Calendar getDateCalendar() {
        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        return calendar;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("person", person);
        result.put("type", type);
        result.put("amount", amount);
        result.put("date", date);
        result.put("notes", notes);
        result.put("uid", uid);
        result.put("timestamp", timestamp);

        return result;
    }
}
