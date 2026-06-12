package org.common.db.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "configurable.auditLogs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "SERVICE_NAME")
    private String serviceName;

    @Column(name = "ACTION_TYPE")
    private String actionType;

    @Column(name = "REMARK")
    private String remark;

    @Column(name = "TIMESTAMP", insertable = false, updatable = false)
    private Timestamp timestamp;

    public Long getId()                    { return id; }
    public void setId(Long v)              { this.id = v; }
    public String getUsername()            { return username; }
    public void setUsername(String v)      { this.username = v; }
    public String getServiceName()         { return serviceName; }
    public void setServiceName(String v)   { this.serviceName = v; }
    public String getActionType()          { return actionType; }
    public void setActionType(String v)    { this.actionType = v; }
    public String getRemark()              { return remark; }
    public void setRemark(String v)        { this.remark = v; }
    public Timestamp getTimestamp()        { return timestamp; }
    public void setTimestamp(Timestamp v)  { this.timestamp = v; }
}
