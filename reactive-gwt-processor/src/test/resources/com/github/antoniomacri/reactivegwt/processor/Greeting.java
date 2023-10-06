package com.github.antoniomacri.reactivegwt.processor;

import java.io.Serializable;

public class Greeting implements Serializable {
    private String greeting;

    public Greeting() {
    }

    public Greeting(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
}
