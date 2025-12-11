
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class ExpenseTrackerApp extends Application {
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private final ObservableList<String> categories = FXCollections.observableArrayList(
            "Food", "Education", "Transport", "Shopping", "Bills", "General");

    private final String CSV_FILE = "expenses.csv";

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
        HBox budgetBox = createBudgetBox();
        table = createTable();
        VBox rightPane = createControls();
        HBox bottomBar = createBottomBar();

        BorderPane root = new BorderPane();
        root.setTop(budgetBox);
        root.setCenter(table);
        root.setRight(rightPane);
        root.setBottom(bottomBar);
        BorderPane.setMargin(table, new Insets(10));
        BorderPane.setMargin(rightPane, new Insets(10));

        loadFromCsv();
        updateBalanceLabel();

        Scene scene = new Scene(root, 1000, 600);
        stage.setScene(scene);
        stage.setTitle("Expense Tracker - JavaFX");
        stage.show();
    }

    private HBox createBudgetBox() {
        Label budLabel = new Label("Initial Budget:");
        budgetField = new TextField();
        budgetField.setPromptText("e.g. 1000");
        Button setBudgetBtn = new Button("Set Budget");
        setBudgetBtn.setOnAction(e -> {
            try {
                double b = Double.parseDouble(budgetField.getText().trim());
                balance = b;
                updateBalanceLabel();
            } catch (NumberFormatException ex) {
                alert("Invalid budget", "Please enter a valid number for budget.");
            }
        });

        balanceLabel = new Label("Balance: 0.00");
        balanceLabel.setStyle("-fx-font-size: 14pt; -fx-font-weight: bold;");

        HBox h = new HBox(8, budLabel, budgetField, setBudgetBtn, new Region(), balanceLabel);
        HBox.setHgrow(h.getChildren().get(3), Priority.ALWAYS);
        h.setPadding(new Insets(10));
        return h;
    }

    private TableView<Transaction> createTable() {
        TableView<Transaction> tv = new TableView<>(transactions);
        tv.setPlaceholder(new Label("No transactions yet"));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dateStr"));
        dateCol.setPrefWidth(100);

        TableColumn<Transaction, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(120);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);

        TableColumn<Transaction, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("method"));
        methodCol.setPrefWidth(80);

        TableColumn<Transaction, Double> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setPrefWidth(100);

        TableColumn<Transaction, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteCol.setPrefWidth(200);

        tv.getColumns().addAll(dateCol, catCol, typeCol, methodCol, amtCol, noteCol);

        return tv;
    }

    private VBox createControls() {
        datePicker = new DatePicker(LocalDate.now());
        categoryCombo = new ComboBox<>(categories);
        categoryCombo.setEditable(true);
        categoryCombo.setPromptText("Category");
        categoryCombo.getSelectionModel().selectFirst();

        amountField = new TextField();
        amountField.setPromptText("Amount");

        noteField = new TextField();
        noteField.setPromptText("Note (optional)");

        RadioButton creditRb = new RadioButton("Credit");
        RadioButton debitRb = new RadioButton("Debit");
        typeGroup = new ToggleGroup();
        creditRb.setToggleGroup(typeGroup);
        debitRb.setToggleGroup(typeGroup);
        debitRb.setSelected(true);

        HBox typeBox = new HBox(8, creditRb, debitRb);

        ToggleButton cashBtn = new ToggleButton("Cash");
        ToggleButton upiBtn = new ToggleButton("UPI");
        methodGroup = new ToggleGroup();
        cashBtn.setToggleGroup(methodGroup);
        upiBtn.setToggleGroup(methodGroup);
        cashBtn.setSelected(true);
        HBox methodBox = new HBox(8, cashBtn, upiBtn);

        Button addBtn = new Button("Add Transaction");
        addBtn.setOnAction(e -> addTransaction());

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.setOnAction(e -> deleteSelected());

        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 8, 0));

        Label catLabel = new Label("Manage Categories:");
        newCategoryField = new TextField();
        newCategoryField.setPromptText("New category name");
        Button addCatBtn = new Button("Add Category");
        addCatBtn.setOnAction(e -> addCategory());
        Button delCatBtn = new Button("Delete Selected Category");
        delCatBtn.setOnAction(e -> deleteCategory());

        ListView<String> catList = new ListView<>(categories);
        catList.setPrefHeight(120);
        catList.setOnMouseClicked(e -> {
            String s = catList.getSelectionModel().getSelectedItem();
            if (s != null)
                categoryCombo.getSelectionModel().select(s);
        });

        VBox v = new VBox(8,
                new Label("Date:"), datePicker,
                new Label("Category:"), categoryCombo,
                new Label("Amount:"), amountField,
                new Label("Type (Credit / Debit):"), typeBox,
                new Label("Method (Cash / UPI):"), methodBox,
                new Label("Note:"), noteField,
                addBtn, deleteBtn,
                sep,
                catLabel, newCategoryField, new HBox(8, addCatBtn, delCatBtn), catList);
        v.setPadding(new Insets(10));
        v.setPrefWidth(300);
        return v;
    }

    private HBox createBottomBar() {
        Button saveBtn = new Button("Save to CSV");
        saveBtn.setOnAction(e -> saveToCsv());
        Button loadBtn = new Button("Load from CSV");
        loadBtn.setOnAction(e -> {
            loadFromCsv();
            updateBalanceLabel();
        });
        HBox h = new HBox(8, saveBtn, loadBtn);
        h.setPadding(new Insets(10));
        return h;
    }

    private void addTransaction() {
        String amtText = amountField.getText().trim();
        if (amtText.isEmpty()) {
            alert("Missing amount", "Please enter an amount.");
            return;
        }
        double amt;
        try {
            amt = Double.parseDouble(amtText);
        } catch (NumberFormatException ex) {
            alert("Invalid amount", "Enter a valid number for amount.");
            return;
        }
        String cat = categoryCombo.getEditor().getText().trim();
        if (cat.isEmpty())
            cat = "General";
        if (!categories.contains(cat))
            categories.add(cat);

        Toggle selectedType = typeGroup.getSelectedToggle();
        String type = selectedType == null ? "Debit" : ((RadioButton) selectedType).getText();

        Toggle selectedMethod = methodGroup.getSelectedToggle();
        String method = selectedMethod == null ? "Cash" : ((ToggleButton) selectedMethod).getText();

        LocalDate date = datePicker.getValue();
        String note = noteField.getText().trim();

        Transaction t = new Transaction(UUID.randomUUID().toString(), date, cat, type, method, amt, note);
        transactions.add(t);
        applyTransactionToBalance(t);
        updateBalanceLabel();

        amountField.clear();
        noteField.clear();
    }

    private void deleteSelected() {
        Transaction sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert("No selection", "Select a transaction to delete.");
            return;
        }
        reverseTransactionFromBalance(sel);
        transactions.remove(sel);
        updateBalanceLabel();
    }

    private void addCategory() {
        String name = newCategoryField.getText().trim();
        if (name.isEmpty()) {
            alert("Empty name", "Enter a category name.");
            return;
        }
        if (categories.contains(name)) {
            alert("Exists", "Category already exists.");
            return;
        }
        categories.add(name);
        newCategoryField.clear();
    }

    private void deleteCategory() {
        String sel = categoryCombo.getValue();
        if (sel == null || !categories.contains(sel)) {
            alert("Select", "Select an existing category to delete.");
            return;
        }
        boolean inUse = transactions.stream().anyMatch(t -> t.getCategory().equals(sel));
        if (inUse) {
            alert("In use", "Cannot delete category - some transactions still use it.");
            return;
        }
        categories.remove(sel);
    }

    private void applyTransactionToBalance(Transaction t) {
        if (t.getType().equalsIgnoreCase("Credit"))
            balance += t.getAmount();
        else
            balance -= t.getAmount();
    }

    private void reverseTransactionFromBalance(Transaction t) {
        if (t.getType().equalsIgnoreCase("Credit"))
            balance -= t.getAmount();
        else
            balance += t.getAmount();
    }

    private void updateBalanceLabel() {
        balanceLabel.setText(String.format("Balance: %.2f", balance));
    }

    private void saveToCsv() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(CSV_FILE))) {
            bw.write("id,date,category,type,method,amount,note");
            bw.newLine();
            for (Transaction t : transactions) {
                bw.write(csvEscape(t.getId()) + "," + t.getDate().toString() + "," + csvEscape(t.getCategory()) + ","
                        + t.getType() + "," + t.getMethod() + "," + t.getAmount() + "," + csvEscape(t.getNote()));
                bw.newLine();
            }
            Files.write(Paths.get("balance.meta"), String.valueOf(balance).getBytes());
            alert("Saved", "Saved transactions to " + CSV_FILE);
        } catch (IOException ex) {
            alert("Error saving", ex.getMessage());
        }
    }

    private void loadFromCsv() {
        transactions.clear();
        Path p = Paths.get(CSV_FILE);
        if (!Files.exists(p))
            return;
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length < 7)
                    continue;
                String id = parts[0];
                LocalDate date = LocalDate.parse(parts[1]);
                String cat = parts[2];
                String type = parts[3];
                String method = parts[4];
                double amt = Double.parseDouble(parts[5]);
                String note = parts[6];
                Transaction t = new Transaction(id, date, cat, type, method, amt, note);
                transactions.add(t);
            }
            Path meta = Paths.get("balance.meta");
            if (Files.exists(meta)) {
                try {
                    balance = Double.parseDouble(new String(Files.readAllBytes(meta)).trim());
                } catch (Exception ignored) {
                }
            } else {
                balance = 0.0;
                for (Transaction t : transactions)
                    applyTransactionToBalance(t);
            }
            updateBalanceLabel();
            alert("Loaded", "Loaded " + transactions.size() + " transactions.");
        } catch (IOException ex) {
            alert("Error loading", ex.getMessage());
        }
    }

    private String csvEscape(String s) {
        if (s == null)
            return "";
        if (s.contains(",") || s.contains("\"") || s.contains(""))
            return '"' + s.replace("\"", "\"\"") + '"';
        return s;
    }

    private String[] parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else
                cur.append(c);
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static class Transaction {
        private final String id;
        private final LocalDate date;
        private final StringProperty category = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty();
        private final StringProperty method = new SimpleStringProperty();
        private final DoubleProperty amount = new SimpleDoubleProperty();
        private final StringProperty note = new SimpleStringProperty();

        public Transaction(String id, LocalDate date, String category, String type, String method, double amount,
                String note) {
            this.id = id;
            this.date = date;
            this.category.set(category);
            this.type.set(type);
            this.method.set(method);
            this.amount.set(amount);
            this.note.set(note == null ? "" : note);
        }

        public String getId() {
            return id;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getDateStr() {
            return date.toString();
        }

        public String getCategory() {
            return category.get();
        }

        public String getType() {
            return type.get();
        }

        public String getMethod() {
            return method.get();
        }

        public double getAmount() {
            return amount.get();
        }

        public String getNote() {
            return note.get();
        }

        public StringProperty categoryProperty() {
            return category;
        }

        public StringProperty typeProperty() {
            return type;
        }

        public StringProperty methodProperty() {
            return method;
        }

        public DoubleProperty amountProperty() {
            return amount;
        }

        public StringProperty noteProperty() {
            return note;
        }
    }
}
