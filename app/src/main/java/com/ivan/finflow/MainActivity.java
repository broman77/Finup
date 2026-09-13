package com.ivan.finflow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 13, 21);
    private static final int SURFACE = Color.rgb(18, 24, 36);
    private static final int SURFACE2 = Color.rgb(25, 33, 48);
    private static final int SURFACE3 = Color.rgb(32, 43, 62);
    private static final int TEXT = Color.rgb(244, 247, 250);
    private static final int MUTED = Color.rgb(150, 161, 178);
    private static final int ACCENT = Color.rgb(124, 223, 175);
    private static final int ACCENT2 = Color.rgb(98, 176, 255);
    private static final int EXPENSE = Color.rgb(255, 124, 124);
    private static final int WARNING = Color.rgb(255, 201, 92);
    private static final int WHITE_08 = Color.argb(22, 255, 255, 255);
    private static final int WHITE_12 = Color.argb(30, 255, 255, 255);

    private DbHelper db;
    private SharedPreferences prefs;
    private LinearLayout content;
    private LinearLayout nav;
    private int currentTab = 0;
    private int txFilter = 0; // 0 all, 1 expense, 2 income
    private int reportMode = 0; // 0 month, 1 year, 2 period

    private final int[] palette = {
        Color.rgb(124, 223, 175),
        Color.rgb(98, 176, 255),
        Color.rgb(255, 201, 92),
        Color.rgb(190, 145, 255),
        Color.rgb(255, 139, 188),
        Color.rgb(120, 220, 228)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildShell();
        showTab(0);
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        FrameLayout frame = new FrameLayout(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        frame.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(8), dp(6), dp(8), dp(10));
        nav.setBackgroundColor(SURFACE);
        String[] labels = {"Главная", "Операции", "Отчёты", "Настройки"};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setTextColor(i == 0 ? ACCENT : MUTED);
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setOnClickListener(v -> showTab(tab));
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }

    private void showTab(int index) {
        currentTab = index;
        for (int i = 0; i < nav.getChildCount(); i++) {
            ((Button) nav.getChildAt(i)).setTextColor(i == index ? ACCENT : MUTED);
        }
        content.removeAllViews();
        if (index == 0) showHome();
        else if (index == 1) showTransactions();
        else if (index == 2) showReports();
        else showSettings();
    }

    private ScrollView page(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(18), dp(18), dp(28));
        body.addView(text(title, 28, TEXT, true));
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = text(subtitle, 14, MUTED, false);
            s.setPadding(0, dp(4), 0, dp(16));
            body.addView(s);
        } else {
            spacer(body, 12);
        }
        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return scroll;
    }

    private LinearLayout bodyOf(ScrollView scroll) {
        return (LinearLayout) scroll.getChildAt(0);
    }

    private void showHome() {
        ScrollView scroll = page("Мои Финансы", monthCaption());
        LinearLayout body = bodyOf(scroll);

        long[] range = monthRange();
        double balance = db.getTotalBalance();
        double income = db.getMonthTotal("income", range[0], range[1]);
        double expense = db.getMonthTotal("expense", range[0], range[1]);
        double budget = prefs.getFloat("budget", 0f);
        List<DbHelper.AccountBalance> accounts = db.getAccountBalances();
        double savings = income - expense;

        body.addView(buildHeroCard(balance, income, expense, accounts.size()));

        sectionHeader(body, "Счета", accounts.isEmpty() ? "Добавьте счёт" : accounts.size() + " активн.");
        body.addView(buildAccountsScroller(accounts));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(statCard("Доходы", money(income), ACCENT), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.setMargins(dp(10), 0, 0, 0);
        row.addView(statCard("Расходы", money(expense), EXPENSE), lp2);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp3.setMargins(dp(10), 0, 0, 0);
        row.addView(statCard("Итог", money(savings), savings >= 0 ? ACCENT2 : WARNING), lp3);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, dp(16), 0, 0);
        body.addView(row, rowParams);

        if (budget > 0) {
            LinearLayout budgetCard = card();
            budgetCard.addView(text("Бюджет месяца", 17, TEXT, true));
            double left = budget - expense;
            TextView status = text(left >= 0 ? "Осталось " + money(left) : "Превышение на " + money(Math.abs(left)), 15, left >= 0 ? ACCENT : EXPENSE, true);
            status.setPadding(0, dp(8), 0, dp(6));
            budgetCard.addView(status);
            budgetCard.addView(progressBar(budget == 0 ? 0 : Math.min(1d, expense / budget), expense <= budget ? ACCENT : EXPENSE), paramsTop(dp(6)));
            TextView used = text("Потрачено " + money(expense) + " из " + money(budget), 13, MUTED, false);
            used.setPadding(0, dp(8), 0, 0);
            budgetCard.addView(used);
            body.addView(budgetCard, paramsTop(dp(16)));
        }

        Button add = primaryButton("+  Добавить операцию");
        add.setOnClickListener(v -> showAddTransactionDialog());
        body.addView(add, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56), 0, dp(16), 0, 0));

        sectionHeader(body, "Структура расходов", expense > 0 ? "Текущий месяц" : "Пока пусто");
        body.addView(buildCategoryOverview(range[0], range[1], 4));

        sectionHeader(body, "Последние операции", "Нажмите, чтобы изменить");
        List<DbHelper.Tx> txs = db.getTransactions(5);
        if (txs.isEmpty()) {
            body.addView(infoNote("Пока нет операций. Добавьте первую покупку или доход."));
        } else {
            for (DbHelper.Tx tx : txs) body.addView(txRow(tx, false));
        }
    }

    private void showTransactions() {
        ScrollView scroll = page("Операции", "Нажмите на операцию, чтобы изменить. Удерживайте, чтобы удалить");
        LinearLayout body = bodyOf(scroll);

        Button add = primaryButton("+  Новая операция");
        add.setOnClickListener(v -> showAddTransactionDialog());
        body.addView(add, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54), 0, 0, 0, dp(14)));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(filterChip("Все", txFilter == 0, () -> { txFilter = 0; showTab(1); }));
        chips.addView(filterChip("Расходы", txFilter == 1, () -> { txFilter = 1; showTab(1); }), chipMargin());
        chips.addView(filterChip("Доходы", txFilter == 2, () -> { txFilter = 2; showTab(1); }), chipMargin());
        body.addView(chips);

        List<DbHelper.Tx> source = db.getTransactions(0);
        List<DbHelper.Tx> filtered = new ArrayList<>();
        for (DbHelper.Tx tx : source) {
            if (txFilter == 0 || (txFilter == 1 && "expense".equals(tx.type)) || (txFilter == 2 && "income".equals(tx.type))) {
                filtered.add(tx);
            }
        }

        LinearLayout summary = card();
        summary.addView(text("Всего операций", 14, MUTED, true));
        TextView count = text(String.valueOf(filtered.size()), 28, TEXT, true);
        count.setPadding(0, dp(4), 0, dp(8));
        summary.addView(count);
        summary.addView(text(txFilter == 0 ? "Показаны все записи" : txFilter == 1 ? "Показаны только расходы" : "Показаны только доходы", 13, MUTED, false));
        body.addView(summary, paramsTop(dp(14)));

        if (filtered.isEmpty()) {
            body.addView(infoNote("Здесь появится история доходов и расходов."));
        } else {
            for (DbHelper.Tx tx : filtered) body.addView(txRow(tx, true));
        }
    }

    private void showReports() {
        ScrollView scroll = page("Отчёты", "Меняйте отчёт: за месяц, за год или за свой период");
        LinearLayout body = bodyOf(scroll);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(filterChip("Месяц", reportMode == 0, () -> { reportMode = 0; showTab(2); }));
        chips.addView(filterChip("Год", reportMode == 1, () -> { reportMode = 1; showTab(2); }), chipMargin());
        chips.addView(filterChip("Период", reportMode == 2, () -> { reportMode = 2; showTab(2); }), chipMargin());
        body.addView(chips);

        if (reportMode == 2) {
            body.addView(customPeriodCard(), paramsTop(dp(14)));
        }

        long[] range = getActiveReportRange();
        double income = db.getMonthTotal("income", range[0], range[1]);
        double expense = db.getMonthTotal("expense", range[0], range[1]);
        double result = income - expense;

        LinearLayout summary = card();
        summary.addView(text("Сводка", 18, TEXT, true));
        TextView total = text(money(result), 30, result >= 0 ? ACCENT2 : EXPENSE, true);
        total.setPadding(0, dp(8), 0, dp(10));
        summary.addView(total);
        summary.addView(metric("Период", activeReportLabel(), TEXT));
        summary.addView(metric("Доходы", money(income), ACCENT));
        summary.addView(metric("Расходы", money(expense), EXPENSE));
        summary.addView(metric("Итог", money(result), result >= 0 ? ACCENT2 : WARNING));
        body.addView(summary, paramsTop(dp(14)));

        if (reportMode == 1) {
            sectionHeader(body, "Расходы по месяцам", "Текущий год");
            body.addView(buildYearChart());
        } else {
            sectionHeader(body, "Расходы за 7 дней", reportMode == 0 ? "Текущий месяц" : "Быстрый обзор");
            body.addView(buildSevenDayChart());
        }

        sectionHeader(body, "Категории расходов", activeReportLabel());
        body.addView(buildCategoryOverview(range[0], range[1], 10));

        sectionHeader(body, "Баланс по счетам", null);
        body.addView(buildAccountsBalanceList());
    }

    private void showSettings() {
        ScrollView scroll = page("Настройки", "Счета, категории, бюджет, валюта и экспорт");
        LinearLayout body = bodyOf(scroll);

        LinearLayout budgetCard = card();
        budgetCard.addView(text("Месячный бюджет", 17, TEXT, true));
        EditText budget = input("Например, 50000");
        float existing = prefs.getFloat("budget", 0f);
        if (existing > 0) budget.setText(String.valueOf((int) existing));
        budget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        budgetCard.addView(budget, inputParams());
        Button saveBudget = secondaryButton("Сохранить бюджет");
        saveBudget.setOnClickListener(v -> {
            try {
                float value = Float.parseFloat(budget.getText().toString().trim().replace(',', '.'));
                prefs.edit().putFloat("budget", value).apply();
                toast("Бюджет сохранён");
                showTab(3);
            } catch (Exception e) {
                toast("Введите сумму цифрами");
            }
        });
        budgetCard.addView(saveBudget, buttonParams());
        body.addView(budgetCard);

        LinearLayout currencyCard = card();
        currencyCard.addView(text("Валюта", 17, TEXT, true));
        String[] currencies = {"RUB ₽", "USD $", "EUR €", "GBP £"};
        Spinner currency = spinner(currencies);
        setSpinnerSelection(currency, prefs.getString("currency", "RUB ₽"));
        currencyCard.addView(currency, inputParams());
        Button saveCurrency = secondaryButton("Сохранить валюту");
        saveCurrency.setOnClickListener(v -> {
            prefs.edit().putString("currency", String.valueOf(currency.getSelectedItem())).apply();
            toast("Валюта сохранена");
            showTab(3);
        });
        currencyCard.addView(saveCurrency, buttonParams());
        body.addView(currencyCard, paramsTop(dp(16)));

        LinearLayout accountsCard = card();
        accountsCard.addView(text("Счета", 17, TEXT, true));
        List<DbHelper.AccountBalance> accounts = db.getAccountBalances();
        if (accounts.isEmpty()) {
            TextView empty = text("Счета появятся здесь", 14, MUTED, false);
            empty.setPadding(0, dp(10), 0, dp(6));
            accountsCard.addView(empty);
        } else {
            for (DbHelper.AccountBalance acc : accounts) accountsCard.addView(accountSimpleRow(acc.name, money(acc.balance)));
        }
        Button addAccount = secondaryButton("+ Добавить счёт");
        addAccount.setOnClickListener(v -> showAddAccountDialog());
        accountsCard.addView(addAccount, buttonParams());
        body.addView(accountsCard, paramsTop(dp(16)));

        LinearLayout categoriesCard = card();
        categoriesCard.addView(text("Категории", 17, TEXT, true));
        TextView categoryInfo = text("Добавляйте свои категории, переименовывайте их и убирайте ненужные. Старые операции при удалении категории сохраняются.", 14, MUTED, false);
        categoryInfo.setPadding(0, dp(8), 0, dp(10));
        categoriesCard.addView(categoryInfo);

        LinearLayout catButtons = new LinearLayout(this);
        catButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button expenseCats = secondaryButton("Расходы · " + db.getCategoryCount("expense"));
        expenseCats.setOnClickListener(v -> showCategoryManagerDialog("expense"));
        Button incomeCats = secondaryButton("Доходы · " + db.getCategoryCount("income"));
        incomeCats.setOnClickListener(v -> showCategoryManagerDialog("income"));
        catButtons.addView(expenseCats, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout.LayoutParams incomeCatParams = new LinearLayout.LayoutParams(0, dp(50), 1f);
        incomeCatParams.setMargins(dp(10), 0, 0, 0);
        catButtons.addView(incomeCats, incomeCatParams);
        categoriesCard.addView(catButtons);
        body.addView(categoriesCard, paramsTop(dp(16)));

        LinearLayout exportCard = card();
        exportCard.addView(text("Экспорт данных", 17, TEXT, true));
        TextView exportInfo = text("Выгрузите все операции в настоящий Excel-файл .xlsx или сохраните резервную копию JSON.", 14, MUTED, false);
        exportInfo.setPadding(0, dp(8), 0, dp(8));
        exportCard.addView(exportInfo);
        Button excel = secondaryButton("Экспортировать все операции в Excel (.xlsx)");
        excel.setOnClickListener(v -> createExcelDocument());
        exportCard.addView(excel, buttonParams());
        Button backup = secondaryButton("Создать резервную копию JSON");
        backup.setOnClickListener(v -> createBackupDocument());
        exportCard.addView(backup, buttonParams());
        body.addView(exportCard, paramsTop(dp(16)));
    }

    private LinearLayout buildHeroCard(double balance, double income, double expense, int accountCount) {
        LinearLayout hero = card();
        hero.setBackground(createGradientCard());

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text("ОБЩИЙ БАЛАНС", 12, Color.argb(190, 255, 255, 255), true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(pill(accountCount + " сч.", WHITE_12, TEXT));
        hero.addView(top);

        TextView amount = text(money(balance), 34, TEXT, true);
        amount.setPadding(0, dp(10), 0, dp(8));
        hero.addView(amount);
        hero.addView(text("За месяц  + " + money(income) + "   − " + money(expense), 14, Color.argb(210, 255, 255, 255), false));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(14), 0, 0);
        chips.addView(pill("Доходы", Color.argb(36, 124, 223, 175), ACCENT));
        chips.addView(spaceX(dp(8)));
        chips.addView(pill("Расходы", Color.argb(36, 255, 124, 124), EXPENSE));
        hero.addView(chips);
        return hero;
    }

    private View buildAccountsScroller(List<DbHelper.AccountBalance> accounts) {
        if (accounts.isEmpty()) return infoNote("Нет счетов. Откройте «Настройки» и добавьте первый счёт.");
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        double total = Math.max(Math.abs(db.getTotalBalance()), 1d);

        for (int i = 0; i < accounts.size(); i++) {
            DbHelper.AccountBalance acc = accounts.get(i);
            int color = palette[i % palette.length];
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            card.setBackground(createRoundedDrawable(SURFACE2, dp(22), WHITE_08, 1));
            card.addView(text(acc.name, 16, TEXT, true));
            TextView sub = text("Текущий баланс", 12, MUTED, false);
            sub.setPadding(0, dp(4), 0, dp(10));
            card.addView(sub);
            card.addView(text(money(acc.balance), 22, color, true));
            card.addView(progressBar(Math.min(1d, Math.abs(acc.balance) / total), color), paramsTop(dp(12)));
            TextView share = text("Доля: " + (int) Math.round(Math.min(1d, Math.abs(acc.balance) / total) * 100) + "%", 12, MUTED, false);
            share.setPadding(0, dp(8), 0, 0);
            card.addView(share);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(240), ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) cp.setMargins(dp(12), 0, 0, 0);
            row.addView(card, cp);
        }
        hsv.addView(row);
        return hsv;
    }

    private View buildCategoryOverview(long fromInclusive, long toExclusive, int limit) {
        List<String[]> cats = db.getCategoryTotals(fromInclusive, toExclusive);
        if (cats.isEmpty()) return infoNote("Добавьте расходы — здесь появится распределение по категориям.");
        double total = 0;
        for (String[] c : cats) total += Double.parseDouble(c[1]);

        LinearLayout wrap = card();
        int shown = Math.min(limit, cats.size());
        for (int i = 0; i < shown; i++) {
            String[] c = cats.get(i);
            String name = c[0];
            double value = Double.parseDouble(c[1]);
            int color = palette[i % palette.length];
            wrap.addView(categoryRow(name, value, total, color, i > 0));
        }
        return wrap;
    }

    private View buildSevenDayChart() {
        List<DbHelper.LabelTotal> days = db.getLastDaysExpenseTotals(7);
        LinearLayout card = card();
        if (days.isEmpty()) {
            card.addView(text("График появится после первых трат.", 14, MUTED, false));
            return card;
        }
        double max = 1;
        for (DbHelper.LabelTotal d : days) max = Math.max(max, d.total);
        LinearLayout bars = new LinearLayout(this);
        bars.setOrientation(LinearLayout.HORIZONTAL);
        bars.setGravity(Gravity.BOTTOM);
        for (DbHelper.LabelTotal d : days) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            TextView amount = text(d.total <= 0 ? "0" : shortMoney(d.total), 11, MUTED, false);
            amount.setGravity(Gravity.CENTER_HORIZONTAL);
            col.addView(amount);
            View bar = new View(this);
            int h = (int) Math.max(dp(10), (d.total / max) * dp(110));
            bar.setBackground(createRoundedDrawable(d.total > 0 ? ACCENT2 : SURFACE3, dp(10), 0, 0));
            col.addView(bar, sizedParams(dp(24), h, 0, dp(8), 0, dp(8)));
            TextView label = text(d.label, 11, MUTED, false);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            col.addView(label);
            bars.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        card.addView(bars);
        return card;
    }

    private View buildYearChart() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        List<DbHelper.LabelTotal> months = db.getYearMonthExpenseTotals(year);
        LinearLayout card = card();
        double max = 1;
        for (DbHelper.LabelTotal m : months) max = Math.max(max, m.total);
        LinearLayout bars = new LinearLayout(this);
        bars.setOrientation(LinearLayout.HORIZONTAL);
        bars.setGravity(Gravity.BOTTOM);
        for (DbHelper.LabelTotal m : months) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            View bar = new View(this);
            int h = (int) Math.max(dp(6), (m.total / max) * dp(120));
            bar.setBackground(createRoundedDrawable(m.total > 0 ? ACCENT2 : SURFACE3, dp(8), 0, 0));
            col.addView(bar, sizedParams(dp(16), h, 0, 0, 0, dp(8)));
            TextView label = text(m.label, 10, MUTED, false);
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            col.addView(label);
            bars.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        card.addView(bars);
        TextView info = text("Траты по месяцам за " + year + " год", 12, MUTED, false);
        info.setPadding(0, dp(10), 0, 0);
        card.addView(info);
        return card;
    }

    private View buildAccountsBalanceList() {
        List<DbHelper.AccountBalance> accounts = db.getAccountBalances();
        if (accounts.isEmpty()) return infoNote("Счета ещё не добавлены.");
        LinearLayout card = card();
        double total = Math.max(Math.abs(db.getTotalBalance()), 1d);
        for (int i = 0; i < accounts.size(); i++) {
            DbHelper.AccountBalance acc = accounts.get(i);
            int color = palette[i % palette.length];
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            if (i > 0) row.setPadding(0, dp(12), 0, 0);
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.addView(text(acc.name, 15, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(text(money(acc.balance), 15, color, true));
            row.addView(top);
            row.addView(progressBar(Math.min(1d, Math.abs(acc.balance) / total), color), paramsTop(dp(8)));
            card.addView(row);
        }
        return card;
    }

    private View customPeriodCard() {
        LinearLayout card = card();
        long[] range = getCustomRange();
        card.addView(text("Свой период", 17, TEXT, true));
        TextView t = text(formatDateOnly(range[0]) + " — " + formatDateOnly(range[1] - 1), 15, ACCENT2, true);
        t.setPadding(0, dp(8), 0, dp(10));
        card.addView(t);
        card.addView(text("Можно выбрать любые даты начала и конца периода.", 13, MUTED, false));
        Button edit = secondaryButton("Изменить период");
        edit.setOnClickListener(v -> showCustomPeriodDialog());
        card.addView(edit, buttonParams());
        return card;
    }

    private View txRow(DbHelper.Tx tx, boolean allowDelete) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setBackground(createRoundedDrawable(SURFACE, dp(18), WHITE_08, 1));

        TextView icon = new TextView(this);
        icon.setText("expense".equals(tx.type) ? "↓" : "↑");
        icon.setTextSize(22);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor("expense".equals(tx.type) ? EXPENSE : ACCENT);
        icon.setBackground(createRoundedDrawable("expense".equals(tx.type) ? Color.argb(28, 255, 124, 124) : Color.argb(28, 124, 223, 175), dp(24), 0, 0));
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text(tx.category, 17, TEXT, true));
        TextView sub = text(formatDate(tx.createdAt) + "  •  " + tx.account + ((tx.note == null || tx.note.trim().isEmpty()) ? "" : "  •  " + tx.note), 12, MUTED, false);
        sub.setPadding(0, dp(4), 0, 0);
        info.addView(sub);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.setMargins(dp(12), 0, dp(12), 0);
        row.addView(info, infoParams);

        row.addView(text(("expense".equals(tx.type) ? "−" : "+") + money(tx.amount), 18, "expense".equals(tx.type) ? EXPENSE : ACCENT, true));
        row.setOnClickListener(v -> showEditTransactionDialog(tx));

        if (allowDelete) {
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Удалить операцию?")
                    .setMessage("Операция будет удалена без возможности восстановления.")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Удалить", (d, which) -> {
                        db.deleteTransaction(tx.id);
                        showTab(currentTab);
                    })
                    .show();
                return true;
            });
        }

        return rowWithTopMargin(row, dp(8));
    }

    private void showAddTransactionDialog() {
        LinearLayout box = dialogBox();
        Spinner type = spinner(new String[]{"Расход", "Доход"});
        EditText amount = input("Например, 1250");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Spinner category = spinner(categoriesFor("expense", null));
        List<String> accounts = db.getAccounts();
        if (accounts.isEmpty()) accounts.add("Основной счёт");
        Spinner account = spinner(accounts.toArray(new String[0]));
        EditText note = input("Комментарий (необязательно)");

        box.addView(label("Тип операции")); box.addView(type, inputParams());
        box.addView(label("Сумма")); box.addView(amount, inputParams());
        box.addView(label("Категория")); box.addView(category, inputParams());
        TextView categoryHint = text("Свои категории: Настройки → Категории", 11, MUTED, false);
        categoryHint.setPadding(0, 0, 0, dp(6));
        box.addView(categoryHint);
        box.addView(label("Счёт")); box.addView(account, inputParams());
        box.addView(label("Комментарий")); box.addView(note, inputParams());

        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                category.setAdapter(spinnerAdapter(categoriesFor(position == 0 ? "expense" : "income", null)));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Новая операция")
            .setView(box)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                double a = Double.parseDouble(amount.getText().toString().trim().replace(',', '.'));
                if (a <= 0) throw new RuntimeException();
                String txType = type.getSelectedItemPosition() == 0 ? "expense" : "income";
                db.addTransaction(txType, a, String.valueOf(category.getSelectedItem()), String.valueOf(account.getSelectedItem()), note.getText().toString().trim(), System.currentTimeMillis());
                dialog.dismiss();
                showTab(currentTab);
            } catch (Exception e) {
                toast("Введите корректную сумму");
            }
        }));
        dialog.show();
    }

    private void showEditTransactionDialog(DbHelper.Tx tx) {
        LinearLayout box = dialogBox();
        Spinner type = spinner(new String[]{"Расход", "Доход"});
        type.setSelection("income".equals(tx.type) ? 1 : 0);
        EditText amount = input("Например, 1250");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(trimDouble(tx.amount));
        Spinner category = spinner(categoriesFor(tx.type, tx.category));
        List<String> accounts = db.getAccounts();
        if (!accounts.contains(tx.account)) accounts.add(tx.account);
        Spinner account = spinner(accounts.toArray(new String[0]));
        setSpinnerSelection(account, tx.account);
        EditText note = input("Комментарий (необязательно)");
        if (tx.note != null) note.setText(tx.note);

        box.addView(label("Тип операции")); box.addView(type, inputParams());
        box.addView(label("Сумма")); box.addView(amount, inputParams());
        box.addView(label("Категория")); box.addView(category, inputParams());
        TextView categoryHint = text("Свои категории: Настройки → Категории", 11, MUTED, false);
        categoryHint.setPadding(0, 0, 0, dp(6));
        box.addView(categoryHint);
        box.addView(label("Счёт")); box.addView(account, inputParams());
        box.addView(label("Комментарий")); box.addView(note, inputParams());

        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String currentCategory = String.valueOf(category.getSelectedItem());
                String targetType = position == 0 ? "expense" : "income";
                String extra = targetType.equals(tx.type) ? tx.category : null;
                category.setAdapter(spinnerAdapter(categoriesFor(targetType, extra)));
                boolean found = setSpinnerSelection(category, currentCategory);
                if (!found) category.setSelection(0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        setSpinnerSelection(category, tx.category);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Редактировать операцию")
            .setView(box)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                double a = Double.parseDouble(amount.getText().toString().trim().replace(',', '.'));
                if (a <= 0) throw new RuntimeException();
                String txType = type.getSelectedItemPosition() == 0 ? "expense" : "income";
                boolean ok = db.updateTransaction(tx.id, txType, a, String.valueOf(category.getSelectedItem()), String.valueOf(account.getSelectedItem()), note.getText().toString().trim(), tx.createdAt);
                if (!ok) throw new RuntimeException();
                dialog.dismiss();
                toast("Операция обновлена");
                showTab(currentTab);
            } catch (Exception e) {
                toast("Не удалось сохранить изменения");
            }
        }));
        dialog.show();
    }

    private String[] categoriesFor(String type, String extra) {
        List<String> list = db.getCategories(type);
        if (extra != null && !extra.trim().isEmpty() && !list.contains(extra)) list.add(extra);
        if (list.isEmpty()) list.add("Другое");
        return list.toArray(new String[0]);
    }

    private void showCategoryManagerDialog(String type) {
        boolean expense = "expense".equals(type);
        List<String> categories = db.getCategories(type);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(8));

        TextView info = text(expense
            ? "Категории расходов используются при добавлении покупок и в отчётах."
            : "Категории доходов используются для зарплаты и других поступлений.", 13, MUTED, false);
        info.setPadding(0, 0, 0, dp(10));
        box.addView(info);

        Button add = primaryButton("+  Добавить свою категорию");
        box.addView(add, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50), 0, 0, 0, dp(10)));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        final AlertDialog[] holder = new AlertDialog[1];
        for (int i = 0; i < categories.size(); i++) {
            String name = categories.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(8), dp(10));
            row.setBackground(createRoundedDrawable(SURFACE2, dp(16), WHITE_08, 1));

            TextView nameView = text(name, 15, TEXT, true);
            row.addView(nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button edit = compactButton("Изм.");
            edit.setOnClickListener(v -> {
                if (holder[0] != null) holder[0].dismiss();
                showCategoryNameDialog(type, name);
            });
            row.addView(edit, new LinearLayout.LayoutParams(dp(64), dp(40)));

            Button delete = compactDangerButton("×");
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(44), dp(40));
            deleteParams.setMargins(dp(6), 0, 0, 0);
            delete.setOnClickListener(v -> confirmDeleteCategory(type, name, holder[0]));
            row.addView(delete, deleteParams);

            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) rp.setMargins(0, dp(8), 0, 0);
            list.addView(row, rp);
        }

        if (categories.isEmpty()) list.addView(infoNote("Пока нет категорий."));
        box.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(360)));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(expense ? "Категории расходов" : "Категории доходов")
            .setView(box)
            .setNegativeButton("Закрыть", null)
            .create();
        holder[0] = dialog;

        add.setOnClickListener(v -> {
            dialog.dismiss();
            showCategoryNameDialog(type, null);
        });
        dialog.show();
    }

    private void showCategoryNameDialog(String type, String oldName) {
        boolean editing = oldName != null;
        LinearLayout box = dialogBox();
        EditText name = input(editing ? "Новое название" : "Например, Подписки");
        if (editing) name.setText(oldName);
        box.addView(label("Название категории"));
        box.addView(name, inputParams());

        TextView hint = text("Категория будет доступна только для " + ("expense".equals(type) ? "расходов" : "доходов") + ".", 12, MUTED, false);
        hint.setPadding(0, 0, 0, dp(4));
        box.addView(hint);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(editing ? "Переименовать категорию" : "Новая категория")
            .setView(box)
            .setNegativeButton("Отмена", (d, which) -> showCategoryManagerDialog(type))
            .setPositiveButton(editing ? "Сохранить" : "Добавить", null)
            .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = name.getText().toString().trim();
            if (newName.isEmpty()) {
                toast("Введите название категории");
                return;
            }
            for (String existing : db.getCategories(type)) {
                if (existing.equalsIgnoreCase(newName) && (!editing || !existing.equalsIgnoreCase(oldName))) {
                    toast("Такая категория уже есть");
                    return;
                }
            }

            boolean ok = editing
                ? db.renameCategory(type, oldName, newName)
                : db.addCategory(type, newName);
            if (!ok) {
                toast("Не удалось сохранить категорию");
                return;
            }
            dialog.dismiss();
            toast(editing ? "Категория переименована" : "Категория добавлена");
            showTab(3);
            showCategoryManagerDialog(type);
        }));
        dialog.show();
    }

    private void confirmDeleteCategory(String type, String name, AlertDialog managerDialog) {
        if (db.getCategoryCount(type) <= 1) {
            toast("Нельзя удалить последнюю категорию");
            return;
        }
        int used = db.getCategoryUsageCount(type, name);
        String message = used > 0
            ? "Категория используется в " + used + " операц. Старые операции сохранятся, но категория исчезнет из списка для новых операций."
            : "Категория исчезнет из списка для новых операций.";

        new AlertDialog.Builder(this)
            .setTitle("Удалить «" + name + "»?")
            .setMessage(message)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить", (d, which) -> {
                if (!db.deleteCategory(type, name)) {
                    toast("Не удалось удалить категорию");
                    return;
                }
                if (managerDialog != null) managerDialog.dismiss();
                toast("Категория удалена");
                showTab(3);
                showCategoryManagerDialog(type);
            })
            .show();
    }

    private Button compactButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(ACCENT2);
        b.setBackground(createRoundedDrawable(Color.argb(28, 98, 176, 255), dp(14), 0, 0));
        b.setPadding(dp(4), 0, dp(4), 0);
        return b;
    }

    private Button compactDangerButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(20);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(EXPENSE);
        b.setBackground(createRoundedDrawable(Color.argb(28, 255, 124, 124), dp(14), 0, 0));
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private void showAddAccountDialog() {
        LinearLayout box = dialogBox();
        EditText name = input("Например, Карта или Наличные");
        EditText opening = input("Начальный баланс");
        opening.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        box.addView(label("Название")); box.addView(name, inputParams());
        box.addView(label("Начальный баланс")); box.addView(opening, inputParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Новый счёт")
            .setView(box)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Добавить", null)
            .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) { toast("Введите название счёта"); return; }
            double b = 0;
            try {
                if (!opening.getText().toString().trim().isEmpty()) b = Double.parseDouble(opening.getText().toString().trim().replace(',', '.'));
            } catch (Exception e) { toast("Некорректный баланс"); return; }
            if (!db.addAccount(n, b)) { toast("Такой счёт уже существует"); return; }
            dialog.dismiss();
            showTab(3);
        }));
        dialog.show();
    }

    private void showCustomPeriodDialog() {
        LinearLayout box = dialogBox();
        long[] current = getCustomRange();
        EditText from = input("01.09.2026");
        EditText to = input("30.09.2026");
        from.setText(formatDateOnly(current[0]));
        to.setText(formatDateOnly(current[1] - 1));
        box.addView(label("Дата начала (дд.мм.гггг)")); box.addView(from, inputParams());
        box.addView(label("Дата конца (дд.мм.гггг)")); box.addView(to, inputParams());

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Свой период отчёта")
            .setView(box)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Long fromValue = parseDateStart(from.getText().toString().trim());
            Long toValue = parseDateStart(to.getText().toString().trim());
            if (fromValue == null || toValue == null) { toast("Введите даты в формате дд.мм.гггг"); return; }
            if (toValue < fromValue) { toast("Дата конца должна быть не раньше даты начала"); return; }
            prefs.edit().putLong("report_custom_from", fromValue).putLong("report_custom_to", toValue + 24L * 60L * 60L * 1000L).apply();
            dialog.dismiss();
            showTab(2);
        }));
        dialog.show();
    }

    private void createExcelDocument() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String stamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "finflow-operations-" + stamp + ".xlsx");
        startActivityForResult(intent, 43);
    }

    private void createBackupDocument() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "finflow-backup.json");
        startActivityForResult(intent, 42);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == 43) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new RuntimeException("No output stream");
                writeExcelWorkbook(os);
                toast("Excel-файл сохранён");
            } catch (Exception e) {
                toast("Не удалось экспортировать Excel");
            }
            return;
        }

        if (requestCode == 42) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new RuntimeException("No output stream");
                os.write(backupJson().getBytes(StandardCharsets.UTF_8));
                toast("Резервная копия сохранена");
            } catch (Exception e) {
                toast("Не удалось сохранить файл");
            }
        }
    }

    private void writeExcelWorkbook(OutputStream outputStream) throws Exception {
        List<DbHelper.Tx> transactions = db.getTransactions(0);
        String currency = prefs.getString("currency", "RUB ₽");
        String symbol = currency.contains("£") ? "£" : currency.contains("€") ? "€" : currency.contains("$") ? "$" : "₽";

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
            zipText(zip, "[Content_Types].xml",
                "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
                "<Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'>" +
                "<Default Extension='rels' ContentType='application/vnd.openxmlformats-package.relationships+xml'/>" +
                "<Default Extension='xml' ContentType='application/xml'/>" +
                "<Override PartName='/xl/workbook.xml' ContentType='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml'/>" +
                "<Override PartName='/xl/worksheets/sheet1.xml' ContentType='application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml'/>" +
                "<Override PartName='/xl/styles.xml' ContentType='application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml'/>" +
                "<Override PartName='/docProps/core.xml' ContentType='application/vnd.openxmlformats-package.core-properties+xml'/>" +
                "<Override PartName='/docProps/app.xml' ContentType='application/vnd.openxmlformats-officedocument.extended-properties+xml'/>" +
                "</Types>");

            zipText(zip, "_rels/.rels",
                "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
                "<Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'>" +
                "<Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='xl/workbook.xml'/>" +
                "<Relationship Id='rId2' Type='http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties' Target='docProps/core.xml'/>" +
                "<Relationship Id='rId3' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties' Target='docProps/app.xml'/>" +
                "</Relationships>");

            zipText(zip, "docProps/core.xml",
                "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
                "<cp:coreProperties xmlns:cp='http://schemas.openxmlformats.org/package/2006/metadata/core-properties' " +
                "xmlns:dc='http://purl.org/dc/elements/1.1/' xmlns:dcterms='http://purl.org/dc/terms/' " +
                "xmlns:dcmitype='http://purl.org/dc/dcmitype/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>" +
                "<dc:title>Мои Финансы — операции</dc:title><dc:creator>Мои Финансы</dc:creator>" +
                "</cp:coreProperties>");

            zipText(zip, "docProps/app.xml",
                "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
                "<Properties xmlns='http://schemas.openxmlformats.org/officeDocument/2006/extended-properties' " +
                "xmlns:vt='http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes'>" +
                "<Application>Мои Финансы</Application></Properties>");

            zipText(zip, "xl/workbook.xml",
                "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
                "<workbook xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main' " +
                "xmlns:r='http://schemas.openxmlformats.org/officeDocument/2006/relationships'>" +
                "<sheets><sheet name='Операции' sheetId='1' r:id='rId1'/></sheets></workbook>");

            zipText(zip, "xl/_rels/workbook.xml.rels",
                "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
                "<Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'>" +
                "<Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet' Target='worksheets/sheet1.xml'/>" +
                "<Relationship Id='rId2' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles' Target='styles.xml'/>" +
                "</Relationships>");

            zipText(zip, "xl/styles.xml", excelStylesXml());
            zipText(zip, "xl/worksheets/sheet1.xml", operationsSheetXml(transactions, symbol));
        }
    }

    private String excelStylesXml() {
        return "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
            "<styleSheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'>" +
            "<fonts count='3'>" +
            "<font><sz val='11'/><name val='Aptos'/></font>" +
            "<font><b/><color rgb='FFFFFFFF'/><sz val='11'/><name val='Aptos'/></font>" +
            "<font><b/><color rgb='FF169B62'/><sz val='11'/><name val='Aptos'/></font>" +
            "</fonts>" +
            "<fills count='3'><fill><patternFill patternType='none'/></fill><fill><patternFill patternType='gray125'/></fill>" +
            "<fill><patternFill patternType='solid'><fgColor rgb='FF172133'/><bgColor indexed='64'/></patternFill></fill></fills>" +
            "<borders count='1'><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count='1'><xf numFmtId='0' fontId='0' fillId='0' borderId='0'/></cellStyleXfs>" +
            "<cellXfs count='4'>" +
            "<xf numFmtId='0' fontId='0' fillId='0' borderId='0' xfId='0'/>" +
            "<xf numFmtId='0' fontId='1' fillId='2' borderId='0' xfId='0' applyFont='1' applyFill='1'><alignment vertical='center'/></xf>" +
            "<xf numFmtId='4' fontId='0' fillId='0' borderId='0' xfId='0' applyNumberFormat='1'/>" +
            "<xf numFmtId='0' fontId='2' fillId='0' borderId='0' xfId='0' applyFont='1'/>" +
            "</cellXfs>" +
            "<cellStyles count='1'><cellStyle name='Normal' xfId='0' builtinId='0'/></cellStyles>" +
            "</styleSheet>";
    }

    private String operationsSheetXml(List<DbHelper.Tx> transactions, String symbol) {
        StringBuilder x = new StringBuilder(4096 + transactions.size() * 350);
        int lastRow = transactions.size() + 1;
        x.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
        x.append("<worksheet xmlns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'>");
        x.append("<dimension ref='A1:G").append(lastRow).append("'/>");
        x.append("<sheetViews><sheetView workbookViewId='0'><pane ySplit='1' topLeftCell='A2' activePane='bottomLeft' state='frozen'/></sheetView></sheetViews>");
        x.append("<cols>");
        x.append("<col min='1' max='1' width='19' customWidth='1'/>");
        x.append("<col min='2' max='2' width='12' customWidth='1'/>");
        x.append("<col min='3' max='3' width='18' customWidth='1'/>");
        x.append("<col min='4' max='4' width='20' customWidth='1'/>");
        x.append("<col min='5' max='5' width='16' customWidth='1'/>");
        x.append("<col min='6' max='6' width='10' customWidth='1'/>");
        x.append("<col min='7' max='7' width='36' customWidth='1'/>");
        x.append("</cols><sheetData>");

        x.append("<row r='1' ht='24' customHeight='1'>");
        String[] headers = {"Дата", "Тип", "Категория", "Счёт", "Сумма", "Валюта", "Комментарий"};
        for (int i = 0; i < headers.length; i++) {
            x.append(inlineCell(columnName(i + 1) + "1", headers[i], 1));
        }
        x.append("</row>");

        int row = 2;
        for (DbHelper.Tx tx : transactions) {
            x.append("<row r='").append(row).append("'>");
            x.append(inlineCell("A" + row, new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru", "RU")).format(new Date(tx.createdAt)), 0));
            x.append(inlineCell("B" + row, "expense".equals(tx.type) ? "Расход" : "Доход", 0));
            x.append(inlineCell("C" + row, tx.category, 0));
            x.append(inlineCell("D" + row, tx.account, 0));
            x.append("<c r='E").append(row).append("' s='2'><v>").append(tx.amount).append("</v></c>");
            x.append(inlineCell("F" + row, symbol, 3));
            x.append(inlineCell("G" + row, tx.note == null ? "" : tx.note, 0));
            x.append("</row>");
            row++;
        }
        x.append("</sheetData>");
        x.append("<autoFilter ref='A1:G").append(lastRow).append("'/>");
        x.append("</worksheet>");
        return x.toString();
    }

    private String inlineCell(String ref, String value, int style) {
        return "<c r='" + ref + "' t='inlineStr' s='" + style + "'><is><t xml:space='preserve'>" + xmlEscape(value) + "</t></is></c>";
    }

    private String columnName(int column) {
        StringBuilder result = new StringBuilder();
        int n = column;
        while (n > 0) {
            n--;
            result.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return result.toString();
    }

    private String xmlEscape(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&apos;"); break;
                default:
                    if (ch >= 0x20 || ch == '\n' || ch == '\r' || ch == '\t') out.append(ch);
            }
        }
        return out.toString();
    }

    private void zipText(ZipOutputStream zip, String path, String text) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String backupJson() {
        StringBuilder s = new StringBuilder();
        s.append("{\"app\":\"FinFlow\",\"version\":3,\"transactions\":[");
        List<DbHelper.Tx> list = db.getTransactions(0);
        for (int i = 0; i < list.size(); i++) {
            DbHelper.Tx t = list.get(i);
            if (i > 0) s.append(',');
            s.append("{\"type\":\"").append(esc(t.type)).append("\",\"amount\":").append(t.amount)
                .append(",\"category\":\"").append(esc(t.category)).append("\",\"account\":\"").append(esc(t.account))
                .append("\",\"note\":\"").append(esc(t.note == null ? "" : t.note)).append("\",\"createdAt\":").append(t.createdAt).append('}');
        }
        s.append("]}");
        return s.toString();
    }

    private long[] getActiveReportRange() {
        if (reportMode == 1) return yearRange();
        if (reportMode == 2) return getCustomRange();
        return monthRange();
    }

    private String activeReportLabel() {
        if (reportMode == 1) {
            return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        }
        if (reportMode == 2) {
            long[] r = getCustomRange();
            return formatDateOnly(r[0]) + " — " + formatDateOnly(r[1] - 1);
        }
        return monthCaption();
    }

    private long[] getCustomRange() {
        long[] month = monthRange();
        long from = prefs.getLong("report_custom_from", month[0]);
        long to = prefs.getLong("report_custom_to", month[1]);
        if (to <= from) to = from + 24L * 60L * 60L * 1000L;
        return new long[]{from, to};
    }

    private long[] monthRange() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long from = c.getTimeInMillis();
        c.add(Calendar.MONTH, 1);
        return new long[]{from, c.getTimeInMillis()};
    }

    private long[] yearRange() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long from = c.getTimeInMillis();
        c.add(Calendar.YEAR, 1);
        return new long[]{from, c.getTimeInMillis()};
    }

    private String monthCaption() {
        SimpleDateFormat f = new SimpleDateFormat("LLLL yyyy", new Locale("ru", "RU"));
        String s = f.format(new Date());
        return s.substring(0, 1).toUpperCase(new Locale("ru", "RU")) + s.substring(1);
    }

    private String money(double value) {
        String c = prefs.getString("currency", "RUB ₽");
        String symbol = c.contains("£") ? "£" : c.contains("€") ? "€" : c.contains("$") ? "$" : "₽";
        NumberFormat f = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        f.setMinimumFractionDigits(0);
        f.setMaximumFractionDigits(2);
        return f.format(value) + " " + symbol;
    }

    private String shortMoney(double value) {
        if (Math.abs(value) >= 1000) return ((int) Math.round(value / 1000d)) + "k";
        return String.valueOf((int) Math.round(value));
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("d MMM, HH:mm", new Locale("ru", "RU")).format(new Date(timestamp));
    }

    private String formatDateOnly(long timestamp) {
        return new SimpleDateFormat("dd.MM.yyyy", new Locale("ru", "RU")).format(new Date(timestamp));
    }

    private Long parseDateStart(String value) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru", "RU"));
            f.setLenient(false);
            Date d = f.parse(value);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis();
        } catch (Exception e) {
            return null;
        }
    }

    private String trimDouble(double value) {
        long asLong = (long) value;
        if (Math.abs(value - asLong) < 0.0000001d) return String.valueOf(asLong);
        return String.valueOf(value);
    }

    private LinearLayout statCard(String title, String value, int color) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(14), dp(14), dp(14), dp(14));
        l.setBackground(createRoundedDrawable(SURFACE, dp(18), WHITE_08, 1));
        l.addView(text(title, 13, MUTED, false));
        TextView v = text(value, 19, color, true);
        v.setPadding(0, dp(8), 0, 0);
        l.addView(v);
        return l;
    }

    private LinearLayout metric(String name, String value, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, 0);
        row.addView(text(name, 14, MUTED, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text(value, 15, color, true));
        return row;
    }

    private LinearLayout categoryRow(String name, double value, double total, int color, boolean withMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        if (withMargin) row.setPadding(0, dp(12), 0, 0);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.addView(text(name, 15, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        int pct = total <= 0 ? 0 : (int) Math.round((value / total) * 100);
        top.addView(text(money(value) + "  •  " + pct + "%", 13, MUTED, false));
        row.addView(top);
        row.addView(progressBar(total <= 0 ? 0 : value / total, color), paramsTop(dp(8)));
        return row;
    }

    private LinearLayout accountSimpleRow(String name, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, 0);
        row.addView(text(name, 14, TEXT, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text(value, 14, ACCENT2, true));
        return row;
    }

    private TextView pill(String label, int bgColor, int textColor) {
        TextView t = text(label, 12, textColor, true);
        t.setPadding(dp(10), dp(6), dp(10), dp(6));
        t.setBackground(createRoundedDrawable(bgColor, dp(100), 0, 0));
        return t;
    }

    private View progressBar(double ratio, int color) {
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(createRoundedDrawable(SURFACE3, dp(8), 0, 0));
        int filled = (int) Math.max(0, Math.min(1000, Math.round(ratio * 1000)));
        int empty = 1000 - filled;
        View fill = new View(this);
        fill.setBackground(createRoundedDrawable(color, dp(8), 0, 0));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(8), filled));
        View rest = new View(this);
        track.addView(rest, new LinearLayout.LayoutParams(0, dp(8), Math.max(1, empty)));
        return track;
    }

    private View infoNote(String msg) {
        TextView t = text(msg, 14, MUTED, false);
        t.setPadding(dp(14), dp(14), dp(14), dp(14));
        t.setBackground(createRoundedDrawable(SURFACE, dp(18), WHITE_08, 1));
        return rowWithTopMargin(t, dp(8));
    }

    private Button filterChip(String label, boolean selected, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(selected ? BG : TEXT);
        b.setBackground(createRoundedDrawable(selected ? ACCENT : SURFACE, dp(100), 0, 0));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout.LayoutParams chipMargin() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        p.setMargins(dp(8), 0, 0, 0);
        return p;
    }

    private void sectionHeader(LinearLayout body, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(title, 18, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (subtitle != null) row.addView(text(subtitle, 12, MUTED, false));
        body.addView(row, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, dp(22), 0, dp(10)));
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView label(String value) {
        TextView t = text(value, 12, MUTED, true);
        t.setPadding(0, dp(8), 0, dp(4));
        return t;
    }

    private Button primaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(BG);
        b.setBackground(createRoundedDrawable(ACCENT, dp(18), 0, 0));
        return b;
    }

    private Button secondaryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(TEXT);
        b.setBackground(createRoundedDrawable(SURFACE2, dp(16), WHITE_08, 1));
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(15);
        e.setBackground(createRoundedDrawable(SURFACE2, dp(16), 0, 0));
        e.setPadding(dp(14), 0, dp(14), 0);
        return e;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        s.setAdapter(spinnerAdapter(values));
        return s;
    }

    private ArrayAdapter<String> spinnerAdapter(String[] values) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(TEXT);
                v.setTextSize(15);
                v.setPadding(dp(12), 0, dp(12), 0);
                v.setBackground(createRoundedDrawable(SURFACE2, dp(16), 0, 0));
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getDropDownView(position, convertView, parent);
                v.setTextColor(TEXT);
                v.setBackgroundColor(SURFACE2);
                v.setPadding(dp(12), dp(12), dp(12), dp(12));
                return v;
            }
        };
    }

    private boolean setSpinnerSelection(Spinner spinner, String value) {
        if (spinner.getAdapter() == null) return false;
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (value.equals(String.valueOf(spinner.getAdapter().getItem(i)))) {
                spinner.setSelection(i);
                return true;
            }
        }
        return false;
    }

    private LinearLayout dialogBox() {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(22), dp(6), dp(22), dp(4));
        return b;
    }

    private LinearLayout.LayoutParams inputParams() {
        return sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52), 0, 0, 0, dp(8));
    }

    private LinearLayout.LayoutParams buttonParams() {
        return sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50), 0, dp(10), 0, 0);
    }

    private LinearLayout.LayoutParams paramsTop(int top) {
        return sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, top, 0, 0);
    }

    private LinearLayout.LayoutParams sizedParams(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.setMargins(left, top, right, bottom);
        return p;
    }

    private View rowWithTopMargin(View child, int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, topMargin, 0, 0);
        child.setLayoutParams(p);
        return child;
    }

    private View spaceX(int width) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(width, 1));
        return v;
    }

    private void spacer(LinearLayout parent, int heightDp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private GradientDrawable createRoundedDrawable(int color, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (strokeColor != 0 && strokeWidthDp > 0) g.setStroke(dp(strokeWidthDp), strokeColor);
        return g;
    }

    private GradientDrawable createGradientCard() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(30, 55, 96), Color.rgb(20, 30, 50)});
        g.setCornerRadius(dp(24));
        return g;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setBackground(createRoundedDrawable(SURFACE, dp(22), WHITE_08, 1));
        return l;
    }

    private String esc(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
