package me.denley.preferencebinder.sample;

import me.denley.preferencebinder.PrefKey;
import me.denley.preferencebinder.PrefType;

@PrefType
public class User {

    String username;

    String email;

    String phone;

    int age;

    @PrefKey("is_verified") boolean verified;

}
