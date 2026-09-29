package com.financialtracker;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;

public class FinancialTrackerApp extends JFrame {
    private final DatabaseHelper db = new DatabaseHelper();
    private final YearMonth month = YearMonth.now();
    private final JLabel balance = new JLabel(), income = new JLabel(), expense = new JLabel(), reminder = new JLabel();
    private final JLabel salaryLabel = new JLabel();
    private final DefaultTableModel table = new DefaultTableModel(new Object[]{"Tanggal","Tipe","Kategori","Nominal","Catatan"},0);
    private final JComboBox<String> type = new JComboBox<>(new String[]{"Pemasukan","Pengeluaran"});
    private final JComboBox<String> category = new JComboBox<>();
    private final JTextField date = new JTextField(LocalDate.now().toString());
    private final JTextField amount = new JTextField(); private final JTextField note = new JTextField();
    private final JTextField salary = new JTextField(); private final JComboBox<String> filter = new JComboBox<>(new String[]{"Hari ini","Minggu ini","Bulan ini","Tahun ini","Rentang tanggal"});
    private final JTextField from = new JTextField(LocalDate.now().withDayOfMonth(1).toString()); private final JTextField to = new JTextField(LocalDate.now().toString());

    private static final Map<String,Double> GOALS = new LinkedHashMap<>();
    static { GOALS.put("Kebutuhan",.50); GOALS.put("Jajan",.05); GOALS.put("Investasi Jangka Pendek",.15); GOALS.put("Hutang",.10); GOALS.put("Investasi Jangka Panjang",.15); }

