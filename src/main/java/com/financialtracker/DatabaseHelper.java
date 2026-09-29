package com.financialtracker;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private static final String URL = "jdbc:sqlite:financial_tracker.db";

    public DatabaseHelper() { createTables(); }

    private Connection connection() throws SQLException { return DriverManager.getConnection(URL); }

    private void createTables() {
        String transactions = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, type TEXT NOT NULL," +
                "category TEXT NOT NULL, amount NUMERIC NOT NULL, note TEXT)";
        String salaries = "CREATE TABLE IF NOT EXISTS monthly_salary (" +
                "month TEXT PRIMARY KEY, amount NUMERIC NOT NULL)";
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.executeUpdate(transactions);
            s.executeUpdate(salaries);
        } catch (SQLException e) { throw new IllegalStateException("Database tidak dapat dibuat", e); }
    }

    public void setSalary(YearMonth month, BigDecimal amount) {
        String sql = "INSERT INTO monthly_salary(month, amount) VALUES(?, ?) " +
                "ON CONFLICT(month) DO UPDATE SET amount=excluded.amount";
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, month.toString()); p.setBigDecimal(2, amount); p.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public BigDecimal getSalary(YearMonth month) {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(
                "SELECT amount FROM monthly_salary WHERE month=?")) {
            p.setString(1, month.toString());
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getBigDecimal(1) : BigDecimal.ZERO; }
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public void saveTransaction(Transaction t) {
        String sql = "INSERT INTO transactions(date,type,category,amount,note) VALUES(?,?,?,?,?)";
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, t.getDate().toString()); p.setString(2, t.getType());
            p.setString(3, t.getCategory()); p.setBigDecimal(4, t.getAmount()); p.setString(5, t.getNote());
            p.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    public List<Transaction> findTransactions(LocalDate from, LocalDate to, String type) {
        List<Transaction> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id,date,type,category,amount,note FROM transactions WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (from != null) { sql.append(" AND date>=?"); args.add(from.toString()); }
        if (to != null) { sql.append(" AND date<=?"); args.add(to.toString()); }
        if (type != null) { sql.append(" AND type=?"); args.add(type); }
        sql.append(" ORDER BY date DESC,id DESC");
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(sql.toString())) {
            for (int i=0;i<args.size();i++) p.setObject(i+1,args.get(i));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) result.add(new Transaction(r.getInt(1), LocalDate.parse(r.getString(2)),
                        r.getString(3), r.getString(4), r.getBigDecimal(5), r.getString(6)));
            }
        } catch (SQLException e) { throw new IllegalStateException(e); }
        return result;
    }

    public BigDecimal total(String type, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type=?");
        List<Object> args = new ArrayList<>(); args.add(type);
        if (from != null) { sql.append(" AND date>=?"); args.add(from.toString()); }
        if (to != null) { sql.append(" AND date<=?"); args.add(to.toString()); }
        try (Connection c=connection(); PreparedStatement p=c.prepareStatement(sql.toString())) {
            for(int i=0;i<args.size();i++) p.setObject(i+1,args.get(i));
            try(ResultSet r=p.executeQuery()){ return r.next()?r.getBigDecimal(1):BigDecimal.ZERO; }
        } catch(SQLException e){ throw new IllegalStateException(e); }
    }

    public BigDecimal totalIncomeIncludingSalary(LocalDate from, LocalDate to) {
        BigDecimal income = total("INCOME", from, to);
        String sql = "SELECT COALESCE(SUM(amount),0) FROM monthly_salary WHERE month>=? AND month<=?";
        String first = from == null ? "0000-01" : YearMonth.from(from).toString();
        String last = to == null ? "9999-12" : YearMonth.from(to).toString();
        try(Connection c=connection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,first); p.setString(2,last);
            try(ResultSet r=p.executeQuery()){ return income.add(r.next()?r.getBigDecimal(1):BigDecimal.ZERO); }
        } catch(SQLException e){ throw new IllegalStateException(e); }
    }

    public Map<String,BigDecimal> categoryTotals(YearMonth month) {
        Map<String,BigDecimal> result=new LinkedHashMap<>();
        LocalDate from=month.atDay(1), to=month.atEndOfMonth();
        for(Transaction t:findTransactions(from,to,"EXPENSE"))
            result.put(t.getCategory(), result.getOrDefault(t.getCategory(),BigDecimal.ZERO).add(t.getAmount()));
        return result;
    }
}
