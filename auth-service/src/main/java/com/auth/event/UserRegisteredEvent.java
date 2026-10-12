package com.auth.event;

public class UserRegisteredEvent {
    private String id, fullName, email, password, userRole;
    private Long phoneNumber;
    public UserRegisteredEvent() {}
    public String getId(){return id;} public void setId(String v){id=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
    public String getUserRole(){return userRole;} public void setUserRole(String v){userRole=v;}
    public Long getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(Long v){phoneNumber=v;}
}
