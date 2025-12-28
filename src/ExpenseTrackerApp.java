import javafx.application.Application;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
//import Expensetracker.util.DBConnection;

public class ExpenseTrackerApp extends Application {
    private static final String CSV_PATH = "C:\\Users\\shiva\\OneDrive\\Documents\\Desktop\\Expense_tracker_app\\expenses.csv";

    // ---------------- DATA ----------------
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    private final ObservableList<String> categories = FXCollections.observableArrayList(
            "Food", "Education", "Transport",
            "Shopping", "Bills", "General");

    private final Map<String, String> categoryMemory = new HashMap<>();

    // ---------------- UI ----------------
    private Label balanceLabel;
    private TableView<Transaction> table;
    private TextField amountField, noteField;
    private DatePicker datePicker;
    private ComboBox<String> categoryCombo;
    private ToggleGroup typeGroup;
    private ToggleGroup methodGroup;

    // ---------------- STATE ----------------
    private double totalbudget = 0;
    private double balance;
    private boolean budgetSet = false;

    @Override
    public void start(Stage stage) {
        // System.out.println("START() CALLED");
        // Expensetracker.util.DBConnection.getConnection();
        // System.out.println("AFTER DB CONNECTION");
        loadBudgetFromCSV();
        loadTransactionsFromCSV();
        recalculateBalanceFromTransactions();

        filteredTransactions = new FilteredList<>(transactions, p -> true);
        table = createTable();
        table.setItems(filteredTransactions);

        BorderPane root = new BorderPane();
        VBox topArea = new VBox(5);
        topArea.getChildren().addAll(createBudgetBox(), createFilterBar());
        root.setTop(topArea);

        root.setCenter(table);
        root.setRight(createControls());
        root.setBottom(createBottomBar());

        BorderPane.setMargin(table, new Insets(10));

        stage.setScene(new Scene(root, 1000, 600));
        stage.setTitle("Expense Tracker");
        stage.show();
    }

    /*
     * private void saveTransactionToDB(Transaction t) {
     * String sql =
     * "INSERT INTO transactions(date, category, type, method, amount, note) " +
     * "VALUES (?, ?, ?, ?, ?, ?)";
     * 
     * try (Connection con = Expensetracker.util.DBConnection.getConnection();
     * PreparedStatement ps = con.prepareStatement(sql)) {
     * 
     * ps.setString(1, t.date.toString());
     * ps.setString(2, t.category);
     * ps.setString(3, t.type);
     * ps.setString(4, t.method);
     * ps.setDouble(5, t.amount);
     * ps.setString(6, t.note);
     * 
     * ps.executeUpdate();
     * 
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * }
     */

    // =====================CSV save=====================
    /*
     * private void saveBudgetToCSV() {
     * 
     * try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_PATH))) {
     * 
     * // Write budget first
     * pw.println("BUDGET," + balance);
     * 
     * // Write header
     * pw.println("Date,Category,Type,Method,Amount,Note");
     * 
     * // Write all transactions
     * for (Transaction t : transactions) {
     * pw.println(
     * t.date + "," +
     * t.category + "," +
     * t.type + "," +
     * t.method + "," +
     * t.amount + "," +
     * t.note.replace(",", " "));
     * }
     * 
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * }
     */

