package org.common.db.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "STUBSERVERUSERS")
public class User {

    @Id
    @Column(name = "USERNAME")
    private String username;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "FIRSTNAME")
    private String firstname;

    @Column(name = "LASTNAME")
    private String lastname;

    @Column(name = "USERROLE")
    private String userrole;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Column(name = "UPDATED_AT")
    private Timestamp updatedAt;

    public String getUsername()              { return username; }
    public void setUsername(String v)        { this.username = v; }
    public String getPassword()              { return password; }
    public void setPassword(String v)        { this.password = v; }
    public String getEmail()                 { return email; }
    public void setEmail(String v)           { this.email = v; }
    public String getFirstname()             { return firstname; }
    public void setFirstname(String v)       { this.firstname = v; }
    public String getLastname()              { return lastname; }
    public void setLastname(String v)        { this.lastname = v; }
    public String getUserrole()              { return userrole; }
    public void setUserrole(String v)        { this.userrole = v; }
    public String getCreatedBy()             { return createdBy; }
    public void setCreatedBy(String v)       { this.createdBy = v; }
    public String getUpdatedBy()             { return updatedBy; }
    public void setUpdatedBy(String v)       { this.updatedBy = v; }
    public Timestamp getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(Timestamp v)    { this.updatedAt = v; }
}