    public FinancialTrackerApp(){ setTitle("Financial Tracker"); setSize(1100,760); setLocationRelativeTo(null); setDefaultCloseOperation(EXIT_ON_CLOSE); build(); refresh(); }
    private void build(){
        JPanel root=new JPanel(new BorderLayout(10,10)); root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel cards=new JPanel(new GridLayout(1,3,10,10)); cards.add(card("Saldo tersimpan",balance)); cards.add(card("Pemasukan bulan ini",income)); cards.add(card("Pengeluaran bulan ini",expense));
        root.add(cards,BorderLayout.NORTH); root.add(new JTabbedPane(){ { addTab("Dashboard",dashboard()); addTab("Semua Pengeluaran",expenses()); } },BorderLayout.CENTER); setContentPane(root);
        type.addActionListener(e->categories()); categories();
    }
    private JPanel card(String title,JLabel value){ JPanel p=new JPanel(new BorderLayout()); p.setBorder(BorderFactory.createTitledBorder(title)); value.setFont(new Font("SansSerif",Font.BOLD,20)); p.add(value); return p; }
    private JPanel dashboard(){
        JPanel p=new JPanel(new BorderLayout(10,10)); JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(new JLabel("Gaji bulan ini:")); salary.setPreferredSize(new Dimension(130,28)); top.add(salary); JButton save=new JButton("Simpan gaji"); save.addActionListener(e->saveSalary()); top.add(save); top.add(salaryLabel); p.add(top,BorderLayout.NORTH);
        JPanel content=new JPanel(); content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS)); content.add(reminder); content.add(Box.createVerticalStrut(8));
        for(String c:GOALS.keySet()){ JPanel row=new JPanel(new BorderLayout(8,8)); row.setBorder(BorderFactory.createTitledBorder(c)); row.setPreferredSize(new Dimension(600,70)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE,80)); row.add(new JLabel(c),BorderLayout.NORTH); row.add(new JProgressBar(),BorderLayout.CENTER); content.add(row); }
        p.add(new JScrollPane(content),BorderLayout.CENTER); p.add(form(),BorderLayout.SOUTH); return p;
    }
    private JPanel form(){
        JPanel p=new JPanel(new GridLayout(2,6,6,6)); p.setBorder(BorderFactory.createTitledBorder("Transaksi cepat")); p.add(type); p.add(category); p.add(date); p.add(amount); p.add(note); JButton b=new JButton("Simpan"); b.addActionListener(e->saveTransaction()); p.add(b); return p;
    }
    private JPanel expenses(){
        JPanel p=new JPanel(new BorderLayout(8,8)); JPanel f=new JPanel(new FlowLayout(FlowLayout.LEFT)); f.add(new JLabel("Filter:")); f.add(filter); f.add(new JLabel("Dari:")); f.add(from); f.add(new JLabel("Sampai:")); f.add(to); JButton apply=new JButton("Terapkan"); apply.addActionListener(e->refreshTable()); f.add(apply); p.add(f,BorderLayout.NORTH);
        JTable t=new JTable(table); t.setRowHeight(25); p.add(new JScrollPane(t),BorderLayout.CENTER); return p;
    }
    private void categories(){ category.removeAllItems(); if(type.getSelectedIndex()==0){category.addItem("Bonus");category.addItem("Lainnya");} else for(String c:GOALS.keySet()) category.addItem(c); }
    private void saveSalary(){ try{ BigDecimal v=money(salary.getText()); if(v.signum()<=0)throw new Exception(); db.setSalary(month,v); refresh(); JOptionPane.showMessageDialog(this,"Gaji bulan ini tersimpan. Goals dihitung ulang."); }catch(Exception e){error("Nominal gaji tidak valid");} }
    private void saveTransaction(){ try{ BigDecimal v=money(amount.getText()); LocalDate d=LocalDate.parse(date.getText().trim()); String typ=type.getSelectedIndex()==0?"INCOME":"EXPENSE"; String cat=(String)category.getSelectedItem();
        if(v.signum()<=0||cat==null)throw new Exception(); if("EXPENSE".equals(typ)){ BigDecimal limit=limit(cat,db.getSalary(YearMonth.from(d))); BigDecimal used=db.categoryTotals(YearMonth.from(d)).getOrDefault(cat,BigDecimal.ZERO); if(limit.signum()>0&&used.add(v).compareTo(limit)>=0) JOptionPane.showMessageDialog(this,"Peringatan: transaksi ini mencapai/melewati budget "+cat+".","Budget",JOptionPane.WARNING_MESSAGE); }
        db.saveTransaction(new Transaction(0,d,typ,cat,v,note.getText().trim().isEmpty()?"-":note.getText().trim())); amount.setText("");note.setText("");refresh();
    }catch(Exception e){error("Tanggal atau nominal transaksi tidak valid");} }
    private BigDecimal money(String s){ return new BigDecimal(s.replace(".","").replace(",",".").trim()); }
    private BigDecimal limit(String c,BigDecimal s){ return s.multiply(BigDecimal.valueOf(GOALS.getOrDefault(c,0d))); }
    private void refresh(){ BigDecimal inc=db.totalIncomeIncludingSalary(month.atDay(1),month.atEndOfMonth()), exp=db.total("EXPENSE",month.atDay(1),month.atEndOfMonth()); income.setText(rp(inc));expense.setText(rp(exp));balance.setText(rp(db.totalIncomeIncludingSalary(null,null).subtract(db.total("EXPENSE",null,null)))); salaryLabel.setText("Tersimpan: "+rp(db.getSalary(month))); renderGoals(); refreshTable(); reminder.setText("<html><b>Pengingat:</b> "+warnings()+"</html>"); }
    private String warnings(){ StringBuilder s=new StringBuilder(); BigDecimal sal=db.getSalary(month); for(Map.Entry<String,Double> e:GOALS.entrySet()){ BigDecimal lim=limit(e.getKey(),sal), used=db.categoryTotals(month).getOrDefault(e.getKey(),BigDecimal.ZERO); if(lim.signum()>0&&used.compareTo(lim.multiply(BigDecimal.valueOf(.8)))>=0)s.append("⚠ ").append(e.getKey()).append(" hampir/melewati batas. "); } return s.length()==0?"Semua budget masih aman.":s.toString(); }
    private void renderGoals(){ Container c=((JPanel)((JTabbedPane)getContentPane().getComponent(1)).getComponentAt(0)); /* cards are refreshed by rebuilding labels through progress bars below */ }
    private void refreshTable(){ LocalDate[] range=range(); table.setRowCount(0); for(Transaction t:db.findTransactions(range[0],range[1],"EXPENSE"))table.addRow(new Object[]{t.getDate(),"Pengeluaran",t.getCategory(),rp(t.getAmount()),t.getNote()}); }
    private LocalDate[] range(){ String x=(String)filter.getSelectedItem(); LocalDate now=LocalDate.now(); if("Hari ini".equals(x))return new LocalDate[]{now,now}; if("Minggu ini".equals(x))return new LocalDate[]{now.with(DayOfWeek.MONDAY),now}; if("Bulan ini".equals(x))return new LocalDate[]{now.withDayOfMonth(1),now}; if("Tahun ini".equals(x))return new LocalDate[]{now.withDayOfYear(1),now}; try{return new LocalDate[]{LocalDate.parse(from.getText()),LocalDate.parse(to.getText())};}catch(Exception e){return new LocalDate[]{now.withDayOfMonth(1),now};} }
    private String rp(BigDecimal x){return "Rp "+x.setScale(0,RoundingMode.HALF_UP).toPlainString();} private void error(String s){JOptionPane.showMessageDialog(this,s,"Input salah",JOptionPane.ERROR_MESSAGE);}
}
