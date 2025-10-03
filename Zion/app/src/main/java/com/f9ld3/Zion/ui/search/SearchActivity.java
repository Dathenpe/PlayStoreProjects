package com.f9ld3.Zion.ui.search;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.databinding.ActivityFragmentHostBinding; // Reusing generic host layout

public class SearchActivity extends AppCompatActivity {

    public static final String EXTRA_QUERY = "extra_search_query";
    private ActivityFragmentHostBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFragmentHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.search_hint); // Set toolbar title to search hint
        }

        String query = getIntent().getStringExtra(EXTRA_QUERY);
        if (query != null && !query.isEmpty()) {
            Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
            // TODO: Pass query to a SearchFragment or directly perform search
        } else {
            Toast.makeText(this, "Search activity opened. Enter a query.", Toast.LENGTH_SHORT).show();
        }

        // Replace the fragment_container with a SearchFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SearchFragment()) // Assuming you create a SearchFragment
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}