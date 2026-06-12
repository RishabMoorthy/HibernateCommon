package org.common.db.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "configurable.assignedServices")
public class AssignedService {

    @EmbeddedId
    private AssignedServiceId id;

    public AssignedService() {}

    public AssignedService(String username, String serviceName) {
        this.id = new AssignedServiceId(username, serviceName);
    }

    public AssignedServiceId getId()         { return id; }
    public void setId(AssignedServiceId v)   { this.id = v; }
}
