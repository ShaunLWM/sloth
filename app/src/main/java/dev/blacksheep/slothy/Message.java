package dev.blacksheep.slothy;

public class Message {
    private String email, message;


    public Message(String email, String message) {
        this.email = email;
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }
}
