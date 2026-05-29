package com.abc.personaldashboard.fragments;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.adapters.UpcomingEventAdapter;
import com.abc.personaldashboard.database.*;
import com.google.firebase.Timestamp;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {
    private static final String DEFAULT_WALLET_REMOTE_ID = "default_cash_wallet";
    private TextView todayDateText, tasksProgressText, completedTasksText, pendingTasksText;
    private TextView totalWalletBalanceText, totalIncomeText, totalExpenseText;
    private TextView upcomingEventsEmptyText;
    private RecyclerView nearestScheduleRecyclerView;
    private FloatingActionButton financeFab, transactionFab, walletsFab;
    private AppDatabase database;
    private FirebaseFirestore firestore;
    private boolean financeActionsExpanded = false;
    private boolean financeSyncInProgress = false;
    private final NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        database = AppDatabase.getInstance(getContext());
        firestore = FirebaseFirestore.getInstance();

        todayDateText = view.findViewById(R.id.today_date);
        tasksProgressText = view.findViewById(R.id.tasks_progress);
        completedTasksText = view.findViewById(R.id.completed_tasks);
        pendingTasksText = view.findViewById(R.id.pending_tasks);
        totalWalletBalanceText = view.findViewById(R.id.total_wallet_balance);
        totalIncomeText = view.findViewById(R.id.total_income);
        totalExpenseText = view.findViewById(R.id.total_expense);
        upcomingEventsEmptyText = view.findViewById(R.id.upcoming_events_empty);
        nearestScheduleRecyclerView = view.findViewById(R.id.nearest_schedule_recycler);
        financeFab = view.findViewById(R.id.fab_finance);
        transactionFab = view.findViewById(R.id.fab_transaction);
        walletsFab = view.findViewById(R.id.fab_wallets);

        nearestScheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setupFinanceActions();

        syncFinanceFromFirestore();

        return view;
    }

    private void setupFinanceActions() {
        financeFab.setOnClickListener(view -> toggleFinanceActions());
        transactionFab.setOnClickListener(view -> {
            collapseFinanceActions();
            showTransactionDialog();
        });
        walletsFab.setOnClickListener(view -> {
            collapseFinanceActions();
            showWalletDialog();
        });
    }

    private void toggleFinanceActions() {
        if (financeActionsExpanded) {
            collapseFinanceActions();
        } else {
            expandFinanceActions();
        }
    }

    private void expandFinanceActions() {
        financeActionsExpanded = true;
        financeFab.setImageResource(R.drawable.ic_close);
        showActionFab(transactionFab);
        showActionFab(walletsFab);
    }

    private void collapseFinanceActions() {
        financeActionsExpanded = false;
        financeFab.setImageResource(R.drawable.ic_add);
        hideActionFab(transactionFab);
        hideActionFab(walletsFab);
    }

    private void showActionFab(FloatingActionButton actionFab) {
        actionFab.setVisibility(View.VISIBLE);
        actionFab.setAlpha(0f);
        actionFab.setScaleX(0.6f);
        actionFab.setScaleY(0.6f);
        actionFab.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
    }

    private void hideActionFab(FloatingActionButton actionFab) {
        actionFab.animate()
                .alpha(0f)
                .scaleX(0.6f)
                .scaleY(0.6f)
                .setDuration(120)
                .withEndAction(() -> actionFab.setVisibility(View.GONE))
                .start();
    }

    private void loadDashboardData() {
        // Set today's date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        todayDateText.setText(sdf.format(new Date()));

        // Load tasks summary in background thread
        new Thread(() -> {
            List<Task> allTasks = database.taskDao().getAllTasks();
            List<Task> completedTasks = database.taskDao().getCompletedTasks();
            List<Wallet> wallets = database.walletDao().getAllWallets();
            double walletBalance = 0;
            for (Wallet wallet : wallets) {
                walletBalance += wallet.getBalance();
            }
            double incomeTotal = database.financeTransactionDao().getTotalByType("Income");
            double expenseTotal = database.financeTransactionDao().getTotalByType("Expense");
            double finalWalletBalance = walletBalance;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tasksProgressText.setText("Total Tasks: " + allTasks.size());
                    completedTasksText.setText("Completed: " + completedTasks.size());
                    pendingTasksText.setText("Pending: " + (allTasks.size() - completedTasks.size()));
                    totalWalletBalanceText.setText("Wallet Balance: " + formatMoney(finalWalletBalance));
                    totalIncomeText.setText("Income: " + formatMoney(incomeTotal));
                    totalExpenseText.setText("Expense: " + formatMoney(expenseTotal));
                });
            }

            loadUpcomingEvents();
        }).start();
    }

    private void loadUpcomingEvents() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            loadUpcomingLocalEvents();
            return;
        }

        String startDate = todayDateKey();
        String endDate = daysFromTodayDateKey(7);
        firestore.collection("users")
                .document(user.getUid())
                .collection("calendarEvents")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<CalendarEvent> events = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        CalendarEvent event = calendarEventFromSnapshot(document);
                        if (isUpcomingEvent(event, startDate, endDate)) {
                            events.add(event);
                        }
                    }
                    showUpcomingEvents(events);
                })
                .addOnFailureListener(exception -> loadUpcomingLocalEvents());
    }

    private void loadUpcomingLocalEvents() {
        new Thread(() -> {
            List<CalendarEvent> allEvents = database.calendarEventDao().getAllEvents();
            String startDate = todayDateKey();
            String endDate = daysFromTodayDateKey(7);
            List<CalendarEvent> upcomingEvents = new ArrayList<>();
            for (CalendarEvent event : allEvents) {
                if (isUpcomingEvent(event, startDate, endDate)) {
                    upcomingEvents.add(event);
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> showUpcomingEvents(upcomingEvents));
            }
        }).start();
    }

    private void showUpcomingEvents(List<CalendarEvent> events) {
        if (!isAdded()) {
            return;
        }

        Collections.sort(events, Comparator
                .comparing((CalendarEvent event) -> event.getEventDate() == null ? "" : event.getEventDate())
                .thenComparing(event -> event.getEventTime() == null ? "" : event.getEventTime()));
        boolean hasEvents = !events.isEmpty();
        upcomingEventsEmptyText.setVisibility(hasEvents ? View.GONE : View.VISIBLE);
        nearestScheduleRecyclerView.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        nearestScheduleRecyclerView.setAdapter(new UpcomingEventAdapter(events));
    }

    private CalendarEvent calendarEventFromSnapshot(DocumentSnapshot snapshot) {
        String name = firstString(snapshot, "eventName", "name", "title");
        String date = firstDate(snapshot, "eventDate", "date", "startDate");
        String time = firstString(snapshot, "eventTime", "time", "startTime");
        String description = firstString(snapshot, "description", "notes", "detail");
        String location = firstString(snapshot, "location", "place", "address");
        return new CalendarEvent(name, date, time, description, location);
    }

    private boolean isUpcomingEvent(CalendarEvent event, String startDate, String endDate) {
        String eventDate = event.getEventDate();
        return eventDate != null
                && !eventDate.isEmpty()
                && eventDate.compareTo(startDate) >= 0
                && eventDate.compareTo(endDate) <= 0;
    }

    private String firstString(DocumentSnapshot snapshot, String... fields) {
        for (String field : fields) {
            Object value = snapshot.get(field);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private String firstDate(DocumentSnapshot snapshot, String... fields) {
        for (String field : fields) {
            Object value = snapshot.get(field);
            String date = normalizeDate(value);
            if (!date.isEmpty()) {
                return date;
            }
        }
        return "";
    }

    private String normalizeDate(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof Timestamp) {
            return dateKey(((Timestamp) value).toDate());
        }

        if (value instanceof Date) {
            return dateKey((Date) value);
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return "";
        }

        String[] patterns = {"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.ENGLISH);
                parser.setLenient(false);
                Date parsedDate = parser.parse(text);
                if (parsedDate != null) {
                    return dateKey(parsedDate);
                }
            } catch (Exception ignored) {
            }
        }

        return text.length() >= 10 ? text.substring(0, 10) : text;
    }

    private String todayDateKey() {
        return dateKey(new Date());
    }

    private String daysFromTodayDateKey(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, days);
        return dateKey(calendar.getTime());
    }

    private String dateKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date);
    }

    private void showTransactionDialog() {
        new Thread(() -> {
            List<Wallet> wallets = database.walletDao().getAllWallets();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (wallets.isEmpty()) {
                        Toast.makeText(getContext(), "Wallet is syncing. Try again in a moment.", Toast.LENGTH_SHORT).show();
                        syncFinanceFromFirestore();
                    } else {
                        buildTransactionDialog(wallets);
                    }
                });
            }
        }).start();
    }

    private void buildTransactionDialog(List<Wallet> wallets) {
        LinearLayout contentLayout = createDialogLayout();
        contentLayout.addView(createDialogTitle("Add money movement", "Track income or spending in a few taps."));

        RadioGroup typeGroup = new RadioGroup(requireContext());
        typeGroup.setOrientation(RadioGroup.HORIZONTAL);
        typeGroup.setPadding(0, dp(8), 0, dp(8));

        RadioButton expenseButton = createTypeButton("Expense", true);
        RadioButton incomeButton = createTypeButton("Income", false);
        typeGroup.addView(expenseButton);
        typeGroup.addView(incomeButton);

        Spinner walletSpinner = new Spinner(requireContext());
        List<String> walletNames = new ArrayList<>();
        for (Wallet wallet : wallets) {
            walletNames.add(wallet.getName() + " - " + formatMoney(wallet.getBalance()));
        }
        ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames);
        walletSpinner.setAdapter(walletAdapter);

        EditText amountInput = createEditText("Amount", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountInput.setTextSize(22);
        amountInput.setTypeface(null, Typeface.BOLD);
        EditText noteInput = createEditText("Note, category, or memo", InputType.TYPE_CLASS_TEXT);

        contentLayout.addView(createLabel("Type"));
        contentLayout.addView(typeGroup);
        contentLayout.addView(createLabel("Wallet"));
        contentLayout.addView(walletSpinner);
        contentLayout.addView(createLabel("Amount"));
        contentLayout.addView(amountInput);
        contentLayout.addView(createLabel("Details"));
        contentLayout.addView(noteInput);

        LinearLayout actionRow = createActionRow();
        Button cancelButton = createDialogButton("Cancel", R.color.ink_500);
        Button saveButton = createDialogButton("Save", R.color.blue_600);
        actionRow.addView(cancelButton);
        actionRow.addView(saveButton);
        contentLayout.addView(actionRow);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(contentLayout)
                .create();

        cancelButton.setOnClickListener(view -> dialog.dismiss());
        saveButton.setOnClickListener(view -> {
            double amount = parseAmount(amountInput.getText().toString());
            if (amount <= 0) {
                amountInput.setError("Enter amount");
                return;
            }

            String type = incomeButton.isChecked() ? "Income" : "Expense";
            Wallet selectedWallet = wallets.get(walletSpinner.getSelectedItemPosition());
            saveTransaction(selectedWallet.getId(), type, amount, noteInput.getText().toString().trim());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveTransaction(int walletId, String type, double amount, String note) {
        new Thread(() -> {
            Wallet wallet = database.walletDao().getWalletById(walletId);
            if (wallet != null) {
                double signedAmount = "Income".equals(type) ? amount : -amount;
                wallet.setBalance(wallet.getBalance() + signedAmount);
                database.walletDao().update(wallet);
                saveWalletToFirestore(wallet);

                FinanceTransaction transaction = new FinanceTransaction(walletId, type, amount, note);
                transaction.setWalletRemoteId(wallet.getRemoteId());
                transaction.setWalletOwnerId(walletOwnerId(wallet));
                database.financeTransactionDao().insert(transaction);
                saveTransactionToFirestore(transaction);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Transaction saved", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                });
            }
        }).start();
    }

    private void showWalletDialog() {
        new Thread(() -> {
            List<Wallet> wallets = database.walletDao().getAllWallets();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (wallets.isEmpty()) {
                        Toast.makeText(getContext(), "Wallet is syncing. Try again in a moment.", Toast.LENGTH_SHORT).show();
                        syncFinanceFromFirestore();
                    } else {
                        buildWalletDialog(wallets);
                    }
                });
            }
        }).start();
    }

    private void buildWalletDialog(List<Wallet> wallets) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout contentLayout = createDialogLayout();
        scrollView.addView(contentLayout);
        contentLayout.addView(createDialogTitle("Wallets", "Edit balances, rename wallets, or create a new one."));

        if (hasDuplicateWalletNames(wallets)) {
            TextView warningText = createLabel("Duplicate wallet names found. Rename one wallet before creating another with the same name.");
            warningText.setTextColor(getResources().getColor(R.color.expense_red, null));
            contentLayout.addView(warningText);
        }

        for (Wallet wallet : wallets) {
            LinearLayout walletCard = new LinearLayout(requireContext());
            walletCard.setOrientation(LinearLayout.VERTICAL);
            walletCard.setPadding(dp(14), dp(12), dp(14), dp(12));
            walletCard.setBackground(createRoundedBackground(R.color.white, R.color.line_blue, 8));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, dp(10), 0, dp(8));
            walletCard.setLayoutParams(cardParams);

            TextView walletNameText = new TextView(requireContext());
            walletNameText.setText(wallet.isShared() ? wallet.getName() + " (Shared)" : wallet.getName());
            walletNameText.setTextColor(getResources().getColor(R.color.ink_900, null));
            walletNameText.setTextSize(17);
            walletNameText.setTypeface(null, Typeface.BOLD);

            EditText nameInput = createEditText("Wallet name", InputType.TYPE_CLASS_TEXT);
            nameInput.setText(wallet.getName());
            nameInput.setEnabled(!wallet.isShared());
            EditText balanceInput = createEditText("Balance", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            balanceInput.setText(String.valueOf(wallet.getBalance()));

            LinearLayout actionRow = createActionRow();
            Button updateButton = createDialogButton("Update", R.color.blue_600);
            Button deleteButton = createDialogButton("Delete", R.color.expense_red);
            deleteButton.setEnabled(!wallet.isShared() && wallets.size() > 1);
            updateButton.setOnClickListener(view -> {
                String walletName = nameInput.getText().toString().trim();
                if (walletName.isEmpty()) {
                    nameInput.setError("Enter wallet name");
                    return;
                }

                if (isDuplicateWalletName(wallets, walletName, wallet.getId())) {
                    nameInput.setError("Wallet already exists");
                    Toast.makeText(getContext(), "Wallet name already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                double newBalance = parseAmount(balanceInput.getText().toString());
                wallet.setName(walletName);
                wallet.setBalance(newBalance);
                updateWallet(wallet);
            });
            deleteButton.setOnClickListener(view -> confirmDeleteWallet(wallet));

            actionRow.addView(updateButton);
            actionRow.addView(deleteButton);
            if (!wallet.isShared()) {
                Button shareButton = createDialogButton("Share", R.color.wallet_green);
                shareButton.setOnClickListener(view -> showShareWalletDialog(wallet));
                actionRow.addView(shareButton);
            }
            walletCard.addView(walletNameText);
            if (wallet.getSharedWithEmail() != null && !wallet.getSharedWithEmail().isEmpty()) {
                walletCard.addView(createLabel("Shared with " + wallet.getSharedWithEmail()));
            }
            walletCard.addView(nameInput);
            walletCard.addView(balanceInput);
            walletCard.addView(actionRow);
            contentLayout.addView(walletCard);
        }

        TextView newWalletTitle = createSectionTitle("New Wallet");

        EditText walletNameInput = createEditText("Wallet name", InputType.TYPE_CLASS_TEXT);
        EditText walletBalanceInput = createEditText("Starting balance", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        Button addWalletButton = createFullWidthDialogButton("Create Wallet", R.color.wallet_green);

        contentLayout.addView(newWalletTitle);
        contentLayout.addView(walletNameInput);
        contentLayout.addView(walletBalanceInput);
        contentLayout.addView(addWalletButton);

        LinearLayout actionRow = createActionRow();
        Button closeButton = createDialogButton("Close", R.color.ink_500);
        actionRow.addView(closeButton);
        contentLayout.addView(actionRow);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(scrollView)
                .create();

        addWalletButton.setOnClickListener(view -> {
            String walletName = walletNameInput.getText().toString().trim();
            if (walletName.isEmpty()) {
                walletNameInput.setError("Enter wallet name");
                return;
            }

            if (isDuplicateWalletName(wallets, walletName, 0)) {
                walletNameInput.setError("Wallet already exists");
                Toast.makeText(getContext(), "Wallet name already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            createWallet(walletName, parseAmount(walletBalanceInput.getText().toString()));
            dialog.dismiss();
        });
        closeButton.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void showShareWalletDialog(Wallet wallet) {
        LinearLayout contentLayout = createDialogLayout();
        contentLayout.addView(createDialogTitle("Share " + wallet.getName(), "Invite an existing account by email."));

        EditText emailInput = createEditText("User email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        contentLayout.addView(createLabel("Email"));
        contentLayout.addView(emailInput);

        LinearLayout actionRow = createActionRow();
        Button cancelButton = createDialogButton("Cancel", R.color.ink_500);
        Button shareButton = createDialogButton("Share", R.color.wallet_green);
        actionRow.addView(cancelButton);
        actionRow.addView(shareButton);
        contentLayout.addView(actionRow);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(contentLayout)
                .create();

        cancelButton.setOnClickListener(view -> dialog.dismiss());
        shareButton.setOnClickListener(view -> {
            String email = emailInput.getText().toString().trim().toLowerCase(Locale.US);
            if (email.isEmpty() || !email.contains("@")) {
                emailInput.setError("Enter a valid email");
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "Please sign in again", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.equals(currentUser.getEmail() == null ? "" : currentUser.getEmail().toLowerCase(Locale.US))) {
                emailInput.setError("Choose another user");
                return;
            }

            shareButton.setEnabled(false);
            shareWalletWithEmail(wallet, email, dialog, emailInput, shareButton);
        });

        dialog.show();
    }

    private void shareWalletWithEmail(Wallet wallet, String email, AlertDialog dialog, EditText emailInput, Button shareButton) {
        firestore.collection("users")
                .whereEqualTo("emailLowercase", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userSnapshot = querySnapshot.getDocuments().get(0);
                        shareWalletWithUser(wallet, userSnapshot.getId(), email, dialog, shareButton);
                        return;
                    }

                    firestore.collection("users")
                            .whereEqualTo("email", email)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(fallbackSnapshot -> {
                                if (fallbackSnapshot.isEmpty()) {
                                    emailInput.setError("No account found for this email");
                                    Toast.makeText(getContext(), "User email is not registered", Toast.LENGTH_SHORT).show();
                                    shareButton.setEnabled(true);
                                    return;
                                }

                                DocumentSnapshot userSnapshot = fallbackSnapshot.getDocuments().get(0);
                                shareWalletWithUser(wallet, userSnapshot.getId(), email, dialog, shareButton);
                            })
                            .addOnFailureListener(exception -> {
                                Toast.makeText(getContext(), "Could not check this email", Toast.LENGTH_SHORT).show();
                                shareButton.setEnabled(true);
                            });
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(getContext(), "Could not check this email", Toast.LENGTH_SHORT).show();
                    shareButton.setEnabled(true);
                });
    }

    private void shareWalletWithUser(Wallet wallet, String sharedUserId, String sharedEmail, AlertDialog dialog, Button shareButton) {
        String ownerId = walletOwnerId(wallet);
        if (ownerId == null || wallet.getRemoteId() == null || wallet.getRemoteId().isEmpty()) {
            Toast.makeText(getContext(), "Wallet is still syncing. Try again soon.", Toast.LENGTH_SHORT).show();
            shareButton.setEnabled(true);
            return;
        }

        Map<String, Object> shareData = new HashMap<>();
        shareData.put("sharedWith", sharedUserId);
        shareData.put("sharedWithEmail", sharedEmail);
        shareData.put("sharedCanEditBalance", true);
        shareData.put("updatedAt", FieldValue.serverTimestamp());

        walletsCollection(ownerId).document(wallet.getRemoteId())
                .set(shareData, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    wallet.setSharedWith(sharedUserId);
                    wallet.setSharedWithEmail(sharedEmail);
                    wallet.setSharedCanEditBalance(true);
                    new Thread(() -> database.walletDao().update(wallet)).start();
                    Toast.makeText(getContext(), "Wallet shared", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadDashboardData();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(getContext(), "Could not share wallet", Toast.LENGTH_SHORT).show();
                    shareButton.setEnabled(true);
                });
    }

    private void updateWallet(Wallet wallet) {
        new Thread(() -> {
            Wallet duplicateWallet = wallet.isShared() ? null : database.walletDao().getWalletByName(wallet.getName());
            if (duplicateWallet != null && duplicateWallet.getId() != wallet.getId()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Wallet name already exists", Toast.LENGTH_SHORT).show());
                }
                return;
            }

            database.walletDao().update(wallet);
            saveWalletToFirestore(wallet);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Wallet updated", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                });
            }
        }).start();
    }

    private void createWallet(String name, double balance) {
        new Thread(() -> {
            if (database.walletDao().getWalletByName(name) != null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Wallet name already exists", Toast.LENGTH_SHORT).show());
                }
                return;
            }

            Wallet wallet = new Wallet(name, balance);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                wallet.setOwnerId(user.getUid());
            }
            long walletId = database.walletDao().insert(wallet);
            wallet.setId((int) walletId);
            saveWalletToFirestore(wallet);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Wallet created", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                });
            }
        }).start();
    }

    private void confirmDeleteWallet(Wallet wallet) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + wallet.getName() + "?")
                .setMessage("This removes the wallet and its related transactions.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteWallet(wallet))
                .show();
    }

    private void deleteWallet(Wallet wallet) {
        new Thread(() -> {
            database.financeTransactionDao().deleteByWalletId(wallet.getId());
            database.walletDao().delete(wallet);
            deleteWalletFromFirestore(wallet);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Wallet deleted", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                });
            }
        }).start();
    }

    private void syncFinanceFromFirestore() {
        if (financeSyncInProgress) {
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            financeSyncInProgress = false;
            loadDashboardData();
            return;
        }

        financeSyncInProgress = true;
        walletsCollection(user.getUid()).get()
                .addOnSuccessListener(walletSnapshots -> {
                    if (!isAdded()) {
                        financeSyncInProgress = false;
                        return;
                    }

                    if (walletSnapshots.isEmpty()) {
                        createDefaultWalletInFirestore(user.getUid());
                        return;
                    }

                    loadSharedWalletsAndTransactions(user.getUid(), walletSnapshots);
                })
                .addOnFailureListener(exception -> {
                    financeSyncInProgress = false;
                    loadDashboardData();
                });
    }

    private void loadSharedWalletsAndTransactions(String userId, QuerySnapshot ownedWalletSnapshots) {
        firestore.collectionGroup("wallets")
                .whereEqualTo("sharedWith", userId)
                .get()
                .addOnSuccessListener(sharedWalletSnapshots -> transactionsCollection(userId).get()
                        .addOnSuccessListener(transactionSnapshots -> cacheFirestoreFinance(userId, ownedWalletSnapshots, sharedWalletSnapshots, transactionSnapshots))
                        .addOnFailureListener(exception -> {
                            financeSyncInProgress = false;
                            loadDashboardData();
                        }))
                .addOnFailureListener(exception -> transactionsCollection(userId).get()
                        .addOnSuccessListener(transactionSnapshots -> cacheFirestoreFinance(userId, ownedWalletSnapshots, null, transactionSnapshots))
                        .addOnFailureListener(transactionException -> {
                            financeSyncInProgress = false;
                            loadDashboardData();
                        }));
    }

    private void createDefaultWalletInFirestore(String userId) {
        Wallet wallet = new Wallet("Cash", 0);
        wallet.setOwnerId(userId);
        DocumentReference walletDocument = walletsCollection(userId).document(DEFAULT_WALLET_REMOTE_ID);
        wallet.setRemoteId(walletDocument.getId());

        walletDocument.set(walletToMap(wallet), SetOptions.merge())
                .addOnCompleteListener(task -> new Thread(() -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            financeSyncInProgress = false;
                            syncFinanceFromFirestore();
                        });
                    } else {
                        financeSyncInProgress = false;
                    }
                }).start());
    }

    private void cacheFirestoreFinance(String userId, QuerySnapshot walletSnapshots, QuerySnapshot sharedWalletSnapshots, QuerySnapshot transactionSnapshots) {
        new Thread(() -> {
            database.financeTransactionDao().deleteAll();
            database.walletDao().deleteAll();

            Map<String, Integer> walletLocalIds = new HashMap<>();
            Map<String, Wallet> uniqueWallets = new HashMap<>();
            Map<String, String> walletRemoteIdRemap = new HashMap<>();
            for (DocumentSnapshot walletSnapshot : walletSnapshots.getDocuments()) {
                Wallet wallet = walletFromSnapshot(walletSnapshot, userId, false);
                String walletKey = walletStorageKey(wallet);
                uniqueWallets.put(walletKey, wallet);
                walletRemoteIdRemap.put(wallet.getRemoteId(), wallet.getRemoteId());
            }
            if (sharedWalletSnapshots != null) {
                for (DocumentSnapshot walletSnapshot : sharedWalletSnapshots.getDocuments()) {
                    String ownerId = ownerIdFromWalletSnapshot(walletSnapshot);
                    if (ownerId == null || ownerId.equals(userId)) {
                        continue;
                    }

                    Wallet wallet = walletFromSnapshot(walletSnapshot, ownerId, true);
                    uniqueWallets.put(walletStorageKey(wallet), wallet);
                    walletRemoteIdRemap.put(wallet.getRemoteId(), wallet.getRemoteId());
                }
            }

            for (Wallet wallet : uniqueWallets.values()) {
                long localId = database.walletDao().insert(wallet);
                walletLocalIds.put(walletStorageKey(wallet), (int) localId);
            }

            for (DocumentSnapshot transactionSnapshot : transactionSnapshots.getDocuments()) {
                FinanceTransaction transaction = transactionFromSnapshot(transactionSnapshot);
                String walletRemoteId = walletRemoteIdRemap.containsKey(transaction.getWalletRemoteId())
                        ? walletRemoteIdRemap.get(transaction.getWalletRemoteId())
                        : transaction.getWalletRemoteId();
                if (walletRemoteId != null && !walletRemoteId.equals(transaction.getWalletRemoteId())) {
                    transactionSnapshot.getReference().update("walletRemoteId", walletRemoteId);
                    transaction.setWalletRemoteId(walletRemoteId);
                }
                String walletOwnerId = transaction.getWalletOwnerId() == null || transaction.getWalletOwnerId().isEmpty()
                        ? userId
                        : transaction.getWalletOwnerId();
                Integer walletId = walletLocalIds.get(walletOwnerId + ":" + walletRemoteId);
                if (walletId != null) {
                    transaction.setWalletId(walletId);
                    database.financeTransactionDao().insert(transaction);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    financeSyncInProgress = false;
                    loadDashboardData();
                });
            } else {
                financeSyncInProgress = false;
            }
        }).start();
    }

    private Wallet walletFromSnapshot(DocumentSnapshot snapshot, String ownerId, boolean shared) {
        String name = snapshot.getString("name");
        Double balance = snapshot.getDouble("balance");
        String createdAt = snapshot.getString("createdAt");
        Boolean sharedCanEditBalance = snapshot.getBoolean("sharedCanEditBalance");

        Wallet wallet = new Wallet(name == null ? "Wallet" : name, balance == null ? 0 : balance);
        wallet.setRemoteId(snapshot.getId());
        wallet.setOwnerId(ownerId);
        wallet.setShared(shared);
        wallet.setSharedCanEditBalance(sharedCanEditBalance != null && sharedCanEditBalance);
        wallet.setSharedWith(snapshot.getString("sharedWith"));
        wallet.setSharedWithEmail(snapshot.getString("sharedWithEmail"));
        wallet.setCreatedAt(createdAt == null ? String.valueOf(System.currentTimeMillis()) : createdAt);
        return wallet;
    }

    private FinanceTransaction transactionFromSnapshot(DocumentSnapshot snapshot) {
        String type = snapshot.getString("type");
        String note = snapshot.getString("note");
        String walletRemoteId = snapshot.getString("walletRemoteId");
        String walletOwnerId = snapshot.getString("walletOwnerId");
        String createdAt = snapshot.getString("createdAt");
        Double amount = snapshot.getDouble("amount");

        FinanceTransaction transaction = new FinanceTransaction(0, type == null ? "Expense" : type, amount == null ? 0 : amount, note == null ? "" : note);
        transaction.setRemoteId(snapshot.getId());
        transaction.setWalletRemoteId(walletRemoteId);
        transaction.setWalletOwnerId(walletOwnerId);
        transaction.setCreatedAt(createdAt == null ? String.valueOf(System.currentTimeMillis()) : createdAt);
        return transaction;
    }

    private void saveWalletToFirestore(Wallet wallet) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        if (wallet.getRemoteId() == null || wallet.getRemoteId().isEmpty()) {
            String walletRemoteId = isDefaultWalletName(wallet.getName())
                    ? DEFAULT_WALLET_REMOTE_ID
                    : walletsCollection(user.getUid()).document().getId();
            wallet.setRemoteId(walletRemoteId);
        }
        if (wallet.getOwnerId() == null || wallet.getOwnerId().isEmpty()) {
            wallet.setOwnerId(user.getUid());
        }

        DocumentReference walletDocument = walletsCollection(walletOwnerId(wallet)).document(wallet.getRemoteId());

        if (wallet.getId() > 0) {
            wallet.setRemoteId(walletDocument.getId());
            new Thread(() -> database.walletDao().update(wallet)).start();
        }

        walletDocument.set(walletToMap(wallet), SetOptions.merge());
    }

    private void deleteWalletFromFirestore(Wallet wallet) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || wallet.getRemoteId() == null || wallet.getRemoteId().isEmpty()) {
            return;
        }

        String userId = walletOwnerId(wallet);
        walletsCollection(userId).document(wallet.getRemoteId()).delete();
        transactionsCollection(userId)
                .whereEqualTo("walletRemoteId", wallet.getRemoteId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        documentSnapshot.getReference().delete();
                    }
                });
    }

    private void mergeDuplicateWalletInFirestore(String userId, String duplicateWalletRemoteId, Wallet keptWallet) {
        if (duplicateWalletRemoteId == null || duplicateWalletRemoteId.equals(keptWallet.getRemoteId())) {
            return;
        }

        walletsCollection(userId).document(keptWallet.getRemoteId()).set(walletToMap(keptWallet), SetOptions.merge());
        walletsCollection(userId).document(duplicateWalletRemoteId).delete();
    }

    private void saveTransactionToFirestore(FinanceTransaction transaction) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DocumentReference transactionDocument = transactionsCollection(user.getUid()).document();
        transaction.setRemoteId(transactionDocument.getId());
        transactionDocument.set(transactionToMap(transaction), SetOptions.merge());
    }

    private Map<String, Object> walletToMap(Wallet wallet) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", wallet.getName());
        data.put("balance", wallet.getBalance());
        data.put("createdAt", wallet.getCreatedAt());
        data.put("ownerId", walletOwnerId(wallet));
        if (wallet.getSharedWith() != null && !wallet.getSharedWith().isEmpty()) {
            data.put("sharedWith", wallet.getSharedWith());
            data.put("sharedWithEmail", wallet.getSharedWithEmail() == null ? "" : wallet.getSharedWithEmail());
            data.put("sharedCanEditBalance", wallet.isSharedCanEditBalance());
        }
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private Map<String, Object> transactionToMap(FinanceTransaction transaction) {
        Map<String, Object> data = new HashMap<>();
        data.put("walletRemoteId", transaction.getWalletRemoteId());
        data.put("walletOwnerId", transaction.getWalletOwnerId());
        data.put("type", transaction.getType());
        data.put("amount", transaction.getAmount());
        data.put("note", transaction.getNote());
        data.put("createdAt", transaction.getCreatedAt());
        data.put("updatedAt", FieldValue.serverTimestamp());
        return data;
    }

    private CollectionReference walletsCollection(String userId) {
        return firestore.collection("users").document(userId).collection("wallets");
    }

    private CollectionReference transactionsCollection(String userId) {
        return firestore.collection("users").document(userId).collection("finance_transactions");
    }

    private String walletOwnerId(Wallet wallet) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (wallet.getOwnerId() != null && !wallet.getOwnerId().isEmpty()) {
            return wallet.getOwnerId();
        }
        return user == null ? null : user.getUid();
    }

    private String ownerIdFromWalletSnapshot(DocumentSnapshot snapshot) {
        DocumentReference userDocument = snapshot.getReference().getParent().getParent();
        return userDocument == null ? null : userDocument.getId();
    }

    private String walletStorageKey(Wallet wallet) {
        return walletOwnerId(wallet) + ":" + wallet.getRemoteId();
    }

    private LinearLayout createDialogLayout() {
        LinearLayout contentLayout = new LinearLayout(requireContext());
        int padding = dp(20);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(padding, padding, padding, padding);
        return contentLayout;
    }

    private LinearLayout createDialogTitle(String title, String subtitle) {
        LinearLayout titleLayout = new LinearLayout(requireContext());
        titleLayout.setOrientation(LinearLayout.VERTICAL);
        titleLayout.setPadding(0, 0, 0, dp(12));

        TextView titleText = new TextView(requireContext());
        titleText.setText(title);
        titleText.setTextColor(getResources().getColor(R.color.ink_900, null));
        titleText.setTextSize(22);
        titleText.setTypeface(null, Typeface.BOLD);

        TextView subtitleText = new TextView(requireContext());
        subtitleText.setText(subtitle);
        subtitleText.setTextColor(getResources().getColor(R.color.ink_500, null));
        subtitleText.setTextSize(14);
        subtitleText.setPadding(0, dp(4), 0, 0);

        titleLayout.addView(titleText);
        titleLayout.addView(subtitleText);
        return titleLayout;
    }

    private TextView createSectionTitle(String text) {
        TextView titleText = new TextView(requireContext());
        titleText.setText(text);
        titleText.setTextColor(getResources().getColor(R.color.ink_900, null));
        titleText.setTextSize(17);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setPadding(0, dp(16), 0, dp(4));
        return titleText;
    }

    private TextView createLabel(String text) {
        TextView labelText = new TextView(requireContext());
        labelText.setText(text);
        labelText.setTextColor(getResources().getColor(R.color.ink_700, null));
        labelText.setTextSize(13);
        labelText.setTypeface(null, Typeface.BOLD);
        labelText.setPadding(0, dp(8), 0, 0);
        return labelText;
    }

    private RadioButton createTypeButton(String text, boolean checked) {
        RadioButton radioButton = new RadioButton(requireContext());
        radioButton.setText(text);
        radioButton.setChecked(checked);
        radioButton.setTextColor(getResources().getColor(R.color.ink_900, null));
        radioButton.setTextSize(15);
        radioButton.setTypeface(null, Typeface.BOLD);
        radioButton.setButtonTintList(ColorStateList.valueOf(getResources().getColor(R.color.blue_600, null)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        radioButton.setLayoutParams(params);
        return radioButton;
    }

    private LinearLayout createActionRow() {
        LinearLayout actionRow = new LinearLayout(requireContext());
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(10), 0, 0);
        return actionRow;
    }

    private Button createDialogButton(String text, int colorRes) {
        Button button = new Button(requireContext());
        button.setText(text);
        button.setTextColor(getResources().getColor(R.color.white, null));
        button.setAllCaps(false);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(colorRes, null)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(48),
                1
        );
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button createFullWidthDialogButton(String text, int colorRes) {
        Button button = createDialogButton(text, colorRes);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        params.setMargins(0, dp(8), 0, dp(8));
        button.setLayoutParams(params);
        return button;
    }

    private EditText createEditText(String hint, int inputType) {
        EditText editText = new EditText(requireContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, dp(8), 0, dp(8));
        editText.setLayoutParams(layoutParams);
        editText.setHint(hint);
        editText.setInputType(inputType);
        editText.setSingleLine(inputType != InputType.TYPE_CLASS_TEXT);
        editText.setTextColor(getResources().getColor(R.color.ink_900, null));
        editText.setHintTextColor(getResources().getColor(R.color.ink_500, null));
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackground(createRoundedBackground(R.color.blue_50, R.color.line_blue, 8));
        return editText;
    }

    private GradientDrawable createRoundedBackground(int fillColorRes, int strokeColorRes, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getResources().getColor(fillColorRes, null));
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), getResources().getColor(strokeColorRes, null));
        return drawable;
    }

    private boolean hasDuplicateWalletNames(List<Wallet> wallets) {
        Map<String, Integer> walletNameCounts = new HashMap<>();
        for (Wallet wallet : wallets) {
            String key = normalizeWalletName(wallet.getName());
            walletNameCounts.put(key, walletNameCounts.containsKey(key) ? walletNameCounts.get(key) + 1 : 1);
            if (walletNameCounts.get(key) > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateWalletName(List<Wallet> wallets, String walletName, int currentWalletId) {
        String normalizedWalletName = normalizeWalletName(walletName);
        for (Wallet wallet : wallets) {
            if (wallet.getId() != currentWalletId && normalizeWalletName(wallet.getName()).equals(normalizedWalletName)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeWalletName(String walletName) {
        return walletName == null ? "" : walletName.trim().toLowerCase(Locale.US);
    }

    private boolean isDefaultWalletName(String walletName) {
        return "cash".equals(normalizeWalletName(walletName));
    }

    private double parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String formatMoney(double amount) {
        return rupiahFormat.format(amount);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onResume() {
        super.onResume();
        syncFinanceFromFirestore();
    }
}
