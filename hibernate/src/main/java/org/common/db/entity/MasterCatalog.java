package org.common.db.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
//@Table(name = "STUBSERVER_MASTER_CATALOG_QA")
public class MasterCatalog {

    @Id
    @Column(name = "MASTERID")
    private Long masterId;

    @Column(name = "VSNAME", length = 150, nullable = false)
    private String vsName;

    @Column(name = "UPDATETIME")
    @Temporal(TemporalType.DATE)
    private Date updateTime;

    @Column(name = "STATUS", length = 50)
    private String status;

    @Column(name = "BACKENDAPPLICATION", length = 300)
    private String backendApplication;

    @Column(name = "BACKENDTYPE", length = 10)
    private String backendType;

    @Column(name = "ENV_TYPE", length = 20)
    private String envType;

    @Column(name = "\"GROUP\"", length = 100)
    private String group;

    @Column(name = "PORT")
    private Integer port;

    public Long getMasterId()                               { return masterId; }
    public void setMasterId(Long masterId)                  { this.masterId = masterId; }
    public String getVsName()                               { return vsName; }
    public void setVsName(String vsName)                    { this.vsName = vsName; }
    public Date getUpdateTime()                             { return updateTime; }
    public void setUpdateTime(Date updateTime)              { this.updateTime = updateTime; }
    public String getStatus()                               { return status; }
    public void setStatus(String status)                    { this.status = status; }
    public String getBackendApplication()                   { return backendApplication; }
    public void setBackendApplication(String backendApplication) { this.backendApplication = backendApplication; }
    public String getBackendType()                          { return backendType; }
    public void setBackendType(String backendType)          { this.backendType = backendType; }
    public String getEnvType()                              { return envType; }
    public void setEnvType(String envType)                  { this.envType = envType; }
    public String getGroup()                                { return group; }
    public void setGroup(String group)                      { this.group = group; }
    public Integer getPort()                                { return port; }
    public void setPort(Integer port)                       { this.port = port; }
}
