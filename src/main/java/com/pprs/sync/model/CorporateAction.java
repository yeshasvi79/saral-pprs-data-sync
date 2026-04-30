package com.pprs.sync.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "corporate_action",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isin", "action_type", "ex_date"}))
public class CorporateAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String isin;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "action_description")
    private String actionDescription;

    @Column(name = "ex_date")
    private LocalDate exDate;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "bc_start_date")
    private LocalDate bcStartDate;

    @Column(name = "bc_end_date")
    private LocalDate bcEndDate;

    @Column(name = "nd_start_date")
    private LocalDate ndStartDate;

    @Column(name = "nd_end_date")
    private LocalDate ndEndDate;

    private String purpose;
    private String exchange;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected CorporateAction() {}

    public CorporateAction(String symbol, String isin, String companyName,
                           String actionType, String actionDescription,
                           LocalDate exDate, LocalDate recordDate,
                           LocalDate bcStartDate, LocalDate bcEndDate,
                           LocalDate ndStartDate, LocalDate ndEndDate,
                           String purpose, String exchange) {
        this.symbol            = symbol;
        this.isin              = isin;
        this.companyName       = companyName;
        this.actionType        = actionType;
        this.actionDescription = actionDescription;
        this.exDate            = exDate;
        this.recordDate        = recordDate;
        this.bcStartDate       = bcStartDate;
        this.bcEndDate         = bcEndDate;
        this.ndStartDate       = ndStartDate;
        this.ndEndDate         = ndEndDate;
        this.purpose           = purpose;
        this.exchange          = exchange;
    }

    // Getters
    public Long getId()                   { return id; }
    public String getSymbol()             { return symbol; }
    public String getIsin()               { return isin; }
    public String getCompanyName()        { return companyName; }
    public String getActionType()         { return actionType; }
    public String getActionDescription()  { return actionDescription; }
    public LocalDate getExDate()          { return exDate; }
    public LocalDate getRecordDate()      { return recordDate; }
    public LocalDate getBcStartDate()     { return bcStartDate; }
    public LocalDate getBcEndDate()       { return bcEndDate; }
    public LocalDate getNdStartDate()     { return ndStartDate; }
    public LocalDate getNdEndDate()       { return ndEndDate; }
    public String getPurpose()            { return purpose; }
    public String getExchange()           { return exchange; }

    // Setters
    public void setSymbol(String symbol)                       { this.symbol = symbol; }
    public void setIsin(String isin)                           { this.isin = isin; }
    public void setCompanyName(String companyName)             { this.companyName = companyName; }
    public void setActionType(String actionType)               { this.actionType = actionType; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }
    public void setExDate(LocalDate exDate)                    { this.exDate = exDate; }
    public void setRecordDate(LocalDate recordDate)            { this.recordDate = recordDate; }
    public void setBcStartDate(LocalDate bcStartDate)          { this.bcStartDate = bcStartDate; }
    public void setBcEndDate(LocalDate bcEndDate)              { this.bcEndDate = bcEndDate; }
    public void setNdStartDate(LocalDate ndStartDate)          { this.ndStartDate = ndStartDate; }
    public void setNdEndDate(LocalDate ndEndDate)              { this.ndEndDate = ndEndDate; }
    public void setPurpose(String purpose)                     { this.purpose = purpose; }
    public void setExchange(String exchange)                   { this.exchange = exchange; }

    @PrePersist
    public void onCreate() { createdAt = updatedAt = OffsetDateTime.now(); }

    @PreUpdate
    public void onUpdate() { updatedAt = OffsetDateTime.now(); }

    @Override
    public String toString() {
        return "CorporateAction{symbol='" + symbol + "', actionType='" + actionType +
               "', exDate=" + exDate + ", isin='" + isin + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CorporateAction c)) return false;
        return Objects.equals(isin, c.isin) &&
               Objects.equals(actionType, c.actionType) &&
               Objects.equals(exDate, c.exDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isin, actionType, exDate);
    }
}