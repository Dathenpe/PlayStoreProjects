// records/SavedStrategiesFragment.java
package records; // Adjust package as necessary

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button
import android.widget.EditText; // Import EditText
import android.widget.TextView; // Import TextView for empty state
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heal.R; // Adjust your package name
import com.example.heal.MainActivity; // Import MainActivity if needed

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SavedStrategiesFragment extends Fragment
        implements StrategiesAdapter.OnStrategyDeleteListener, StrategiesAdapter.OnStrategyEditListener,StrategiesAdapter.OnStrategyClickListener { // Implement new interface

    private RecyclerView recyclerView;
    private StrategiesAdapter adapter;
    private List<String> allSavedStrategies = new ArrayList<>();
    private MainActivity mainActivity; // Reference to MainActivity
    private Button addStrategyButton; // Reference to the Add button
    private TextView emptyStateTextView; // Reference to the empty state TextView

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_strategies, container, false);

        if (mainActivity != null) {
            mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        }

        recyclerView = view.findViewById(R.id.recyclerViewStrategies);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize adapter with both listeners
        adapter = new StrategiesAdapter(getContext(), allSavedStrategies, this, this,this);
        recyclerView.setAdapter(adapter);

        addStrategyButton = view.findViewById(R.id.addStrategyButton);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);

        addStrategyButton.setOnClickListener(v -> showAddStrategyDialog());

        loadAllSavedStrategies();
        return view;
    }

    private void loadAllSavedStrategies() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("coping_strategies", Context.MODE_PRIVATE);
        String savedStrategies = sharedPreferences.getString("strategies", "");

        allSavedStrategies.clear();
        if (!savedStrategies.isEmpty()) {
            String[] strategiesArray = savedStrategies.split(",");
            for (String strategy : strategiesArray) {
                if (!strategy.trim().isEmpty()) {
                    allSavedStrategies.add(strategy.trim());
                }
            }
        }
        adapter.updateStrategies(allSavedStrategies);
        updateEmptyStateVisibility();
    }

    private void saveAllStrategiesToSharedPreferences() {
        if (getContext() == null) return;
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("coping_strategies", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("strategies", TextUtils.join(",", allSavedStrategies));
        editor.apply();
        updateEmptyStateVisibility();
    }

    private void updateEmptyStateVisibility() {
        if (allSavedStrategies.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDeleteStrategy(int position) {
        if (position != RecyclerView.NO_POSITION && position < allSavedStrategies.size()) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Strategy")
                    .setMessage("Are you sure you want to delete this strategy?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        allSavedStrategies.remove(position);
                        adapter.notifyItemRemoved(position);
                        saveAllStrategiesToSharedPreferences();
                        Toast.makeText(getContext(), "Strategy deleted!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public void onEditStrategy(int position, String currentStrategy) {
        if (position != RecyclerView.NO_POSITION && position < allSavedStrategies.size()) {
            showEditStrategyDialog(position, currentStrategy);
        }
    }

    private void showAddStrategyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Strategy");

        final EditText input = new EditText(getContext());
        input.setHint("Enter your new coping strategy");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newStrategy = input.getText().toString().trim();
            if (!newStrategy.isEmpty()) {
                if (!allSavedStrategies.contains(newStrategy)){
                    allSavedStrategies.add(newStrategy);
                    saveAllStrategiesToSharedPreferences();
                    adapter.notifyItemInserted(allSavedStrategies.size() - 1);
                    Toast.makeText(getContext(), "Strategy added!", Toast.LENGTH_SHORT).show();
                    recyclerView.scrollToPosition(allSavedStrategies.size() - 1);
                }else {
                    Toast.makeText(mainActivity, "Strategy already exists", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(getContext(), "Strategy cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEditStrategyDialog(int position, String currentStrategy) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Edit Strategy");

        final EditText input = new EditText(getContext());
        input.setText(currentStrategy);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String updatedStrategy = input.getText().toString().trim();
            if (!updatedStrategy.isEmpty()) {
                allSavedStrategies.set(position, updatedStrategy);
                saveAllStrategiesToSharedPreferences();
                adapter.notifyItemChanged(position);
                Toast.makeText(getContext(), "Strategy updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Strategy cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    @Override
    public void onResume() {
        super.onResume();
        loadAllSavedStrategies();
    }

    @Override
    public void onStrategyClick(String strategyText) {
        new AlertDialog.Builder(getContext())
                .setTitle("Coping Strategy Details")
                .setMessage(strategyText)
                .setPositiveButton("Close", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}
