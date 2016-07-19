package com.test.kai.sendmailauto;

/**
 * Created by kai-chuan on 7/19/16.
 */
public class TaskConfiguration {
    private final String TAG = "TaskConfiguration";
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private String username;
    private String password;
    private String recipient;
    private String subject;
    private String message;

    public void setMonth(int month) {
        this.month = month;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getYear() {

        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public void setYear(int year) {
        this.year = year;

    }

    @Override
    public String toString() {
        return "Recipient: " + this.recipient + "\n" + "Subject: "+ this.subject + "\n" +
               "Launch at: " + this.year + "-" + (this.month+1) + "-" + this.day + " " +
                fixup(this.hour) + ":" + fixup(this.minute);
    }

    private String fixup(int num) {
        if (num<10)
            return "0"+ String.valueOf(num);
        return String.valueOf(num);
    }
}
