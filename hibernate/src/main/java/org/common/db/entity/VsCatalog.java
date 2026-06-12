package org.common.db.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
//@Table(name = "READYAPI_VS_CATALOG")
public class VsCatalog {

    @Id
    @Column(name = "MASTERID", nullable = false)
    private Long masterId;

    @Column(name = "VSNAME", nullable = false, length = 150)
    private String vsName;

    @Column(name = "TRANSPORTTYPE", nullable = false, length = 25)
    private String transportType;

    @Column(name = "VIRTSERVER", nullable = false, length = 50)
    private String virtServer;

    @Column(name = "PORT")
    private Long port;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "UPDATETIME")
    private Date updateTime;

    @Column(name = "UPDATEDBY", length = 50)
    private String updatedBy;

    @Column(name = "STATUS", length = 50)
    private String status;

    @Column(name = "PARTNER", length = 300)
    private String partner;

    @Column(name = "TABLENAME", length = 100)
    private String tableName;

    @Column(name = "INTERNAL_EXTERNAL", length = 10)
    private String internalExternal;

    @Column(name = "ENV_TYPE", length = 20)
    private String envType;

    @Column(name = "HEALTHCHECK", length = 10)
    private String healthCheck;

    // GROUP is a reserved keyword in SQL, so quote it
    @Column(name = "\"GROUP\"", length = 100)
    private String group;

    @Column(name = "TAGS", length = 100)
    private String tags;

    public Long getMasterId()                               { return masterId; }
    public void setMasterId(Long masterId)                  { this.masterId = masterId; }
    public String getVsName()                               { return vsName; }
    public void setVsName(String vsName)                    { this.vsName = vsName; }
    public String getTransportType()                        { return transportType; }
    public void setTransportType(String transportType)      { this.transportType = transportType; }
    public String getVirtServer()                           { return virtServer; }
    public void setVirtServer(String virtServer)            { this.virtServer = virtServer; }
    public Long getPort()                                   { return port; }
    public void setPort(Long port)                          { this.port = port; }
    public void setPort(int port)                           { this.port = (long) port; }
    public Date getUpdateTime()                             { return updateTime; }
    public void setUpdateTime(Date updateTime)              { this.updateTime = updateTime; }
    public String getUpdatedBy()                            { return updatedBy; }
    public void setUpdatedBy(String updatedBy)              { this.updatedBy = updatedBy; }
    public String getStatus()                               { return status; }
    public void setStatus(String status)                    { this.status = status; }
    public String getPartner()                              { return partner; }
    public void setPartner(String partner)                  { this.partner = partner; }
    public String getTableName()                            { return tableName; }
    public void setTableName(String tableName)              { this.tableName = tableName; }
    public String getInternalExternal()                     { return internalExternal; }
    public void setInternalExternal(String internalExternal){ this.internalExternal = internalExternal; }
    public String getEnvType()                              { return envType; }
    public void setEnvType(String envType)                  { this.envType = envType; }
    public String getHealthCheck()                          { return healthCheck; }
    public void setHealthCheck(String healthCheck)          { this.healthCheck = healthCheck; }
    public String getGroup()                                { return group; }
    public void setGroup(String group)                      { this.group = group; }
    public String getTags()                                 { return tags; }
    public void setTags(String tags)                        { this.tags = tags; }
}
