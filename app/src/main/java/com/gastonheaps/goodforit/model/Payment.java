package com.gastonheaps.goodforit.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class Payment {
    public Integer amount;
    public String paymentDate;
    public String notes;
    public Object timestamp;
    public String uid;

    public Payment() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Payment(Integer amount, String paymentDate, String notes, Object timestamp, String uid) {
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.notes = notes;
        this.timestamp = timestamp;
        this.uid = uid;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getPaymentDate() {
        return paymentDate;
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
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("amount", amount);
        result.put("paymentDate", paymentDate);
        result.put("notes", notes);
        result.put("timestamp", timestamp);
        result.put("uid", uid);

        return result;
    }
}
