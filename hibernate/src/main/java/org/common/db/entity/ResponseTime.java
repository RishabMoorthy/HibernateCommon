package org.common.db.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "READYAPI_RESPONSE_TIME")
public class ResponseTime {

    @Id
    @Column(name = "RESPID")
    private Long respId;

    @Column(name = "METRICSID")
    private Long metricsId;

    @Column(name = "STARTTIME")
    private Timestamp startTime;

    @Column(name = "ENDTIME")
    private Timestamp endTime;

    @Column(name = "AVGRESPTIME")
    private Double avgRespTime;

    @Column(name = "MAXRESPTIME")
    private Double maxRespTime;

    @Column(name = "AVGTPS")
    private Double avgTps;

    public Long getRespId()                { return respId; }
    public void setRespId(Long v)          { this.respId = v; }
    public Long getMetricsId()             { return metricsId; }
    public void setMetricsId(Long v)       { this.metricsId = v; }
    public Timestamp getStartTime()        { return startTime; }
    public void setStartTime(Timestamp v)  { this.startTime = v; }
    public Timestamp getEndTime()          { return endTime; }
    public void setEndTime(Timestamp v)    { this.endTime = v; }
    public Double getAvgRespTime()         { return avgRespTime; }
    public void setAvgRespTime(Double v)   { this.avgRespTime = v; }
    public Double getMaxRespTime()         { return maxRespTime; }
    public void setMaxRespTime(Double v)   { this.maxRespTime = v; }
    public Double getAvgTps()              { return avgTps; }
    public void setAvgTps(Double v)        { this.avgTps = v; }
}
