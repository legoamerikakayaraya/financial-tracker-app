package com.financialtracker;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Transaction {
    private final int id; private final LocalDate date; private final String type, category, note; private final BigDecimal amount;
    public Transaction(int id, LocalDate date, String type, String category, BigDecimal amount, String note) {
        this.id=id; this.date=date; this.type=type; this.category=category; this.amount=amount; this.note=note;
    }
    public int getId(){return id;} public LocalDate getDate(){return date;} public String getType(){return type;}
    public String getCategory(){return category;} public BigDecimal getAmount(){return amount;} public String getNote(){return note;}
}
