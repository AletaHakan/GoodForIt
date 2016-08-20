package com.gastonheaps.goodforit.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gaston on 6/5/2016.
 */
@IgnoreExtraProperties
public class Payment {
    public Integer amount;
    public String paymentDate;
    public String notes;
    public HashMap<String, Object> timestampCreated;
    public String uid;

    public Payment() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Payment(Integer amount, String paymentDate, String notes, HashMap<String, Object> timestampCreated, String uid) {
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.notes = notes;
        this.timestampCreated = timestampCreated;
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

    public HashMap<String, Object> getTimestampCreated() {
        return timestampCreated;
    }

    @Exclude
    public long getTimestampCreatedLong(){
        return (long)timestampCreated.get("timestamp");
    }
}
