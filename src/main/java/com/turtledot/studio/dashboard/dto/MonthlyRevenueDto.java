package com.turtledot.studio.dashboard.dto;

import java.math.BigDecimal;

public class MonthlyRevenueDto {
    private String month;
    private int year;
    private BigDecimal revenue;
    private int paymentCount;

    public MonthlyRevenueDto() {}

    public MonthlyRevenueDto(String month, int year, BigDecimal revenue, int paymentCount) {
        this.month = month;
        this.year = year;
        this.revenue = revenue;
        this.paymentCount = paymentCount;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public int getPaymentCount() { return paymentCount; }
    public void setPaymentCount(int paymentCount) { this.paymentCount = paymentCount; }
}
