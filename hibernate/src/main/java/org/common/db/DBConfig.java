package org.common.db;

public class DBConfig {
    private String dbType;
    private String url;
    private String username;
    private String password;
    private String driver;
    private String dilact;

    public String getDbType()               { return dbType; }
    public void setDbType(String dbType)    { this.dbType = dbType; }
    public String getUrl()                  { return url; }
    public void setUrl(String url)          { this.url = url; }
    public String getUsername()             { return username; }
    public void setUsername(String username){ this.username = username; }
    public String getPassword()             { return password; }
    public void setPassword(String password){ this.password = password; }
    public String getDriver()               { return driver; }
    public void setDriver(String driver)    { this.driver = driver; }
    public String getDilact()               { return dilact; }
    public void setDilact(String dilact)    { this.dilact = dilact; }
}
