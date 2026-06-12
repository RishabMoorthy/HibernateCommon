package org.common.db.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
//@Table(name = "VS_LIVEURLS")
public class VsLiveUrl {

    @Id
    @Column(name = "VSURLID")
    private Long vsUrlId;

    @Column(name = "VSID")
    private Long vsid;

    @Column(name = "HOST", length = 200)
    private String host;

    @Column(name = "ISACTIVE", length = 2)
    private String isActive;

    @Column(name = "UPDATETIME")
    @Temporal(TemporalType.DATE)
    private Date updateTime;

    @Column(name = "UPDATEDBY", length = 50)
    private String updatedBy;

    public Long getVsUrlId()                            { return vsUrlId; }
    public void setVsUrlId(Long v)                      { this.vsUrlId = v; }
    public Long getVsid()                               { return vsid; }
    public void setVsid(Long v)                         { this.vsid = v; }
    public String getHost()                             { return host; }
    public void setHost(String v)                       { this.host = v; }
    public String getIsActive()                         { return isActive; }
    public void setIsActive(String v)                   { this.isActive = v; }
    public Date getUpdateTime()                         { return updateTime; }
    public void setUpdateTime(Date v)                   { this.updateTime = v; }
    public String getUpdatedBy()                        { return updatedBy; }
    public void setUpdatedBy(String v)                  { this.updatedBy = v; }
}
