package com.f9ld3.heal.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.f9ld3.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ui.HomeFragment;

public class NewJournalEntryActivity extends AppCompatActivity {

    private EditText journalEntryEditText;
    private Button saveJournalEntryButton;
    private Gson gson = new Gson();
    private static final String PREFS_JOURNAL = "journal_prefs";
    private static final String KEY_JOURNAL_ENTRIES = "journal_entries";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_journal_entry);

        journalEntryEditText = findViewById(R.id.journal_entry_edit_text);
        saveJournalEntryButton = findViewById(R.id.save_journal_entry_button);

        saveJournalEntryButton.setOnClickListener(v -> saveJournalEntry());
    }

    private void saveJournalEntry() {
        String journalText = journalEntryEditText.getText().toString().trim();
        if (journalText.isEmpty()) {
            Toast.makeText(this, "Please write something.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_JOURNAL, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_JOURNAL_ENTRIES, null);
        List<HomeFragment.JournalEntry> journalEntries;
        if (json != null) {
            Type type = new TypeToken<List<HomeFragment.JournalEntry>>() {}.getType();
            journalEntries = gson.fromJson(json, type);
        } else {
            journalEntries = new ArrayList<>();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        journalEntries.add(new HomeFragment.JournalEntry(timestamp, journalText, System.currentTimeMillis()));

        SharedPreferences.Editor editor = prefs.edit();
        String updatedJson = gson.toJson(journalEntries);
        editor.putString(KEY_JOURNAL_ENTRIES, updatedJson);
        editor.apply();

        Toast.makeText(this, "Journal entry saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}