import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class ExpenseTrackerApp extends Application {

    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private final ObservableList<String> categories = FXCollections.observableArrayList(
            "Food", "Education", "Transport", "Shopping", "Bills", "General");

    private final Map<String, String> categoryMemory = new HashMap<>();

    private Label balanceLabel;
    private TableView<Transaction> table;
    private TextField amountField, noteField, newCategoryField, budgetField;
    private DatePicker datePicker;
    private ComboBox<String> categoryCombo;
    private ToggleGroup typeGroup;
    private ToggleGroup methodGroup;

    private double balance = 0.0;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(createBudgetBox());
        table = createTable();
        root.setCenter(table);
        root.setRight(createControls());
        root.setBottom(createBottomBar());

        BorderPane.setMargin(table, new Insets(10));

        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.setTitle("Expense Tracker");
        stage.show();
    }

    // ---------------- UI ----------------

    private HBox createBudgetBox() {
        budgetField = new TextField();
        Button setBudget = new Button("Set Budget");
        setBudget.setOnAction(e -> {
            balance = Double.parseDouble(budgetField.getText());
            updateBalance();
        });

        balanceLabel = new Label("Balance: 0.00");
        balanceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox h = new HBox(10, new Label("Initial Budget:"), budgetField, setBudget, new Region(), balanceLabel);
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
        categoryCombo.getSelectionModel().selectFirst();

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

        VBox v = new VBox(8,
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

        HBox h = new HBox(10, importCsv);
        h.setPadding(new Insets(10));
        return h;
    }

    // ---------------- LOGIC ----------------

    private void addManualTransaction() {
        double amt = Double.parseDouble(amountField.getText());
        String type = ((RadioButton) typeGroup.getSelectedToggle()).getText();
        String method = ((ToggleButton) methodGroup.getSelectedToggle()).getText();
        String cat = categoryCombo.getEditor().getText();

        Transaction t = new Transaction(LocalDate.now(), cat, type, method, amt, noteField.getText());
        apply(t);
    }

    private void importCSV() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
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

                Transaction t = new Transaction(date, cat, type, "UPI", amt, name);
                apply(t);
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

    private <T> TableColumn<Transaction, T> column(String name, String prop, int w) {
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

    // ---------------- MODEL ----------------

    public static class Transaction {
        LocalDate date;
        String category, type, method, note;
        double amount;

        Transaction(LocalDate d, String c, String t, String m, double a, String n) {
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
