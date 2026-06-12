package org.common.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AssignedServiceId implements Serializable {

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "SERVICENAME")
    private String serviceName;

    public AssignedServiceId() {}

    public AssignedServiceId(String username, String serviceName) {
        this.username = username;
        this.serviceName = serviceName;
    }

    public String getUsername()             { return username; }
    public void setUsername(String v)       { this.username = v; }
    public String getServiceName()          { return serviceName; }
    public void setServiceName(String v)    { this.serviceName = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignedServiceId)) return false;
        AssignedServiceId that = (AssignedServiceId) o;
        return Objects.equals(username, that.username) && Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() { return Objects.hash(username, serviceName); }
}
