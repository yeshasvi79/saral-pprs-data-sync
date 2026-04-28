package com.pprs.sync.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "securities_master",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isin", "exchange"}))
public class Security {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String isin;
    private String symbol;
    private String name;
    private String exchange;
    private String series;

    @Column(name = "face_value")
    private BigDecimal faceValue;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "source_updated_at")
    private LocalDate sourceUpdatedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // Required by JPA
    protected Security() {}

    // All-args constructor
    public Security(Long id, String isin, String symbol, String name,
                    String exchange, String series, BigDecimal faceValue,
                    Boolean isActive, LocalDate sourceUpdatedAt,
                    OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id              = id;
        this.isin            = isin;
        this.symbol          = symbol;
        this.name            = name;
        this.exchange        = exchange;
        this.series          = series;
        this.faceValue       = faceValue;
        this.isActive        = isActive;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.createdAt       = createdAt;
        this.updatedAt       = updatedAt;
    }

    // Getters
    public Long getId()                     { return id; }
    public String getIsin()                 { return isin; }
    public String getSymbol()               { return symbol; }
    public String getName()                 { return name; }
    public String getExchange()             { return exchange; }
    public String getSeries()               { return series; }
    public BigDecimal getFaceValue()        { return faceValue; }
    public Boolean getIsActive()            { return isActive; }
    public LocalDate getSourceUpdatedAt()   { return sourceUpdatedAt; }
    public OffsetDateTime getCreatedAt()    { return createdAt; }
    public OffsetDateTime getUpdatedAt()    { return updatedAt; }

    // Setters
    public void setId(Long id)                          { this.id = id; }
    public void setIsin(String isin)                    { this.isin = isin; }
    public void setSymbol(String symbol)                { this.symbol = symbol; }
    public void setName(String name)                    { this.name = name; }
    public void setExchange(String exchange)            { this.exchange = exchange; }
    public void setSeries(String series)                { this.series = series; }
    public void setFaceValue(BigDecimal faceValue)      { this.faceValue = faceValue; }
    public void setIsActive(Boolean isActive)           { this.isActive = isActive; }
    public void setSourceUpdatedAt(LocalDate d)         { this.sourceUpdatedAt = d; }
    public void setCreatedAt(OffsetDateTime createdAt)  { this.createdAt = createdAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt)  { this.updatedAt = updatedAt; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String isin;
        private String symbol;
        private String name;
        private String exchange;
        private String series;
        private BigDecimal faceValue;
        private Boolean isActive = true;
        private LocalDate sourceUpdatedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public Builder id(Long id)                          { this.id = id; return this; }
        public Builder isin(String isin)                    { this.isin = isin; return this; }
        public Builder symbol(String symbol)                { this.symbol = symbol; return this; }
        public Builder name(String name)                    { this.name = name; return this; }
        public Builder exchange(String exchange)            { this.exchange = exchange; return this; }
        public Builder series(String series)                { this.series = series; return this; }
        public Builder faceValue(BigDecimal faceValue)      { this.faceValue = faceValue; return this; }
        public Builder isActive(Boolean isActive)           { this.isActive = isActive; return this; }
        public Builder sourceUpdatedAt(LocalDate d)         { this.sourceUpdatedAt = d; return this; }
        public Builder createdAt(OffsetDateTime createdAt)  { this.createdAt = createdAt; return this; }
        public Builder updatedAt(OffsetDateTime updatedAt)  { this.updatedAt = updatedAt; return this; }

        public Security build() {
            return new Security(id, isin, symbol, name, exchange, series,
                                faceValue, isActive, sourceUpdatedAt,
                                createdAt, updatedAt);
        }
    }

    @PrePersist
    public void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "Security{isin='" + isin + "', symbol='" + symbol +
               "', exchange='" + exchange + "', series='" + series + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Security s)) return false;
        return Objects.equals(isin, s.isin) &&
               Objects.equals(exchange, s.exchange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isin, exchange);
    }
}