    private void writeCSVFromMemory() {

        try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_PATH))) {

            // ✅ Write budget ONCE
            pw.println("BUDGET," + totalbudget);

            // Header
            pw.println("Date,Category,Type,Method,Amount,Note");

            // All transactions
            for (Transaction t : transactions) {
                pw.println(
                        t.date + "," +
                                t.category + "," +
                                t.type + "," +
                                t.method + "," +
                                t.amount + "," +
                                t.note.replace(",", " "));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTransactionsFromCSV() {

        File file = new File(CSV_PATH);
        if (!file.exists())
            return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            br.readLine();

            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");

                Transaction t = new Transaction(
                        LocalDate.parse(p[0]),
                        p[1],
                        p[2],
                        p[3],
                        Double.parseDouble(p[4]),
                        p[5]);

                transactions.add(t);

            }

            updateBalance();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recalculateBalanceFromTransactions() {
        balance = totalbudget;

        for (Transaction t : transactions) {
            if (t.type.equals("Credit")) {
                balance += t.amount;
            } else {
                balance -= t.amount;
            }
        }
    }

    private void loadBudgetFromCSV() {

        File file = new File(CSV_PATH);
        if (!file.exists())
            return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line = br.readLine();

            if (line != null && line.startsWith("BUDGET")) {
                totalbudget = Double.parseDouble(line.split(",")[1]);
                balance = totalbudget; // reset balance
                budgetSet = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= UI =================

    private TextField budgetField;
    private Button setBudgetBtn;
    private Button saveBudgetBtn;

    private HBox createBudgetBox() {

        budgetField = new TextField();
        budgetField.setPrefWidth(120);
        budgetField.setDisable(true); // 🔒 locked by default

        // Show last loaded budget
        budgetField.setText(String.format("%.2f", totalbudget));

        balanceLabel = new Label("Balance: ₹ " + String.format("%.2f", balance));
        balanceLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");

        setBudgetBtn = new Button("Set Budget");
        saveBudgetBtn = new Button("Save");
        saveBudgetBtn.setDisable(true);

        // 🔓 Enable editing
        setBudgetBtn.setOnAction(e -> {
            budgetField.setDisable(false);
            budgetField.requestFocus();
            saveBudgetBtn.setDisable(false);
        });

        // 💾 Save budget
        saveBudgetBtn.setOnAction(e -> {
            try {
                totalbudget = Double.parseDouble(budgetField.getText());
                balance = totalbudget;
                budgetSet = true;

                budgetSet = true;

                budgetField.setDisable(true);
                saveBudgetBtn.setDisable(true);

                updateBalance();
                writeCSVFromMemory(); // ✅ persist budget

            } catch (Exception ex) {
                alert("Invalid Budget", "Enter a valid number.");
            }
        });

        HBox h = new HBox(
                10,
                new Label("Budget:"),
                budgetField,
                setBudgetBtn,
                saveBudgetBtn,
                new Region(),
                balanceLabel);

        HBox.setHgrow(h.getChildren().get(5), Priority.ALWAYS);
        h.setPadding(new Insets(10));

        return h;
    }

    private TableView<Transaction> createTable() {

        TableView<Transaction> tv = new TableView<>(transactions);

        tv.getColumns().addAll(
                column("Date", "dateStr", 100),
                column("Category", "category", 120),
                column("Type", "type", 80),
                column("Method", "method", 80),
                column("Amount", "amount", 100),
                column("Note", "note", 200));
        return tv;
    }

    private VBox createControls() {

        datePicker = new DatePicker(LocalDate.now());

        categoryCombo = new ComboBox<>(categories);
        categoryCombo.setEditable(true);
        categoryCombo.getEditor().setPromptText("Type category");
        categoryCombo.setValue("General");

        amountField = new TextField();
        noteField = new TextField();

        RadioButton debit = new RadioButton("Debit");
        RadioButton credit = new RadioButton("Credit");
        typeGroup = new ToggleGroup();
        debit.setToggleGroup(typeGroup);
        credit.setToggleGroup(typeGroup);
        debit.setSelected(true);

        ToggleButton cash = new ToggleButton("Cash");
        ToggleButton upi = new ToggleButton("UPI");
        methodGroup = new ToggleGroup();
        cash.setToggleGroup(methodGroup);
        upi.setToggleGroup(methodGroup);
        cash.setSelected(true);

        Button add = new Button("Add Transaction");
        add.setOnAction(e -> addManualTransaction());

        VBox v = new VBox(
                8,
                new Label("Date"), datePicker,
                new Label("Category"), categoryCombo,
                new Label("Amount"), amountField,
                new HBox(10, debit, credit),
                new HBox(10, cash, upi),
                new Label("Note"), noteField,
                add);

        v.setPadding(new Insets(10));
        v.setPrefWidth(280);
        return v;
    }

    private HBox createBottomBar() {

        Button importCsv = new Button("Import CSV");
        importCsv.setOnAction(e -> importCSV());

        Button dashboard = new Button("Dashboard");
        dashboard.setOnAction(e -> openDashboard());

        Button delete = new Button("Delete Selected");
        delete.setOnAction(e -> deleteSelectedTransaction());

        HBox h = new HBox(10, importCsv, dashboard, delete);
        h.setPadding(new Insets(10));
        return h;
    }

    private void deleteSelectedTransaction() {

        Transaction selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            alert("No Selection", "Please select a transaction to delete.");
            return;
        }

        // Update balance
        balance -= selected.type.equals("Credit")
                ? selected.amount
                : -selected.amount;

        transactions.remove(selected);
        updateBalance();

        // Update CSV permanently
        writeCSVFromMemory();
    }

    /*
     * private void rewriteCSVFromMemory() {
     * saveTransactionToCSV(t);
     * 
     * try (PrintWriter pw = new PrintWriter(new FileWriter(CSV_PATH))) {
     * 
     * // Write header again
     * pw.println("Date,Category,Type,Method,Amount,Note");
     * 
     * for (Transaction t : transactions) {
     * pw.println(
     * t.date + "," +
     * t.category + "," +
     * t.type + "," +
     * t.method + "," +
     * t.amount + "," +
     * t.note.replace(",", " "));
     * }
     * 
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * }
     */

    // =========filter=====================
    private FilteredList<Transaction> filteredTransactions;
    private ComboBox<String> categoryFilter;
    private TextField noteFilter;
    private DatePicker fromDatePicker, toDatePicker;
    private TextField minAmountField, maxAmountField;

    private HBox createFilterBar() {

        // ---- Category Filter ----
        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().add("All");
        categoryFilter.getItems().addAll(categories);
        categoryFilter.setValue("All");
        categoryFilter.setPrefWidth(120);

        HBox categoryBox = new HBox(3, new Label("Category:"), categoryFilter);
        categoryBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Note Filter ----
        noteFilter = new TextField();
        noteFilter.setPromptText("Search note");
        noteFilter.setPrefWidth(140);

        // ---- Date Filters ----
        fromDatePicker = new DatePicker();
        fromDatePicker.setPromptText("From");

        toDatePicker = new DatePicker();
        toDatePicker.setPromptText("To");

        HBox dateBox = new HBox(3,
                new Label("Date:"), fromDatePicker, toDatePicker);
        dateBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Amount Filters ----
        minAmountField = new TextField();
        minAmountField.setPromptText("Min ₹");
        minAmountField.setPrefWidth(80);

        maxAmountField = new TextField();
        maxAmountField.setPromptText("Max ₹");
        maxAmountField.setPrefWidth(80);

        HBox amountBox = new HBox(3,
                new Label("Amount:"), minAmountField, maxAmountField);
        amountBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Clear Button ----
        Button clearFilters = new Button("Clear");
        clearFilters.setOnAction(e -> clearFilters());

        // ---- Apply listeners ----
        categoryFilter.setOnAction(e -> applyFilters());
        noteFilter.textProperty().addListener((a, b, c) -> applyFilters());
        fromDatePicker.setOnAction(e -> applyFilters());
        toDatePicker.setOnAction(e -> applyFilters());
        minAmountField.textProperty().addListener((a, b, c) -> applyFilters());
        maxAmountField.textProperty().addListener((a, b, c) -> applyFilters());

        HBox filterBar = new HBox(
                8,
                categoryBox,
                noteFilter,
                dateBox,
                amountBox,
                clearFilters);

        filterBar.setPadding(new Insets(8));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        return filterBar;
    }

    private void applyFilters() {

        filteredTransactions.setPredicate(t -> {

            // Category filter
            if (!categoryFilter.getValue().equals("All") &&
                    !t.category.equals(categoryFilter.getValue()))
                return false;

            // Note filter
            if (!noteFilter.getText().isBlank() &&
                    !t.note.toLowerCase().contains(noteFilter.getText().toLowerCase()))
                return false;

            // Date range filter
            if (fromDatePicker.getValue() != null &&
                    t.date.isBefore(fromDatePicker.getValue()))
                return false;

            if (toDatePicker.getValue() != null &&
                    t.date.isAfter(toDatePicker.getValue()))
                return false;

            // Amount range filter
            try {
                if (!minAmountField.getText().isBlank() &&
                        t.amount < Double.parseDouble(minAmountField.getText()))
                    return false;

                if (!maxAmountField.getText().isBlank() &&
                        t.amount > Double.parseDouble(maxAmountField.getText()))
                    return false;
            } catch (NumberFormatException e) {
                return true; // ignore invalid input
            }

            return true;
        });
    }

    private void clearFilters() {

        categoryFilter.setValue("All");
        noteFilter.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        minAmountField.clear();
        maxAmountField.clear();

        applyFilters();
    }

    // ================= LOGIC =================

    private void addManualTransaction() {

        if (!budgetSet) {
            alert("Budget Not Set", "Please set budget first.");
            return;
        }

        double amt;
        try {
            amt = Double.parseDouble(amountField.getText());
        } catch (Exception e) {
            alert("Invalid Amount", "Enter valid amount.");
            return;
        }

        String type = ((RadioButton) typeGroup.getSelectedToggle()).getText();

        Toggle selectedMethod = methodGroup.getSelectedToggle();
        if (selectedMethod == null) {
            alert("Payment Method", "Select Cash or UPI.");
            return;
        }

        String method = ((ToggleButton) selectedMethod).getText();

        String cat = categoryCombo.getEditor().getText();
        if (cat == null || cat.isBlank())
            cat = "General";
        if (!categories.contains(cat))
            categories.add(cat);

        apply(new Transaction(
                datePicker.getValue(),
                cat,
                type,
                method,
                amt,
                noteField.getText()));

        amountField.clear();
        noteField.clear();
    }

    private void importCSV() {

        if (!budgetSet) {
            alert("Budget Not Set", "Set budget first.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fc.showOpenDialog(null);
        if (file == null)
            return;

        try {
            List<String> lines = Files.readAllLines(file.toPath());

            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(",");
                LocalDate date = LocalDate.parse(p[0]);
                String name = p[1];
                double amt = Double.parseDouble(p[2]);
                String type = p[3];

                String cat = categoryMemory.get(name);
                if (cat == null) {
                    TextInputDialog d = new TextInputDialog("General");
                    d.setHeaderText("Category for: " + name);
                    cat = d.showAndWait().orElse("General");
                    categoryMemory.put(name, cat);
                }

                if (!categories.contains(cat))
                    categories.add(cat);
                apply(new Transaction(date, cat, type, "UPI", amt, name));
            }
        } catch (Exception ex) {
            alert("CSV Error", ex.getMessage());
        }
    }

    private void apply(Transaction t) {

        transactions.add(t);

        balance += t.type.equals("Credit")
                ? t.amount
                : -t.amount;

        writeCSVFromMemory(); // ✅ ONE SOURCE OF TRUTH
        updateBalance();
    }

    /*
     * private void loadTransactionsFromDB() {
     * 
     * transactions.clear();
     * 
     * String sql = "SELECT * FROM transactions";
     * 
     * try (Connection con = Expensetracker.util.DBConnection.getConnection();
     * PreparedStatement ps = con.prepareStatement(sql);
     * ResultSet rs = ps.executeQuery()) {
     * 
     * while (rs.next()) {
     * Transaction t = new Transaction(
     * LocalDate.parse(rs.getString("date")),
     * rs.getString("category"),
     * rs.getString("type"),
     * rs.getString("method"),
     * rs.getDouble("amount"),
     * rs.getString("note"));
     * 
     * transactions.add(t);
     * balance += t.type.equals("Credit")
     * ? t.amount
     * : -t.amount;
     * }
     * 
     * updateBalance();
     * 
     * } catch (Exception e) {
     * e.printStackTrace();
     * }
     * }
     */

    private void updateBalance() {
        balanceLabel.setText(String.format("Balance: %.2f", balance));
    }

    // ================= DASHBOARD =================
    private final Map<String, String> categoryColorMap = new HashMap<>();
    private int colorIndex = 0;

    private final String[] COLORS = {
            "#e67e22", "#f1c40f", "#2ecc71",
            "#3498db", "#9b59b6", "#e84393",
            "#16a085", "#d35400"
    };

    private String getColorForCategory(String category) {
        return categoryColorMap.computeIfAbsent(
                category,
                k -> COLORS[colorIndex++ % COLORS.length]);
    }

    private void openDashboard() {

        Map<String, Double> categorySum = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.type.equals("Debit")) {
                categorySum.put(
                        t.category,
                        categorySum.getOrDefault(t.category, 0.0) + t.amount);
            }
        }

        // -------- PIE CHART WITH PERCENTAGE --------
        // -------- PIE CHART --------
        PieChart pie = new PieChart();

        double total = categorySum.values().stream().mapToDouble(Double::doubleValue).sum();

        VBox categoryAmountBox = new VBox(8);

        for (Map.Entry<String, Double> e : categorySum.entrySet()) {

            String category = e.getKey();
            double value = e.getValue();
            String color = getColorForCategory(category);

            PieChart.Data data = new PieChart.Data(category, value);
            pie.getData().add(data);

            double percent = (value / total) * 100;

            data.nodeProperty().addListener((obs, oldNode, node) -> {
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + color + ";");

                    Tooltip.install(node,
                            new Tooltip(
                                    category +
                                            "\n₹ " + String.format("%.2f", value) +
                                            String.format(" (%.1f%%)", percent)));
                }
            });

            // Side info panel
            Circle dot = new Circle(6, Color.web(color));
            Label label = new Label(category + "  ₹ " + String.format("%.2f", value));
            label.setStyle("-fx-font-size: 13px;");

            HBox row = new HBox(10, dot, label);
            row.setAlignment(Pos.CENTER_LEFT);

            categoryAmountBox.getChildren().add(row);
        }

        pie.setTitle("Expenses by Category");

        // -------- LINE CHART --------
        CategoryAxis x = new CategoryAxis();
        x.setLabel("Date");

        NumberAxis y = new NumberAxis();
        y.setLabel("Amount (₹)");

        LineChart<String, Number> lineChart = new LineChart<>(x, y);
        lineChart.setLegendVisible(false);

        ComboBox<String> filter = new ComboBox<>(FXCollections.observableArrayList(
                "Weekly", "Monthly", "Yearly"));
        filter.setValue("Monthly");
        filter.setOnAction(e -> updateLineChart(lineChart, filter.getValue()));

        updateLineChart(lineChart, "Monthly");

        HBox pieSection = new HBox(30, pie, categoryAmountBox);
        pieSection.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(15, filter, pieSection, lineChart);
        root.setPadding(new Insets(15));

        Stage s = new Stage();
        s.setTitle("Dashboard");
        s.setScene(new Scene(root, 900, 750));
        s.show();
    }

    private void updateLineChart(LineChart<String, Number> chart, String mode) {

        chart.getData().clear();

        Map<String, Map<String, Double>> categoryMap = new HashMap<>();
        WeekFields wf = WeekFields.of(Locale.getDefault());

        // ✅ t IS VALID HERE
        for (Transaction t : transactions) {

            if (!t.type.equals("Debit"))
                continue;

            String timeKey;
            if (mode.equals("Weekly"))
                timeKey = "Week " + t.date.get(wf.weekOfWeekBasedYear());
            else if (mode.equals("Yearly"))
                timeKey = String.valueOf(t.date.getYear());
            else
                timeKey = t.date.toString(); // yyyy-MM-dd

            categoryMap
                    .computeIfAbsent(t.category, k -> new TreeMap<>())
                    .merge(timeKey, t.amount, Double::sum);
        }

        // build chart series
        for (String category : categoryMap.keySet()) {

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(category);

            for (Map.Entry<String, Double> e : categoryMap.get(category).entrySet()) {

                XYChart.Data<String, Number> data = new XYChart.Data<>(e.getKey(), e.getValue());

                series.getData().add(data);

                data.nodeProperty().addListener((obs, o, node) -> {
                    if (node != null) {
                        Tooltip.install(node,
                                new Tooltip(
                                        category +
                                                "\nDate: " + e.getKey() +
                                                "\n₹ " + e.getValue()));
                    }
                });
            }

            String color = getColorForCategory(category);
            series.nodeProperty().addListener((obs, o, n) -> {
                if (n != null)
                    n.setStyle("-fx-stroke: " + color + ";");
            });

            chart.getData().add(series);
        }
    }

    // ================= HELPERS =================

    private <T> TableColumn<Transaction, T> column(
            String name, String prop, int w) {

        TableColumn<Transaction, T> c = new TableColumn<>(name);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    private void alert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, m);
        a.setTitle(t);
        a.showAndWait();
    }

    // ================= MODEL =================

    public static class Transaction {
        LocalDate date;
        String category, type, method, note;
        double amount;

        Transaction(LocalDate d, String c,
                String t, String m,
                double a, String n) {
            date = d;
            category = c;
            type = t;
            method = m;
            amount = a;
            note = n;
        }

        public String getDateStr() {
            return date.toString();
        }

        public String getCategory() {
            return category;
        }

        public String getType() {
            return type;
        }

        public String getMethod() {
            return method;
        }

        public double getAmount() {
            return amount;
        }

        public String getNote() {
            return note;
        }
    }
}
