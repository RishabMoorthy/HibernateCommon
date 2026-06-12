package org.common.db.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
//@Table(name = "READYAPI_DAILY_METRICS")
public class DailyMetrics {

    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "vsdetails_seq")
    @Id
    @Column(name = "METRICSID")
    private Long metricsId;

    @Column(name = "TRANSDATE", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date transDate;

    @Column(name = "VSNAME", length = 150)
    private String vsName;

    @Column(name = "COUNT")
    private Long count;

    @Column(name = "VIRTSERVERNAME", length = 50)
    private String virtServerName = "10.44.16.44";

    @Column(name = "ENVTYPE", length = 25)
    private String envType;

    // Getters & Setters
    public Long getMetricsId()                          { return metricsId; }
    public void setMetricsId(Long v)                    { this.metricsId = v; }
    public Date getTransDate()                          { return transDate; }
    public void setTransDate(Date v)                    { this.transDate = v; }
    public String getVsName()                           { return vsName; }
    public void setVsName(String v)                     { this.vsName = v; }
    public Long getCount()                              { return count; }
    public void setCount(Long v)                        { this.count = v; }
    public String getVirtServerName()                   { return virtServerName; }
    public void setVirtServerName(String v)             { this.virtServerName = v; }
    public String getEnvType()                          { return envType; }
    public void setEnvType(String v)                    { this.envType = v; }
}
