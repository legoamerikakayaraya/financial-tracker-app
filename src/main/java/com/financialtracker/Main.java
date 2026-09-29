package com.financialtracker;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.YearMonth;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FinancialTrackerApp app = new FinancialTrackerApp();
            app.setVisible(true);
        });
    }
}
