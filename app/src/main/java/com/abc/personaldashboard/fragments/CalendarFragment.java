package com.abc.personaldashboard.fragments;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.database.AppDatabase;
import com.abc.personaldashboard.database.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {
    private enum CalendarMode {
        MONTHLY,
        WEEKLY,
        ANNUAL
    }

    private static final String STORAGE_DATE_FORMAT = "yyyy-MM-dd";
    private static final int DAYS_IN_WEEK = 7;

    private Button monthViewButton;
    private Button weekViewButton;
    private Button yearViewButton;
    private TextView calendarTitle;
    private TextView selectedDateTitle;
    private LinearLayout calendarContainer;
    private LinearLayout eventsList;
    private AppDatabase database;
    private CalendarMode currentMode = CalendarMode.MONTHLY;
    private Calendar visibleDate = Calendar.getInstance();
    private Calendar selectedDate = Calendar.getInstance();
    private Map<String, List<CalendarEvent>> eventsByDate = new HashMap<>();
    private final SimpleDateFormat storageFormatter = new SimpleDateFormat(STORAGE_DATE_FORMAT, Locale.ENGLISH);
    private final SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
    private final SimpleDateFormat weekFormatter = new SimpleDateFormat("MMM d", Locale.ENGLISH);
    private final SimpleDateFormat fullDateFormatter = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        database = AppDatabase.getInstance(getContext());
        monthViewButton = view.findViewById(R.id.month_view_button);
        weekViewButton = view.findViewById(R.id.week_view_button);
        yearViewButton = view.findViewById(R.id.year_view_button);
        calendarTitle = view.findViewById(R.id.calendar_title);
        selectedDateTitle = view.findViewById(R.id.selected_date_title);
        calendarContainer = view.findViewById(R.id.calendar_container);
        eventsList = view.findViewById(R.id.events_list);

        Button previousButton = view.findViewById(R.id.previous_period_button);
        Button nextButton = view.findViewById(R.id.next_period_button);

        monthViewButton.setOnClickListener(v -> switchMode(CalendarMode.MONTHLY));
        weekViewButton.setOnClickListener(v -> switchMode(CalendarMode.WEEKLY));
        yearViewButton.setOnClickListener(v -> switchMode(CalendarMode.ANNUAL));
        previousButton.setOnClickListener(v -> movePeriod(-1));
        nextButton.setOnClickListener(v -> movePeriod(1));

        loadEvents();
        return view;
    }

    private void switchMode(CalendarMode mode) {
        currentMode = mode;
        visibleDate = (Calendar) selectedDate.clone();
        renderCalendar();
    }

    private void movePeriod(int amount) {
        if (currentMode == CalendarMode.MONTHLY) {
            visibleDate.add(Calendar.MONTH, amount);
        } else if (currentMode == CalendarMode.WEEKLY) {
            visibleDate.add(Calendar.WEEK_OF_YEAR, amount);
        } else {
            visibleDate.add(Calendar.YEAR, amount);
        }
        renderCalendar();
    }

    private void loadEvents() {
        new Thread(() -> {
            List<CalendarEvent> events = database.calendarEventDao().getAllEvents();
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                eventsByDate = groupEventsByDate(events);
                renderCalendar();
                renderSelectedEvents();
            });
        }).start();
    }

    private Map<String, List<CalendarEvent>> groupEventsByDate(List<CalendarEvent> events) {
        Map<String, List<CalendarEvent>> groupedEvents = new HashMap<>();
        for (CalendarEvent event : events) {
            String date = event.getEventDate();
            if (!groupedEvents.containsKey(date)) {
                groupedEvents.put(date, new ArrayList<>());
            }
            groupedEvents.get(date).add(event);
        }
        return groupedEvents;
    }

    private void renderCalendar() {
        if (getContext() == null) {
            return;
        }

        calendarContainer.removeAllViews();
        updateModeButtons();

        if (currentMode == CalendarMode.MONTHLY) {
            calendarTitle.setText(monthFormatter.format(visibleDate.getTime()));
            renderMonthView();
        } else if (currentMode == CalendarMode.WEEKLY) {
            renderWeekView();
        } else {
            calendarTitle.setText(String.valueOf(visibleDate.get(Calendar.YEAR)));
            renderAnnualView();
        }
    }

    private void renderMonthView() {
        addWeekdayHeader(calendarContainer);

        GridLayout grid = createGrid(DAYS_IN_WEEK);
        Calendar cursor = firstVisibleDayForMonth(visibleDate);
        for (int index = 0; index < 42; index++) {
            Calendar date = (Calendar) cursor.clone();
            TextView dayCell = createDateCell(date, date.get(Calendar.MONTH) == visibleDate.get(Calendar.MONTH), true);
            grid.addView(dayCell);
            cursor.add(Calendar.DATE, 1);
        }
        calendarContainer.addView(grid);
    }

    private void renderWeekView() {
        Calendar startOfWeek = startOfWeek(visibleDate);
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DATE, 6);
        calendarTitle.setText(weekFormatter.format(startOfWeek.getTime()) + " - " + weekFormatter.format(endOfWeek.getTime()));

        GridLayout grid = createGrid(DAYS_IN_WEEK);
        Calendar cursor = (Calendar) startOfWeek.clone();
        for (int index = 0; index < DAYS_IN_WEEK; index++) {
            Calendar date = (Calendar) cursor.clone();
            TextView dayCell = createDateCell(date, true, false);
            grid.addView(dayCell);
            cursor.add(Calendar.DATE, 1);
        }
        calendarContainer.addView(grid);
    }

    private void renderAnnualView() {
        int year = visibleDate.get(Calendar.YEAR);
        for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; month++) {
            TextView monthTitle = new TextView(getContext());
            monthTitle.setText(new SimpleDateFormat("MMMM", Locale.ENGLISH).format(monthDate(year, month).getTime()));
            monthTitle.setTextColor(color(R.color.ink_900));
            monthTitle.setTextSize(15);
            monthTitle.setTypeface(Typeface.DEFAULT_BOLD);
            monthTitle.setPadding(4, dp(12), 4, dp(6));
            calendarContainer.addView(monthTitle);

            addWeekdayHeader(calendarContainer);
            GridLayout grid = createGrid(DAYS_IN_WEEK);
            Calendar cursor = firstVisibleDayForMonth(monthDate(year, month));
            for (int index = 0; index < 42; index++) {
                Calendar date = (Calendar) cursor.clone();
                TextView dayCell = createDateCell(date, date.get(Calendar.MONTH) == month, true);
                grid.addView(dayCell);
                cursor.add(Calendar.DATE, 1);
            }
            calendarContainer.addView(grid);
        }
    }

    private TextView createDateCell(Calendar date, boolean isCurrentPeriod, boolean compact) {
        String dateKey = storageFormatter.format(date.getTime());
        List<CalendarEvent> events = eventsByDate.get(dateKey);
        boolean hasEvents = events != null && !events.isEmpty();
        boolean isSelected = isSameDay(date, selectedDate);
        boolean isToday = isSameDay(date, Calendar.getInstance());

        TextView cell = new TextView(getContext());
        cell.setGravity(Gravity.CENTER);
        cell.setMinHeight(compact ? dp(52) : dp(86));
        cell.setTextColor(isCurrentPeriod ? color(R.color.ink_900) : color(R.color.ink_500));
        cell.setTextSize(compact ? 13 : 14);
        cell.setTypeface(Typeface.DEFAULT, isSelected || isToday ? Typeface.BOLD : Typeface.NORMAL);
        cell.setPadding(dp(3), dp(5), dp(3), dp(5));
        cell.setText(buildCellText(date, events, compact));
        cell.setBackground(createCellBackground(isSelected, isToday, hasEvents));
        cell.setOnClickListener(v -> {
            selectedDate = (Calendar) date.clone();
            visibleDate = (Calendar) date.clone();
            renderCalendar();
            renderSelectedEvents();
            showAddEventDialog(date);
        });

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        cell.setLayoutParams(params);
        return cell;
    }

    private String buildCellText(Calendar date, List<CalendarEvent> events, boolean compact) {
        StringBuilder text = new StringBuilder();
        if (!compact) {
            text.append(new SimpleDateFormat("EEE", Locale.ENGLISH).format(date.getTime())).append("\n");
        }
        text.append(date.get(Calendar.DAY_OF_MONTH));
        if (events != null && !events.isEmpty()) {
            text.append("\n").append(events.size()).append(events.size() == 1 ? " event" : " events");
        }
        return text.toString();
    }

    private GradientDrawable createCellBackground(boolean isSelected, boolean isToday, boolean hasEvents) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(8));
        if (isSelected) {
            drawable.setColor(color(R.color.cyan_100));
            drawable.setStroke(dp(2), color(R.color.cyan_500));
        } else if (isToday) {
            drawable.setColor(color(R.color.blue_50));
            drawable.setStroke(dp(1), color(R.color.blue_500));
        } else if (hasEvents) {
            drawable.setColor(color(R.color.cyan_50));
            drawable.setStroke(dp(1), color(R.color.cyan_300));
        } else {
            drawable.setColor(color(R.color.white));
            drawable.setStroke(dp(1), color(R.color.blue_100));
        }
        return drawable;
    }

    private void addWeekdayHeader(LinearLayout parent) {
        GridLayout header = createGrid(DAYS_IN_WEEK);
        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String weekday : weekdays) {
            TextView label = new TextView(getContext());
            label.setGravity(Gravity.CENTER);
            label.setText(weekday);
            label.setTextColor(color(R.color.ink_500));
            label.setTextSize(12);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            label.setLayoutParams(params);
            header.addView(label);
        }
        parent.addView(header);
    }

    private GridLayout createGrid(int columns) {
        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(columns);
        grid.setUseDefaultMargins(false);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return grid;
    }

    private void renderSelectedEvents() {
        eventsList.removeAllViews();
        String selectedDateKey = storageFormatter.format(selectedDate.getTime());
        List<CalendarEvent> selectedEvents = eventsByDate.get(selectedDateKey);
        selectedDateTitle.setText(fullDateFormatter.format(selectedDate.getTime()));

        if (selectedEvents == null || selectedEvents.isEmpty()) {
            TextView emptyView = createEventText("No events yet. Tap a date to add one.", false);
            eventsList.addView(emptyView);
            return;
        }

        for (CalendarEvent event : selectedEvents) {
            StringBuilder eventText = new StringBuilder();
            if (event.getEventTime() != null && !event.getEventTime().trim().isEmpty()) {
                eventText.append(event.getEventTime()).append(" · ");
            }
            eventText.append(event.getEventName());
            if (event.getLocation() != null && !event.getLocation().trim().isEmpty()) {
                eventText.append("\n").append(event.getLocation());
            }
            if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
                eventText.append("\n").append(event.getDescription());
            }
            eventsList.addView(createEventText(eventText.toString(), true));
        }
    }

    private TextView createEventText(String text, boolean hasEvent) {
        TextView eventView = new TextView(getContext());
        eventView.setText(text);
        eventView.setTextColor(hasEvent ? color(R.color.ink_900) : color(R.color.ink_500));
        eventView.setTextSize(14);
        eventView.setPadding(dp(10), dp(9), dp(10), dp(9));
        eventView.setBackground(createEventBackground(hasEvent));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        eventView.setLayoutParams(params);
        return eventView;
    }

    private GradientDrawable createEventBackground(boolean hasEvent) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(8));
        drawable.setColor(color(hasEvent ? R.color.blue_50 : R.color.white));
        drawable.setStroke(dp(1), color(R.color.blue_100));
        return drawable;
    }

    private void showAddEventDialog(Calendar date) {
        if (getContext() == null) {
            return;
        }

        LinearLayout form = new LinearLayout(getContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        form.setPadding(padding, dp(8), padding, 0);

        TextView dateLabel = new TextView(getContext());
        dateLabel.setText(fullDateFormatter.format(date.getTime()));
        dateLabel.setTextColor(color(R.color.ink_700));
        dateLabel.setTextSize(14);
        dateLabel.setPadding(0, 0, 0, dp(8));
        form.addView(dateLabel);

        EditText nameInput = createDialogInput("Event name");
        EditText timeInput = createDialogInput("Time (HH:MM)");
        EditText descriptionInput = createDialogInput("Description");
        EditText locationInput = createDialogInput("Location");
        form.addView(nameInput);
        form.addView(timeInput);
        form.addView(descriptionInput);
        form.addView(locationInput);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Add Event")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (nameInput.getText().toString().trim().isEmpty()) {
                        Toast.makeText(getContext(), "Event name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveEvent(
                            date,
                            nameInput.getText().toString().trim(),
                            timeInput.getText().toString().trim(),
                            descriptionInput.getText().toString().trim(),
                            locationInput.getText().toString().trim());
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private EditText createDialogInput(String hint) {
        EditText input = new EditText(getContext());
        input.setHint(hint);
        input.setSingleLine(!"Description".equals(hint));
        input.setMinHeight(dp(48));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setTextColor(color(R.color.ink_900));
        input.setHintTextColor(color(R.color.ink_500));
        input.setBackgroundResource(R.drawable.bg_input);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        input.setLayoutParams(params);
        return input;
    }

    private void saveEvent(Calendar date, String name, String time, String description, String location) {
        String dateKey = storageFormatter.format(date.getTime());
        CalendarEvent event = new CalendarEvent(name, dateKey, time, description, location);
        new Thread(() -> {
            database.calendarEventDao().insert(event);
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Event added!", Toast.LENGTH_SHORT).show();
                loadEvents();
            });
        }).start();
    }

    private void updateModeButtons() {
        styleModeButton(monthViewButton, currentMode == CalendarMode.MONTHLY);
        styleModeButton(weekViewButton, currentMode == CalendarMode.WEEKLY);
        styleModeButton(yearViewButton, currentMode == CalendarMode.ANNUAL);
    }

    private void styleModeButton(Button button, boolean selected) {
        button.setTextColor(color(selected ? R.color.white : R.color.ink_700));
        button.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(8));
        drawable.setColor(color(selected ? R.color.blue_600 : R.color.white));
        drawable.setStroke(dp(1), color(selected ? R.color.blue_600 : R.color.blue_100));
        button.setBackground(drawable);
    }

    private Calendar firstVisibleDayForMonth(Calendar month) {
        Calendar firstDay = (Calendar) month.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        int offset = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        firstDay.add(Calendar.DATE, -offset);
        return firstDay;
    }

    private Calendar startOfWeek(Calendar date) {
        Calendar start = (Calendar) date.clone();
        int offset = start.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        start.add(Calendar.DATE, -offset);
        return start;
    }

    private Calendar monthDate(int year, int month) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month);
        date.set(Calendar.DAY_OF_MONTH, 1);
        return date;
    }

    private boolean isSameDay(Calendar firstDate, Calendar secondDate) {
        return firstDate.get(Calendar.YEAR) == secondDate.get(Calendar.YEAR)
                && firstDate.get(Calendar.DAY_OF_YEAR) == secondDate.get(Calendar.DAY_OF_YEAR);
    }

    private int color(int colorResource) {
        return ContextCompat.getColor(requireContext(), colorResource);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
