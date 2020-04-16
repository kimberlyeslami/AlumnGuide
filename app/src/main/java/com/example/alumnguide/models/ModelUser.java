package com.example.alumnguide.models;

public class ModelUser {
    String id, username, email, search, currentYear, courseStudying, image;

    public ModelUser() {

    }

    public ModelUser(String id, String username, String email, String search, String currentYear, String courseStudying, String image) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.search = search;
        this.currentYear = currentYear;
        this.courseStudying = courseStudying;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getCurrentYear() {
        return currentYear;
    }

    public void setCurrentYear(String currentYear) {
        this.currentYear = currentYear;
    }

    public String getCourseStudying() {
        return courseStudying;
    }

    public void setCourseStudying(String courseStudying) {
        this.courseStudying = courseStudying;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}