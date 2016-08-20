package com.gastonheaps.goodforit.model;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;

@IgnoreExtraProperties
public class User {

    public String email;
    public String name;
    public String phone;
    private HashMap<String, Object> timestampCreated;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String name, String phone, HashMap<String, Object> timestampCreated) {
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.timestampCreated = timestampCreated;
    }

}