package com.f9ld3.Zion.ui.search;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

        // The toolbar from activity_fragment_host.xml is gone, so we don't set it here.
        // The SearchFragment will be responsible for its own toolbar.

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SearchFragment()) // Assuming you create a SearchFragment
                    .commit();
        }

        // You can still handle the incoming search query
        String query = getIntent().getStringExtra(EXTRA_QUERY);
        if (query != null && !query.isEmpty()) {
            Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();
        }
    }

    // This will handle the back arrow from the fragment's toolbar if it's set up correctly.
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}