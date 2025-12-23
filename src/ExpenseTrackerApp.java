import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

public class ExpenseTrackerApp extends Application {

    // ---------------- DATA ----------------
    private final ObservableList<Transaction> transactions =
            FXCollections.observableArrayList();

    private final ObservableList<String> categories =
            FXCollections.observableArrayList(
                    "Food", "Education", "Transport",
                    "Shopping", "Bills", "General"
            );

    private final Map<String, String> categoryMemory = new HashMap<>();

    // ---------------- UI ----------------
    private Label balanceLabel;
    private TableView<Transaction> table;
    private TextField amountField, noteField, budgetField;
    private DatePicker datePicker;
    private ComboBox<String> categoryCombo;
    private ToggleGroup typeGroup;
    private ToggleGroup methodGroup;

    // ---------------- STATE ----------------
    private double balance = 0.0;
    private boolean budgetSet = false;

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();
        root.setTop(createBudgetBox());
        table = createTable();
        root.setCenter(table);
        root.setRight(createControls());
        root.setBottom(createBottomBar());

        BorderPane.setMargin(table, new Insets(10));

        stage.setScene(new Scene(root, 1000, 600));
        stage.setTitle("Expense Tracker");
        stage.show();
    }

    // ================= UI =================

    private HBox createBudgetBox() {

        budgetField = new TextField();

        Button setBudget = new Button("Set Budget");
        setBudget.setOnAction(e -> {
            try {
                balance = Double.parseDouble(budgetField.getText());
                budgetSet = true;
                updateBalance();
            } catch (Exception ex) {
                alert("Invalid Budget", "Enter a valid number.");
            }
        });

        balanceLabel = new Label("Balance: 0.00");
        balanceLabel.setStyle("-fx-font-size:14px; -fx-font-weight:bold;");

        HBox h = new HBox(
                10,
                new Label("Initial Budget:"),
                budgetField,
                setBudget,
                new Region(),
                balanceLabel
        );

        HBox.setHgrow(h.getChildren().get(3), Priority.ALWAYS);
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
                column("Note", "note", 200)
        );
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
                add
        );

        v.setPadding(new Insets(10));
        v.setPrefWidth(280);
        return v;
    }

    private HBox createBottomBar() {

        Button importCsv = new Button("Import GPay CSV");
        importCsv.setOnAction(e -> importCSV());

        Button dashboard = new Button("Dashboard");
        dashboard.setOnAction(e -> openDashboard());

        HBox h = new HBox(10, importCsv, dashboard);
        h.setPadding(new Insets(10));
        return h;
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

        String type =
                ((RadioButton) typeGroup.getSelectedToggle()).getText();

        Toggle selectedMethod = methodGroup.getSelectedToggle();
        if (selectedMethod == null) {
            alert("Payment Method", "Select Cash or UPI.");
            return;
        }

        String method = ((ToggleButton) selectedMethod).getText();

        String cat = categoryCombo.getEditor().getText();
        if (cat == null || cat.isBlank()) cat = "General";
        if (!categories.contains(cat)) categories.add(cat);

        apply(new Transaction(
                datePicker.getValue(),
                cat,
                type,
                method,
                amt,
                noteField.getText()
        ));

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
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fc.showOpenDialog(null);
        if (file == null) return;

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

                if (!categories.contains(cat)) categories.add(cat);
                apply(new Transaction(date, cat, type, "UPI", amt, name));
            }
        } catch (Exception ex) {
            alert("CSV Error", ex.getMessage());
        }
    }

    private void apply(Transaction t) {
        transactions.add(t);
        balance += t.type.equals("Credit") ? t.amount : -t.amount;
        updateBalance();
    }

    private void updateBalance() {
        balanceLabel.setText(String.format("Balance: %.2f", balance));
    }

    // ================= DASHBOARD =================

    private void openDashboard() {

        Map<String, Double> categorySum = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.type.equals("Debit")) {
                categorySum.put(
                        t.category,
                        categorySum.getOrDefault(t.category, 0.0) + t.amount
                );
            }
        }

        // -------- PIE CHART WITH PERCENTAGE --------
        PieChart pie = new PieChart();

        double total = categorySum.values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        for (Map.Entry<String, Double> e : categorySum.entrySet()) {

            PieChart.Data data =
                    new PieChart.Data(e.getKey(), e.getValue());

            pie.getData().add(data);

            double percent = (e.getValue() / total) * 100;

            Tooltip tip = new Tooltip(
                    String.format("%s : %.2f%%",
                            e.getKey(), percent)
            );

            data.nodeProperty().addListener((obs, oldN, newN) -> {
                if (newN != null) Tooltip.install(newN, tip);
            });
        }

        pie.setTitle("Expenses by Category");

        // -------- LINE CHART --------
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        LineChart<String, Number> lineChart =
                new LineChart<>(x, y);
        lineChart.setLegendVisible(false);

        ComboBox<String> filter =
                new ComboBox<>(FXCollections.observableArrayList(
                        "Weekly", "Monthly", "Yearly"
                ));
        filter.setValue("Monthly");
        filter.setOnAction(e ->
                updateLineChart(lineChart, filter.getValue())
        );

        updateLineChart(lineChart, "Monthly");

        VBox root = new VBox(10, filter, pie, lineChart);
        root.setPadding(new Insets(10));

        Stage s = new Stage();
        s.setTitle("Dashboard");
        s.setScene(new Scene(root, 750, 700));
        s.show();
    }

    private void updateLineChart(LineChart<String, Number> chart, String mode) {

        chart.getData().clear();
        Map<String, Double> map = new TreeMap<>();
        WeekFields wf = WeekFields.of(Locale.getDefault());

        for (Transaction t : transactions) {
            if (!t.type.equals("Debit")) continue;

            String key;
            if (mode.equals("Weekly"))
                key = "Week " + t.date.get(wf.weekOfWeekBasedYear());
            else if (mode.equals("Yearly"))
                key = String.valueOf(t.date.getYear());
            else
                key = t.date.getMonth() + " " + t.date.getYear();

            map.put(key, map.getOrDefault(key, 0.0) + t.amount);
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        map.forEach((k, v) ->
                s.getData().add(new XYChart.Data<>(k, v))
        );
        chart.getData().add(s);
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

        public String getDateStr() { return date.toString(); }
        public String getCategory() { return category; }
        public String getType() { return type; }
        public String getMethod() { return method; }
        public double getAmount() { return amount; }
        public String getNote() { return note; }
    }
}
