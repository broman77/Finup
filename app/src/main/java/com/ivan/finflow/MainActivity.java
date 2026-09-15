package com.ivan.finflow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.content.ContentResolver;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int REQ_CREATE_EXCEL = 41;
    private static final int REQ_CREATE_BACKUP = 42;
    private static final int REQ_OPEN_BACKUP = 43;

    private int BG;
    private int SURFACE;
    private int SURFACE2;
    private int SURFACE3;
    private int TEXT;
    private int MUTED;
    private int ACCENT;
    private int ACCENT2;
    private int EXPENSE;
    private int WARNING;
    private int WHITE_08;
    private int WHITE_12;
    private int ON_ACCENT = Color.rgb(12, 18, 26);
    private boolean isLightTheme = false;

    private DbHelper db;
    private SharedPreferences prefs;
    private LinearLayout content;
    private LinearLayout nav;
    private int currentTab = 0;
    private int reportMode = 0; // 0 month, 1 year, 2 period
    private boolean viewingAllOperations = false;
    private Uri pendingImportUri;

    private int[] palette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DbHelper(this);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        applyAppearancePrefs();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(SURFACE);
        buildShell();
        showTab(0);
    }

    private void applyAppearancePrefs() {
        String themeMode = prefs.getString("theme_mode", "dark");
        boolean dark;
        if ("system".equals(themeMode)) {
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            dark = nightModeFlags != Configuration.UI_MODE_NIGHT_NO;
        } else {
            dark = !"light".equals(themeMode);
        }
        isLightTheme = !dark;

        if (dark) {
            BG = Color.rgb(10, 13, 21);
            SURFACE = Color.rgb(18, 24, 36);
            SURFACE2 = Color.rgb(25, 33, 48);
            SURFACE3 = Color.rgb(32, 43, 62);
            TEXT = Color.rgb(244, 247, 250);
            MUTED = Color.rgb(150, 161, 178);
            WHITE_08 = Color.argb(22, 255, 255, 255);
            WHITE_12 = Color.argb(30, 255, 255, 255);
            ON_ACCENT = Color.rgb(9, 16, 24);
        } else {
            BG = Color.rgb(245, 247, 251);
            SURFACE = Color.rgb(255, 255, 255);
            SURFACE2 = Color.rgb(247, 249, 253);
            SURFACE3 = Color.rgb(227, 233, 242);
            TEXT = Color.rgb(24, 31, 44);
            MUTED = Color.rgb(102, 113, 130);
            WHITE_08 = Color.argb(16, 20, 28, 41);
            WHITE_12 = Color.argb(28, 20, 28, 41);
            ON_ACCENT = Color.rgb(12, 18, 26);
        }

        String accentMode = prefs.getString("accent_mode", "mint");
        if ("blue".equals(accentMode)) {
            ACCENT = Color.rgb(95, 153, 255);
            ACCENT2 = Color.rgb(62, 122, 233);
        } else if ("purple".equals(accentMode)) {
            ACCENT = Color.rgb(177, 128, 255);
            ACCENT2 = Color.rgb(127, 98, 255);
        } else if ("coral".equals(accentMode)) {
            ACCENT = Color.rgb(255, 146, 102);
            ACCENT2 = Color.rgb(255, 112, 131);
        } else {
            ACCENT = Color.rgb(124, 223, 175);
            ACCENT2 = Color.rgb(98, 176, 255);
        }
        EXPENSE = Color.rgb(255, 124, 124);
        WARNING = Color.rgb(255, 201, 92);
        palette = new int[]{ACCENT, ACCENT2, WARNING, Color.rgb(190, 145, 255), Color.rgb(255, 139, 188), Color.rgb(120, 220, 228)};
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
        String[] labels = {"Операции", "Отчёты", "Настройки"};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setSingleLine(true);
            b.setGravity(Gravity.CENTER);
            b.setPadding(0, 0, 0, 0);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setTextColor(i == 0 ? ACCENT : MUTED);
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setOnClickListener(v -> showTab(tab));
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(54), 1f));
        }
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
    }


    private void showTab(int index) {
        viewingAllOperations = false;
        currentTab = index;
        for (int i = 0; i < nav.getChildCount(); i++) {
            ((Button) nav.getChildAt(i)).setTextColor(i == index ? ACCENT : MUTED);
        }
        content.removeAllViews();
        if (index == 0) showHome();
        else if (index == 1) showReports();
        else showSettings();
    }

    private ScrollView page(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pagePad = responsivePagePaddingDp();
        body.setPadding(dp(pagePad), dp(isCompactPhone() ? 12 : 16), dp(pagePad), dp(28));
        if (title != null && !title.isEmpty()) {
            body.addView(text(title, 28, TEXT, true));
            if (subtitle != null && !subtitle.isEmpty()) {
                TextView s = text(subtitle, 14, MUTED, false);
                s.setPadding(0, dp(4), 0, dp(16));
                body.addView(s);
            } else {
                spacer(body, 12);
            }
        } else if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = text(subtitle, 14, MUTED, false);
            s.setPadding(0, 0, 0, dp(16));
            body.addView(s);
        } else {
            spacer(body, 8);
        }
        scroll.addView(body);
        content.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return scroll;
    }

    private LinearLayout bodyOf(ScrollView scroll) {
        return (LinearLayout) scroll.getChildAt(0);
    }

    private void showHome() {
        ScrollView scroll = page(null, null);
        LinearLayout body = bodyOf(scroll);
        long[] range = monthRange();
        double balance = db.getTotalBalance();
        double income = db.getMonthTotal("income", range[0], range[1]);
        double expense = db.getMonthTotal("expense", range[0], range[1]);
        double budget = prefs.getFloat("budget", 0f);
        List<DbHelper.AccountBalance> accounts = db.getAccountBalances();
        double savings = income - expense;

        body.addView(buildHeroCard(balance, income, expense, accounts.size()));

        LinearLayout overview = card();
        overview.addView(text("Быстрый обзор", 18, TEXT, true));
        overview.addView(metric("Доходы", money(income), ACCENT));
        overview.addView(metric("Расходы", money(expense), EXPENSE));
        overview.addView(metric("Итог", money(savings), savings >= 0 ? ACCENT2 : WARNING));
        if (budget > 0) {
            double left = budget - expense;
            overview.addView(metric("Остаток бюджета", money(left), left >= 0 ? ACCENT : EXPENSE));
            overview.addView(progressBar(budget == 0 ? 0 : Math.min(1d, expense / budget), expense <= budget ? ACCENT : EXPENSE), paramsTop(dp(12)));
        }
        body.addView(overview, paramsTop(dp(16)));

        Button add = primaryButton("+  Добавить операцию");
        add.setOnClickListener(v -> showAddTransactionDialog());
        body.addView(add, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56), 0, dp(16), 0, 0));

        sectionHeader(body, "Счета", accounts.isEmpty() ? "Добавьте счёт" : accounts.size() + " активн.");
        body.addView(buildAccountsScroller(accounts));

        sectionHeader(body, "Структура расходов", expense > 0 ? "Текущий месяц" : "Пока пусто");
        body.addView(buildCategoryOverview(range[0], range[1], 4));

        sectionHeaderAction(body, "Последние операции", "Все операции", this::showAllOperations);
        List<DbHelper.Tx> txs = db.getTransactions(10);
        if (txs.isEmpty()) {
            body.addView(infoNote("Пока нет операций. Добавьте первую покупку или доход."));
        } else {
            for (DbHelper.Tx tx : txs) body.addView(txRow(tx, false));
        }
    }

    private void showAllOperations() {
        viewingAllOperations = true;
        currentTab = 0;
        for (int i = 0; i < nav.getChildCount(); i++) {
            ((Button) nav.getChildAt(i)).setTextColor(i == 0 ? ACCENT : MUTED);
        }
        content.removeAllViews();
        ScrollView scroll = page(null, null);
        LinearLayout body = bodyOf(scroll);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = secondaryButton("← Назад");
        back.setOnClickListener(v -> showTab(0));
        top.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(46)));
        TextView title = text("Все операции", isCompactPhone() ? 19 : 21, TEXT, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMargins(dp(12), 0, 0, 0);
        top.addView(title, titleParams);
        body.addView(top);

        List<DbHelper.Tx> all = db.getTransactions(0);
        LinearLayout summary = card();
        summary.addView(text("Всего операций", 13, MUTED, true));
        TextView count = text(String.valueOf(all.size()), 26, TEXT, true);
        count.setPadding(0, dp(4), 0, 0);
        summary.addView(count);
        body.addView(summary, paramsTop(dp(14)));

        if (all.isEmpty()) {
            body.addView(infoNote("Операций пока нет."));
        } else {
            for (DbHelper.Tx tx : all) body.addView(txRow(tx, true));
        }
    }

    private void refreshOperationsView() {
        if (viewingAllOperations) showAllOperations();
        else showTab(0);
    }

    private void showReports() {
        ScrollView scroll = page(null, null);
        LinearLayout body = bodyOf(scroll);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(filterChip("Месяц", reportMode == 0, () -> { reportMode = 0; showTab(1); }));
        chips.addView(filterChip("Год", reportMode == 1, () -> { reportMode = 1; showTab(1); }), chipMargin());
        chips.addView(filterChip("Период", reportMode == 2, () -> { reportMode = 2; showTab(1); }), chipMargin());
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
        ScrollView scroll = page(null, null);
        LinearLayout body = bodyOf(scroll);
        LinearLayout appearanceCard = card();
        appearanceCard.addView(text("Оформление", 17, TEXT, true));
        TextView appearanceInfo = text("Выберите тему и основной цвет интерфейса. Настройки сохраняются в резервной копии.", 14, MUTED, false);
        appearanceInfo.setPadding(0, dp(8), 0, dp(10));
        appearanceCard.addView(appearanceInfo);

        String[] themes = {"Тёмная", "Светлая", "Системная"};
        Spinner themeSpinner = spinner(themes);
        String storedTheme = prefs.getString("theme_mode", "dark");
        setSpinnerSelection(themeSpinner, "system".equals(storedTheme) ? "Системная" : "light".equals(storedTheme) ? "Светлая" : "Тёмная");
        appearanceCard.addView(label("Тема"));
        appearanceCard.addView(themeSpinner, inputParams());

        String[] accents = {"Мятный", "Синий", "Фиолетовый", "Коралловый"};
        Spinner accentSpinner = spinner(accents);
        String storedAccent = prefs.getString("accent_mode", "mint");
        setSpinnerSelection(accentSpinner, "blue".equals(storedAccent) ? "Синий" : "purple".equals(storedAccent) ? "Фиолетовый" : "coral".equals(storedAccent) ? "Коралловый" : "Мятный");
        appearanceCard.addView(label("Основной цвет интерфейса"));
        appearanceCard.addView(accentSpinner, inputParams());

        Button saveAppearance = secondaryButton("Сохранить оформление");
        saveAppearance.setOnClickListener(v -> {
            String themeMode = String.valueOf(themeSpinner.getSelectedItem());
            String accentMode = String.valueOf(accentSpinner.getSelectedItem());
            prefs.edit()
                    .putString("theme_mode", "Светлая".equals(themeMode) ? "light" : "Системная".equals(themeMode) ? "system" : "dark")
                    .putString("accent_mode", "Синий".equals(accentMode) ? "blue" : "Фиолетовый".equals(accentMode) ? "purple" : "Коралловый".equals(accentMode) ? "coral" : "mint")
                    .apply();
            recreate();
        });
        appearanceCard.addView(saveAppearance, buttonParams());
        body.addView(appearanceCard);

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
                showTab(2);
            } catch (Exception e) {
                toast("Введите сумму цифрами");
            }
        });
        budgetCard.addView(saveBudget, buttonParams());
        body.addView(budgetCard, paramsTop(dp(16)));

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
            showTab(2);
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
        catButtons.setOrientation(LinearLayout.VERTICAL);
        Button expenseCats = secondaryButton("Категории расходов · " + db.getCategoryCount("expense"));
        expenseCats.setOnClickListener(v -> showCategoryManagerDialog("expense"));
        Button incomeCats = secondaryButton("Категории доходов · " + db.getCategoryCount("income"));
        incomeCats.setOnClickListener(v -> showCategoryManagerDialog("income"));
        catButtons.addView(expenseCats, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        LinearLayout.LayoutParams incomeCatParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        incomeCatParams.setMargins(0, dp(8), 0, 0);
        catButtons.addView(incomeCats, incomeCatParams);
        categoriesCard.addView(catButtons);
        body.addView(categoriesCard, paramsTop(dp(16)));

        LinearLayout exportCard = card();
        exportCard.addView(text("Экспорт данных", 17, TEXT, true));
        TextView exportInfo = text("Excel — для просмотра операций. Резервная копия JSON сохраняет операции, счета, все свои категории и оформление приложения.", 14, MUTED, false);
        exportInfo.setPadding(0, dp(8), 0, dp(8));
        exportCard.addView(exportInfo);
        Button excel = secondaryButton("Экспортировать все операции в Excel (.xlsx)");
        excel.setOnClickListener(v -> createExcelDocument());
        exportCard.addView(excel, buttonParams());
        Button backup = secondaryButton("Сохранить лог операций (JSON)");
        backup.setOnClickListener(v -> createBackupDocument());
        exportCard.addView(backup, buttonParams());
        Button restore = secondaryButton("Загрузить лог и восстановить данные");
        restore.setOnClickListener(v -> openBackupDocument());
        exportCard.addView(restore, buttonParams());
        TextView updateInfo = text("Чтобы обновить приложение без потери данных, устанавливайте новую версию поверх старой и не удаляйте текущую.", 13, MUTED, false);
        updateInfo.setPadding(0, dp(10), 0, 0);
        exportCard.addView(updateInfo);
        body.addView(exportCard, paramsTop(dp(16)));
    }

    private LinearLayout buildHeroCard(double balance, double income, double expense, int accountCount) {
        LinearLayout hero = card();
        hero.setBackground(createGradientCard());

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text("Общий баланс", 14, isLightTheme ? Color.argb(210, 24, 31, 44) : Color.argb(215, 255, 255, 255), true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(pill(accountCount + " сч.", WHITE_12, TEXT));
        hero.addView(top);

        TextView amount = text(money(balance), isCompactPhone() ? 29 : 32, TEXT, true);
        amount.setPadding(0, dp(10), 0, dp(8));
        amount.setSingleLine(true);
        amount.setAutoSizeTextTypeUniformWithConfiguration(20, isCompactPhone() ? 30 : 34, 1, TypedValue.COMPLEX_UNIT_SP);
        hero.addView(amount);
        hero.addView(text("За текущий месяц: + " + money(income) + "   ·   − " + money(expense), 14, isLightTheme ? Color.argb(180, 24, 31, 44) : Color.argb(210, 255, 255, 255), false));

        TextView tip = text("Обновляйте приложение поверх старой версии. Для переноса данных используйте резервную копию.", 12, isLightTheme ? Color.argb(170, 24, 31, 44) : Color.argb(190, 230, 236, 244), false);
        tip.setPadding(0, dp(14), 0, 0);
        hero.addView(tip);
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
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(accountCardWidthPx(), ViewGroup.LayoutParams.WRAP_CONTENT);
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
        row.setPadding(dp(isCompactPhone() ? 10 : 14), dp(12), dp(isCompactPhone() ? 10 : 14), dp(12));
        row.setBackground(createRoundedDrawable(SURFACE, dp(18), WHITE_08, 1));

        TextView icon = new TextView(this);
        icon.setText("expense".equals(tx.type) ? "↓" : "↑");
        icon.setTextSize(isCompactPhone() ? 20 : 22);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor("expense".equals(tx.type) ? EXPENSE : ACCENT);
        icon.setBackground(createRoundedDrawable("expense".equals(tx.type) ? withAlpha(EXPENSE, 28) : withAlpha(ACCENT, 28), dp(24), 0, 0));
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(text(tx.category, isCompactPhone() ? 15 : 17, TEXT, true));
        TextView sub = text(formatDate(tx.createdAt) + "  •  " + tx.account + ((tx.note == null || tx.note.trim().isEmpty()) ? "" : "  •  " + tx.note), 12, MUTED, false);
        sub.setPadding(0, dp(4), 0, 0);
        info.addView(sub);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.setMargins(dp(12), 0, dp(12), 0);
        row.addView(info, infoParams);

        TextView txAmount = text(("expense".equals(tx.type) ? "−" : "+") + money(tx.amount), isCompactPhone() ? 15 : 18, "expense".equals(tx.type) ? EXPENSE : ACCENT, true);
        txAmount.setSingleLine(true);
        txAmount.setAutoSizeTextTypeUniformWithConfiguration(12, isCompactPhone() ? 16 : 18, 1, TypedValue.COMPLEX_UNIT_SP);
        row.addView(txAmount);
        row.setOnClickListener(v -> showEditTransactionDialog(tx));

        if (allowDelete) {
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Удалить операцию?")
                    .setMessage("Операция будет удалена без возможности восстановления.")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Удалить", (d, which) -> {
                        db.deleteTransaction(tx.id);
                        refreshOperationsView();
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
        EditText amount = input("Например, 4658,77");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Spinner category = spinner(categoriesFor("expense", null));
        List<String> accounts = db.getAccounts();
        if (accounts.isEmpty()) accounts.add("Основной счёт");
        Spinner account = spinner(accounts.toArray(new String[0]));
        EditText note = input("Комментарий (необязательно)");

        box.addView(label("Тип операции")); box.addView(type, inputParams());
        box.addView(label("Сумма"));
        LinearLayout amountRow = new LinearLayout(this);
        amountRow.setOrientation(LinearLayout.HORIZONTAL);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        amountRow.addView(amount, new LinearLayout.LayoutParams(0, dp(52), 1f));
        Button commaButton = secondaryButton(",");
        LinearLayout.LayoutParams commaParams = new LinearLayout.LayoutParams(dp(56), dp(52));
        commaParams.setMargins(dp(8), 0, 0, 0);
        amountRow.addView(commaButton, commaParams);
        commaButton.setOnClickListener(v -> insertComma(amount));
        box.addView(amountRow, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52), 0, 0, 0, dp(8)));
        TextView amountHint = text("Можно вводить и 4658,77, и 4658.77", 11, MUTED, false);
        amountHint.setPadding(0, 0, 0, dp(6));
        box.addView(amountHint);
        box.addView(label("Категория")); box.addView(category, inputParams());
        Button quickCategory = secondaryButton("+ Добавить свою категорию");
        quickCategory.setOnClickListener(v -> showQuickAddCategoryDialog(type.getSelectedItemPosition() == 0 ? "expense" : "income", category));
        box.addView(quickCategory, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0, 0, 0, dp(8)));
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
        EditText amount = input("Например, 4658,77");
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(trimDouble(tx.amount).replace('.', ','));
        Spinner category = spinner(categoriesFor(tx.type, tx.category));
        List<String> accounts = db.getAccounts();
        if (!accounts.contains(tx.account)) accounts.add(tx.account);
        Spinner account = spinner(accounts.toArray(new String[0]));
        setSpinnerSelection(account, tx.account);
        EditText note = input("Комментарий (необязательно)");
        if (tx.note != null) note.setText(tx.note);

        box.addView(label("Тип операции")); box.addView(type, inputParams());
        box.addView(label("Сумма"));
        LinearLayout amountRow = new LinearLayout(this);
        amountRow.setOrientation(LinearLayout.HORIZONTAL);
        amountRow.setGravity(Gravity.CENTER_VERTICAL);
        amountRow.addView(amount, new LinearLayout.LayoutParams(0, dp(52), 1f));
        Button commaButton = secondaryButton(",");
        LinearLayout.LayoutParams commaParams = new LinearLayout.LayoutParams(dp(56), dp(52));
        commaParams.setMargins(dp(8), 0, 0, 0);
        amountRow.addView(commaButton, commaParams);
        commaButton.setOnClickListener(v -> insertComma(amount));
        box.addView(amountRow, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52), 0, 0, 0, dp(8)));
        TextView amountHint = text("Можно вводить и 4658,77, и 4658.77", 11, MUTED, false);
        amountHint.setPadding(0, 0, 0, dp(6));
        box.addView(amountHint);
        box.addView(label("Категория")); box.addView(category, inputParams());
        Button quickCategory = secondaryButton("+ Добавить свою категорию");
        quickCategory.setOnClickListener(v -> showQuickAddCategoryDialog(type.getSelectedItemPosition() == 0 ? "expense" : "income", category));
        box.addView(quickCategory, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46), 0, 0, 0, dp(8)));
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
            .setNeutralButton("Удалить", null)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create();

        dialog.setOnShowListener(x -> {
            Button deleteButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            deleteButton.setTextColor(EXPENSE);
            deleteButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Удалить операцию?")
                .setMessage("Операция «" + tx.category + "» на сумму " + money(tx.amount) + " будет удалена без возможности восстановления.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (confirmDialog, which) -> {
                    db.deleteTransaction(tx.id);
                    dialog.dismiss();
                    toast("Операция удалена");
                    refreshOperationsView();
                })
                .show());

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    double a = Double.parseDouble(amount.getText().toString().trim().replace(',', '.'));
                    if (a <= 0) throw new RuntimeException();
                    String txType = type.getSelectedItemPosition() == 0 ? "expense" : "income";
                    boolean ok = db.updateTransaction(tx.id, txType, a, String.valueOf(category.getSelectedItem()), String.valueOf(account.getSelectedItem()), note.getText().toString().trim(), tx.createdAt);
                    if (!ok) throw new RuntimeException();
                    dialog.dismiss();
                    toast("Операция обновлена");
                    refreshOperationsView();
                } catch (Exception e) {
                    toast("Не удалось сохранить изменения");
                }
            });
        });
        dialog.show();
    }

    private void insertComma(EditText amount) {
        String current = amount.getText().toString();
        if (current.contains(",") || current.contains(".")) return;
        int cursor = amount.getSelectionStart();
        if (cursor < 0) cursor = current.length();
        String next;
        if (current.isEmpty()) {
            next = "0,";
            cursor = 2;
        } else {
            next = current.substring(0, cursor) + "," + current.substring(cursor);
            cursor += 1;
        }
        amount.setText(next);
        amount.setSelection(Math.min(cursor, next.length()));
    }

    private void showQuickAddCategoryDialog(String type, Spinner categorySpinner) {
        LinearLayout box = dialogBox();
        EditText name = input("Например, Подписки");
        box.addView(label("Название новой категории"));
        box.addView(name, inputParams());
        TextView hint = text("Категория сразу появится в этой операции и сохранится для следующих.", 12, MUTED, false);
        hint.setPadding(0, 0, 0, dp(4));
        box.addView(hint);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("expense".equals(type) ? "Новая категория расхода" : "Новая категория дохода")
            .setView(box)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Добавить", null)
            .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = name.getText().toString().trim();
            if (newName.isEmpty()) {
                toast("Введите название категории");
                return;
            }
            for (String existing : db.getCategories(type)) {
                if (existing.equalsIgnoreCase(newName)) {
                    categorySpinner.setAdapter(spinnerAdapter(categoriesFor(type, null)));
                    setSpinnerSelection(categorySpinner, existing);
                    dialog.dismiss();
                    toast("Такая категория уже есть — выбрал её");
                    return;
                }
            }
            if (!db.addCategory(type, newName)) {
                toast("Не удалось добавить категорию");
                return;
            }
            categorySpinner.setAdapter(spinnerAdapter(categoriesFor(type, null)));
            setSpinnerSelection(categorySpinner, newName);
            dialog.dismiss();
            toast("Категория добавлена");
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
            showTab(2);
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
                showTab(2);
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
        b.setBackground(createRoundedDrawable(withAlpha(ACCENT2, 28), dp(14), 0, 0));
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
        b.setBackground(createRoundedDrawable(withAlpha(EXPENSE, 28), dp(14), 0, 0));
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
            showTab(2);
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
            showTab(1);
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
        intent.putExtra(Intent.EXTRA_TITLE, "finup-backup.json");
        startActivityForResult(intent, REQ_CREATE_BACKUP);
    }

    private void openBackupDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQ_OPEN_BACKUP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQ_CREATE_EXCEL) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                writeExcelWorkbook(os);
                toast("Excel-файл сохранён");
            } catch (Exception e) {
                toast("Не удалось экспортировать Excel");
            }
            return;
        }

        if (requestCode == REQ_CREATE_BACKUP) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                os.write(backupJson().getBytes(StandardCharsets.UTF_8));
                toast("Лог операций сохранён");
            } catch (Exception e) {
                toast("Не удалось сохранить лог");
            }
            return;
        }

        if (requestCode == REQ_OPEN_BACKUP) {
            pendingImportUri = uri;
            new AlertDialog.Builder(this)
                .setTitle("Восстановить данные?")
                .setMessage("Текущие операции, счета и категории будут заменены данными из файла резервной копии.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Восстановить", (d, which) -> restoreFromBackupUri(pendingImportUri))
                .show();
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
                "<dc:title>SaldoNest — операции</dc:title><dc:creator>SaldoNest</dc:creator>" +
                "</cp:coreProperties>");

            zipText(zip, "docProps/app.xml",
                "<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
                "<Properties xmlns='http://schemas.openxmlformats.org/officeDocument/2006/extended-properties' " +
                "xmlns:vt='http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes'>" +
                "<Application>SaldoNest</Application></Properties>");

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
        try {
            JSONObject root = new JSONObject();
            root.put("app", "SaldoNest");
            root.put("version", 5);
            root.put("exportedAt", System.currentTimeMillis());

            JSONObject settings = new JSONObject();
            settings.put("currency", prefs.getString("currency", "RUB ₽"));
            settings.put("budget", prefs.getFloat("budget", 0f));
            settings.put("theme_mode", prefs.getString("theme_mode", "dark"));
            settings.put("accent_mode", prefs.getString("accent_mode", "mint"));
            settings.put("report_custom_from", getCustomRange()[0]);
            settings.put("report_custom_to", getCustomRange()[1]);
            root.put("settings", settings);

            JSONArray accounts = new JSONArray();
            for (DbHelper.AccountItem a : db.getAccountItems()) {
                JSONObject item = new JSONObject();
                item.put("name", a.name);
                item.put("openingBalance", a.openingBalance);
                accounts.put(item);
            }
            root.put("accounts", accounts);

            JSONObject categories = new JSONObject();
            categories.put("expense", new JSONArray(db.getCategories("expense")));
            categories.put("income", new JSONArray(db.getCategories("income")));
            root.put("categories", categories);

            JSONArray txs = new JSONArray();
            List<DbHelper.Tx> list = db.getTransactions(0);
            for (DbHelper.Tx t : list) {
                JSONObject item = new JSONObject();
                item.put("type", t.type);
                item.put("amount", t.amount);
                item.put("category", t.category);
                item.put("account", t.account);
                item.put("note", t.note == null ? "" : t.note);
                item.put("createdAt", t.createdAt);
                txs.put(item);
            }
            root.put("transactions", txs);
            return root.toString();
        } catch (Exception e) {
            return "{\"app\":\"SaldoNest\",\"version\":5,\"transactions\":[]}";
        }
    }

    private void restoreFromBackupUri(Uri uri) {
        if (uri == null) return;
        try {
            String json = readTextFromUri(uri);
            JSONObject root = new JSONObject(json);

            List<DbHelper.AccountItem> accounts = new ArrayList<>();
            JSONArray accountsJson = root.optJSONArray("accounts");
            if (accountsJson != null) {
                for (int i = 0; i < accountsJson.length(); i++) {
                    JSONObject item = accountsJson.optJSONObject(i);
                    if (item == null) continue;
                    String name = item.optString("name", "").trim();
                    if (name.isEmpty()) continue;
                    accounts.add(new DbHelper.AccountItem(name, item.optDouble("openingBalance", 0)));
                }
            }

            List<String> expenseCategories = new ArrayList<>();
            List<String> incomeCategories = new ArrayList<>();
            JSONObject categories = root.optJSONObject("categories");
            if (categories != null) {
                expenseCategories = jsonArrayToStrings(categories.optJSONArray("expense"));
                incomeCategories = jsonArrayToStrings(categories.optJSONArray("income"));
            }

            List<DbHelper.Tx> txs = new ArrayList<>();
            JSONArray txJson = root.optJSONArray("transactions");
            if (txJson == null) txJson = root.optJSONArray("tx");
            if (txJson != null) {
                for (int i = 0; i < txJson.length(); i++) {
                    JSONObject item = txJson.optJSONObject(i);
                    if (item == null) continue;
                    String type = item.optString("type", "expense");
                    double amount = item.optDouble("amount", 0);
                    String category = item.optString("category", type.equals("income") ? "Другое" : "Другое");
                    String account = item.optString("account", "Основной счёт");
                    String note = item.optString("note", "");
                    long createdAt = item.optLong("createdAt", System.currentTimeMillis());
                    if (amount <= 0) continue;
                    txs.add(new DbHelper.Tx(0, type, amount, category, account, note, createdAt));
                    if (type.equals("income")) { if (!incomeCategories.contains(category)) incomeCategories.add(category); }
                    else { if (!expenseCategories.contains(category)) expenseCategories.add(category); }
                }
            }

            db.replaceAllData(accounts, txs, expenseCategories, incomeCategories);

            JSONObject settings = root.optJSONObject("settings");
            if (settings != null) {
                SharedPreferences.Editor ed = prefs.edit();
                ed.putString("currency", settings.optString("currency", prefs.getString("currency", "RUB ₽")));
                ed.putFloat("budget", (float) settings.optDouble("budget", prefs.getFloat("budget", 0f)));
                ed.putString("theme_mode", settings.optString("theme_mode", prefs.getString("theme_mode", "dark")));
                ed.putString("accent_mode", settings.optString("accent_mode", prefs.getString("accent_mode", "mint")));
                ed.putLong("report_custom_from", settings.optLong("report_custom_from", monthRange()[0]));
                ed.putLong("report_custom_to", settings.optLong("report_custom_to", monthRange()[1]));
                ed.apply();
            }

            toast("Данные восстановлены");
            recreate();
        } catch (Exception e) {
            toast("Не удалось восстановить данные");
        }
    }

    private List<String> jsonArrayToStrings(JSONArray array) {
        List<String> out = new ArrayList<>();
        if (array == null) return out;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty() && !out.contains(value)) out.add(value);
        }
        return out;
    }

    private String readTextFromUri(Uri uri) throws Exception {
        ContentResolver resolver = getContentResolver();
        try (InputStream is = resolver.openInputStream(uri); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
            return os.toString("UTF-8");
        }
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
        b.setTextColor(selected ? ON_ACCENT : TEXT);
        b.setBackground(createRoundedDrawable(selected ? ACCENT : SURFACE, dp(100), 0, 0));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private LinearLayout.LayoutParams chipMargin() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        p.setMargins(dp(8), 0, 0, 0);
        return p;
    }

    private void sectionHeaderAction(LinearLayout body, String title, String actionText, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(title, isCompactPhone() ? 17 : 18, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView actionView = text(actionText, 13, ACCENT2, true);
        actionView.setPadding(dp(10), dp(6), dp(2), dp(6));
        actionView.setOnClickListener(v -> action.run());
        row.addView(actionView);
        body.addView(row, sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, dp(22), 0, dp(10)));
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
        b.setTextColor(ON_ACCENT);
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
        return sizedParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56), 0, dp(10), 0, 0);
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

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private GradientDrawable createRoundedDrawable(int color, int radius, int strokeColor, int strokeWidthDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (strokeColor != 0 && strokeWidthDp > 0) g.setStroke(dp(strokeWidthDp), strokeColor);
        return g;
    }

    private GradientDrawable createGradientCard() {
        int start = isLightTheme ? Color.rgb(233, 242, 255) : Color.rgb(28, 47, 82);
        int end = isLightTheme ? Color.rgb(248, 251, 255) : Color.rgb(18, 29, 49);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        g.setCornerRadius(dp(24));
        g.setStroke(dp(1), withAlpha(isLightTheme ? ACCENT2 : Color.WHITE, isLightTheme ? 26 : 20));
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

    private int screenWidthDp() {
        int width = getResources().getConfiguration().screenWidthDp;
        if (width > 0) return width;
        return (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
    }

    private boolean isCompactPhone() {
        return screenWidthDp() < 360;
    }

    private int responsivePagePaddingDp() {
        int width = screenWidthDp();
        if (width < 340) return 10;
        if (width < 380) return 14;
        if (width < 430) return 18;
        return 22;
    }

    private int accountCardWidthPx() {
        int available = getResources().getDisplayMetrics().widthPixels - dp(responsivePagePaddingDp() * 2 + 10);
        int desired = dp(screenWidthDp() >= 600 ? 300 : screenWidthDp() >= 430 ? 270 : 240);
        int minimum = dp(180);
        return Math.max(minimum, Math.min(desired, available));
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
