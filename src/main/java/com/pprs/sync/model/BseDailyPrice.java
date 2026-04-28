package com.pprs.sync.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;


@Entity
@Table(name = "bse_daily_price",
       uniqueConstraints = @UniqueConstraint(columnNames = {"code", "trade_date"}))
public class BseDailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String isin;
    private String name;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;

    @Column(name = "prev_close")
    private BigDecimal prevClose;

    private Long volume;
    private BigDecimal turnover;

    @Column(name = "total_trades")
    private Long totalTrades;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    protected BseDailyPrice() {}

    public BseDailyPrice(String code, String isin, String name,
                         BigDecimal open, BigDecimal high, BigDecimal low,
                         BigDecimal close, BigDecimal prevClose,
                         Long volume, BigDecimal turnover,
                         Long totalTrades, LocalDate tradeDate) {
        this.code        = code;
        this.isin        = isin;
        this.name        = name;
        this.open        = open;
        this.high        = high;
        this.low         = low;
        this.close       = close;
        this.prevClose   = prevClose;
        this.volume      = volume;
        this.turnover    = turnover;
        this.totalTrades = totalTrades;
        this.tradeDate   = tradeDate;
    }

    // Getters
    public Long getId()              { return id; }
    public String getCode()          { return code; }
    public String getIsin()          { return isin; }
    public String getName()          { return name; }
    public BigDecimal getOpen()      { return open; }
    public BigDecimal getHigh()      { return high; }
    public BigDecimal getLow()       { return low; }
    public BigDecimal getClose()     { return close; }
    public BigDecimal getPrevClose() { return prevClose; }
    public Long getVolume()          { return volume; }
    public BigDecimal getTurnover()  { return turnover; }
    public Long getTotalTrades()     { return totalTrades; }
    public LocalDate getTradeDate()  { return tradeDate; }

    @PrePersist
    public void onCreate() { createdAt = OffsetDateTime.now(); }

    @Override
    public String toString() {
        return "BseDailyPrice{code='" + code + "', date=" + tradeDate +
               ", close=" + close + "}";
    }
}