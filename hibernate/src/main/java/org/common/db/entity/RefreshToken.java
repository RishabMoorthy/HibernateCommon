package org.common.db.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "AUTH_REFRESH_TOKENS")
public class RefreshToken {

    @Id
    @Column(name = "JTI")
    private String jti;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "EXPIRES_AT")
    private Timestamp expiresAt;

    @Column(name = "ISSUED_AT")
    private Timestamp issuedAt;

    @Column(name = "USER_AGENT")
    private String userAgent;

    @Column(name = "IP")
    private String ip;

    public String getJti()                 { return jti; }
    public void setJti(String v)           { this.jti = v; }
    public String getUsername()            { return username; }
    public void setUsername(String v)      { this.username = v; }
    public Timestamp getExpiresAt()        { return expiresAt; }
    public void setExpiresAt(Timestamp v)  { this.expiresAt = v; }
    public Timestamp getIssuedAt()         { return issuedAt; }
    public void setIssuedAt(Timestamp v)   { this.issuedAt = v; }
    public String getUserAgent()           { return userAgent; }
    public void setUserAgent(String v)     { this.userAgent = v; }
    public String getIp()                  { return ip; }
    public void setIp(String v)            { this.ip = v; }
}
