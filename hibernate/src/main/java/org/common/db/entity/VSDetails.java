package org.common.db.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
//@Table(name = "STUBSERVERQA_VSDETAILS")
public class VSDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "vsdetails_seq")
    @SequenceGenerator(name = "vsdetails_seq",
            sequenceName = "ISEQ$$_146368",
            allocationSize = 1)
    @Column(name = "VSID")
    private Long vsid;

    @Column(name = "VSNAME", length = 255)
    private String vsName;

    @Column(name = "PORT")
    private Integer port;

    @Column(name = "LASTUPDATED", length = 30)
    private String lastUpdated;

    @Column(name = "USERNAME", length = 255)
    private String username;

    @Column(name = "STATUS", length = 10)
    private String status;

    @Column(name = "KEEPREQRESLOGS", length = 10)
    private String keepReqResLogs = "No";

    @Column(name = "KEEPREQRESLOGSDAYS", length = 20)
    private String keepReqResLogsDays = "15";

    @Column(name = "SAVERESPTIME", length = 20)
    private String saveRespTime = "No";

    @Column(name = "\"GROUP\"")
    private String group;

    @Column(name = "TAGS", length = 100)
    private String tags;

    @Column(name = "DELAYMODE", length = 50)
    private String delayMode = "FIXED";

    @Column(name = "DELAY")
    private Integer delay = 0;

    @Column(name = "LOWERMS")
    private Integer lowerMs = 0;

    @Column(name = "UPPERMS")
    private Integer upperMs = 0;

    @Column(name = "SIGMA", precision = 10, scale = 4)
    private BigDecimal sigma = BigDecimal.ZERO;

    @Column(name = "MEDIANMS", precision = 10, scale = 4)
    private BigDecimal medianMs = BigDecimal.ZERO;

    @Column(name = "TOTALTXN")
    private Integer totalTxn = 0;

    @Column(name = "DELAYPERCENT")
    private Integer delayPercent = 0;

    @Column(name = "DATASOURCEENABLED", length = 10)
    private String datasourceEnabled;

    public Long getVsid()                                   { return vsid; }
    public void setVsid(Long vsid)                          { this.vsid = vsid; }
    public String getVsName()                               { return vsName; }
    public void setVsName(String vsName)                    { this.vsName = vsName; }
    public Integer getPort()                                { return port; }
    public void setPort(Integer port)                       { this.port = port; }
    public String getLastUpdated()                          { return lastUpdated; }
    public void setLastUpdated(String lastUpdated)          { this.lastUpdated = lastUpdated; }
    public String getUsername()                             { return username; }
    public void setUsername(String username)                { this.username = username; }
    public String getStatus()                               { return status; }
    public void setStatus(String status)                    { this.status = status; }
    public String getKeepReqResLogs()                       { return keepReqResLogs; }
    public void setKeepReqResLogs(String keepReqResLogs)    { this.keepReqResLogs = keepReqResLogs; }
    public String getKeepReqResLogsDays()                   { return keepReqResLogsDays; }
    public void setKeepReqResLogsDays(String keepReqResLogsDays) { this.keepReqResLogsDays = keepReqResLogsDays; }
    public String getSaveRespTime()                         { return saveRespTime; }
    public void setSaveRespTime(String saveRespTime)        { this.saveRespTime = saveRespTime; }
    public String getGroup()                                { return group; }
    public void setGroup(String group)                      { this.group = group; }
    public String getTags()                                 { return tags; }
    public void setTags(String tags)                        { this.tags = tags; }
    public String getDelayMode()                            { return delayMode; }
    public void setDelayMode(String delayMode)              { this.delayMode = delayMode; }
    public Integer getDelay()                               { return delay; }
    public void setDelay(Integer delay)                     { this.delay = delay; }
    public Integer getLowerMs()                             { return lowerMs; }
    public void setLowerMs(Integer lowerMs)                 { this.lowerMs = lowerMs; }
    public Integer getUpperMs()                             { return upperMs; }
    public void setUpperMs(Integer upperMs)                 { this.upperMs = upperMs; }
    public BigDecimal getSigma()                            { return sigma; }
    public void setSigma(BigDecimal sigma)                  { this.sigma = sigma; }
    public BigDecimal getMedianMs()                         { return medianMs; }
    public void setMedianMs(BigDecimal medianMs)            { this.medianMs = medianMs; }
    public Integer getTotalTxn()                            { return totalTxn; }
    public void setTotalTxn(Integer totalTxn)               { this.totalTxn = totalTxn; }
    public Integer getDelayPercent()                        { return delayPercent; }
    public void setDelayPercent(Integer delayPercent)       { this.delayPercent = delayPercent; }
    public String getDatasourceEnabled()                    { return datasourceEnabled; }
    public void setDatasourceEnabled(String datasourceEnabled) { this.datasourceEnabled = datasourceEnabled; }
}
