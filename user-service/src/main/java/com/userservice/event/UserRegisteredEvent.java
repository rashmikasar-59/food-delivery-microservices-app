package com.userservice.event;

public class UserRegisteredEvent {
    private String id;
    private String fullName;
    private String email;
    private String password; // already BCrypt-encoded by user-service
    private Long phoneNumber;
    private String userRole;

    public UserRegisteredEvent() {}
    public UserRegisteredEvent(String id, String fullName, String email, String password, Long phoneNumber, String userRole) {
        this.id=id; this.fullName=fullName; this.email=email; this.password=password;
        this.phoneNumber=phoneNumber; this.userRole=userRole;
    }
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public Long getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(Long v){phoneNumber=v;}
    public String getUserRole(){return userRole;} public void setUserRole(String v){userRole=v;}
}
