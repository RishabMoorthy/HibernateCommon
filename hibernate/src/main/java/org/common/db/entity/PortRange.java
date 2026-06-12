package org.common.db.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "READYAPI_PORT_RANGE")
public class PortRange {

    @Id
    @Column(name = "PORTID")
    private Long portId;

    @Column(name = "APPNAME")
    private String appName;

    @Column(name = "PORTS")
    private String ports;

    @Column(name = "UPDATEDBY")
    private String updatedBy;

    @Column(name = "UPDATETIME")
    private Timestamp updateTime;

    public Long getPortId()               { return portId; }
    public void setPortId(Long v)         { this.portId = v; }
    public String getAppName()            { return appName; }
    public void setAppName(String v)      { this.appName = v; }
    public String getPorts()              { return ports; }
    public void setPorts(String v)        { this.ports = v; }
    public String getUpdatedBy()          { return updatedBy; }
    public void setUpdatedBy(String v)    { this.updatedBy = v; }
    public Timestamp getUpdateTime()      { return updateTime; }
    public void setUpdateTime(Timestamp v){ this.updateTime = v; }
}
