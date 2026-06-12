package org.common.db.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "READYAPI_MONTHLY_METRICS")
public class MonthlyMetrics {

    @Id
    @Column(name = "ID")
    private Long id;

    @Column(name = "VSNAME")
    private String vsname;

    @Column(name = "MONTH")
    private String month;

    @Column(name = "YEAR")
    private String year;

    @Column(name = "COUNT")
    private Long count;

    @Column(name = "QACOUNT")
    private Long qaCount;

    @Column(name = "PERFCOUNT")
    private Long perfCount;

    public Long getId()              { return id; }
    public void setId(Long v)        { this.id = v; }
    public String getVsname()        { return vsname; }
    public void setVsname(String v)  { this.vsname = v; }
    public String getMonth()         { return month; }
    public void setMonth(String v)   { this.month = v; }
    public String getYear()          { return year; }
    public void setYear(String v)    { this.year = v; }
    public Long getCount()           { return count; }
    public void setCount(Long v)     { this.count = v; }
    public Long getQaCount()         { return qaCount; }
    public void setQaCount(Long v)   { this.qaCount = v; }
    public Long getPerfCount()       { return perfCount; }
    public void setPerfCount(Long v) { this.perfCount = v; }
}
