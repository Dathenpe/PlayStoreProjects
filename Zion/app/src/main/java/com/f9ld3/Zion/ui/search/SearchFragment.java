package com.f9ld3.Zion.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.Zion.R;
import com.f9ld3.Zion.ui.player.PlayerMedia;
import com.f9ld3.Zion.ui.player.PlayerPostAdapter;
import com.google.android.material.tabs.TabLayout;

public class SearchFragment extends Fragment implements PlayerPostAdapter.OnMediaClickListener {

    // Manual view binding (no ViewBinding class needed)
    private TabLayout tabLayout;
    private EditText searchEditText;
    private ImageButton buttonClearSearch;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textPlaceholder;

    private SearchViewModel searchViewModel;
    private PlayerPostAdapter videoAdapter;
    private PlayerPostAdapter podcastAdapter;
    private SearchAllAdapter allAdapter;

    private static final int TAB_ALL = 0;
    private static final int TAB_VIDEOS = 1;
    private static final int TAB_PODCASTS = 2;
    private static final int TAB_USERS = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Corrected view initializations
        tabLayout = view.findViewById(R.id.tab_layout);
        searchEditText = view.findViewById(R.id.search_input_edit_text);
        buttonClearSearch = view.findViewById(R.id.button_clear_search);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        textPlaceholder = view.findViewById(R.id.text_placeholder);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        setupSearchBar();
        setupTabs();
        setupRecyclerView();
        setupObservers();
    }

    private void setupSearchBar() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    searchViewModel.searchAll(s.toString());
                } else if (s.length() == 0) {
                    searchViewModel.clearResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
            searchViewModel.clearResults();
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Videos"));
        tabLayout.addTab(tabLayout.newTab().setText("Podcasts"));
        tabLayout.addTab(tabLayout.newTab().setText("Users"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        videoAdapter = new PlayerPostAdapter(this);
        podcastAdapter = new PlayerPostAdapter(this);
        allAdapter = new SearchAllAdapter(this);

        recyclerView.setAdapter(allAdapter);
    }

    private void setupObservers() {
        searchViewModel.getAllResults().observe(getViewLifecycleOwner(), results -> {
            if (tabLayout.getSelectedTabPosition() == TAB_ALL) {
                allAdapter.submitList(results);
                updateEmptyState(results.isEmpty());
            }
        });

        searchViewModel.getVideoResults().observe(getViewLifecycleOwner(), videos -> {
            if (tabLayout.getSelectedTabPosition() == TAB_VIDEOS) {
                videoAdapter.submitList(videos);
                updateEmptyState(videos.isEmpty());
            }
        });

        searchViewModel.getPodcastResults().observe(getViewLifecycleOwner(), podcasts -> {
            if (tabLayout.getSelectedTabPosition() == TAB_PODCASTS) {
                podcastAdapter.submitList(podcasts);
                updateEmptyState(podcasts.isEmpty());
            }
        });

        searchViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        searchViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchTab(int position) {
        switch (position) {
            case TAB_ALL:
                recyclerView.setAdapter(allAdapter);
                allAdapter.submitList(searchViewModel.getAllResults().getValue());
                break;
            case TAB_VIDEOS:
                recyclerView.setAdapter(videoAdapter);
                videoAdapter.submitList(searchViewModel.getVideoResults().getValue());
                break;
            case TAB_PODCASTS:
                recyclerView.setAdapter(podcastAdapter);
                podcastAdapter.submitList(searchViewModel.getPodcastResults().getValue());
                break;
            case TAB_USERS:
                // TODO: Implement user adapter
                break;
        }

        updateEmptyState(recyclerView.getAdapter() != null && recyclerView.getAdapter().getItemCount() == 0);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (searchEditText.getText().toString().trim().isEmpty()) {
            textPlaceholder.setText("Start typing to search");
            textPlaceholder.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else if (isEmpty) {
            textPlaceholder.setText("No results found");
            textPlaceholder.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textPlaceholder.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMediaClick(PlayerMedia mediaItem) {
        Toast.makeText(getContext(), "Playing: " + mediaItem.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify views to prevent memory leaks
        tabLayout = null;
        searchEditText = null;
        buttonClearSearch = null;
        recyclerView = null;
        progressBar = null;
        textPlaceholder = null;
    }
}