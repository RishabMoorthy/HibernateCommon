package org.common.db.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
//@Table(name = "VS_EXECUTIONMODE")
public class VsExecutionMode {

    @Id
    @Column(name = "VSID")
    private Long vsid;

    @Column(name = "MASTERID", length = 100, nullable = false)
    private Long masterId;

    @Column(name = "VIRTSERVER", length = 50)
    private String virtServer;

    @Column(name = "EXECUTIONMODE", length = 50)
    private String executionMode;

    @Column(name = "UPDATETIME")
    @Temporal(TemporalType.DATE)
    private Date updateTime;

    @Column(name = "UPDATEDBY", length = 50)
    private String updatedBy;

    public Long getVsid()                               { return vsid; }
    public void setVsid(Long v)                         { this.vsid = v; }
    public Long getMasterId()                           { return masterId; }
    public void setMasterId(Long v)                     { this.masterId = v; }
    public String getVirtServer()                       { return virtServer; }
    public void setVirtServer(String v)                 { this.virtServer = v; }
    public String getExecutionMode()                    { return executionMode; }
    public void setExecutionMode(String v)              { this.executionMode = v; }
    public Date getUpdateTime()                         { return updateTime; }
    public void setUpdateTime(Date v)                   { this.updateTime = v; }
    public String getUpdatedBy()                        { return updatedBy; }
    public void setUpdatedBy(String v)                  { this.updatedBy = v; }
}
