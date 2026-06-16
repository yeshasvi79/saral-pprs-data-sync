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
import java.util.Objects;


@Entity
@Table(name = "daily_price",
       uniqueConstraints = @UniqueConstraint(columnNames = {"code", "exchange", "trade_date"}))
public class DailyPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String isin;
    private String name;
    private String exchange;
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

    protected DailyPrice() {}

    public DailyPrice(String code, String isin, String name, String exchange,
                      BigDecimal open, BigDecimal high, BigDecimal low,
                      BigDecimal close, BigDecimal prevClose,
                      Long volume, BigDecimal turnover,
                      Long totalTrades, LocalDate tradeDate) {
        this.code        = code;
        this.isin        = isin;
        this.name        = name;
        this.exchange    = exchange;
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
    public String getExchange()      { return exchange; }
    public BigDecimal getOpen()      { return open; }
    public BigDecimal getHigh()      { return high; }
    public BigDecimal getLow()       { return low; }
    public BigDecimal getClose()     { return close; }
    public BigDecimal getPrevClose() { return prevClose; }
    public Long getVolume()          { return volume; }
    public BigDecimal getTurnover()  { return turnover; }
    public Long getTotalTrades()     { return totalTrades; }
    public LocalDate getTradeDate()  { return tradeDate; }

    // Setters
    public void setCode(String code)               { this.code = code; }
    public void setIsin(String isin)               { this.isin = isin; }
    public void setName(String name)               { this.name = name; }
    public void setExchange(String exchange)       { this.exchange = exchange; }
    public void setOpen(BigDecimal open)           { this.open = open; }
    public void setHigh(BigDecimal high)           { this.high = high; }
    public void setLow(BigDecimal low)             { this.low = low; }
    public void setClose(BigDecimal close)         { this.close = close; }
    public void setPrevClose(BigDecimal prevClose) { this.prevClose = prevClose; }
    public void setVolume(Long volume)             { this.volume = volume; }
    public void setTurnover(BigDecimal turnover)   { this.turnover = turnover; }
    public void setTotalTrades(Long totalTrades)   { this.totalTrades = totalTrades; }
    public void setTradeDate(LocalDate tradeDate)  { this.tradeDate = tradeDate; }

    @PrePersist
    public void onCreate() { createdAt = OffsetDateTime.now(); }

    @Override
    public String toString() {
        return "DailyPrice{code='" + code + "', exchange='" + exchange +
               "', date=" + tradeDate + ", close=" + close + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyPrice d)) return false;
        return Objects.equals(code, d.code) &&
               Objects.equals(exchange, d.exchange) &&
               Objects.equals(tradeDate, d.tradeDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, exchange, tradeDate);
    }
}