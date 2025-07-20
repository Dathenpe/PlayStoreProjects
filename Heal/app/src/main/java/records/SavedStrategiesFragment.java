package records; // Adjust package as necessary

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;

import java.util.ArrayList;
import java.util.List;

import ui.CustomInputDialogFragment;
import ui.CustomMessageDialogFragment;

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
            emptyStateTextView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
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
            // Replaced AlertDialog with CustomMessageDialogFragment
            CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                    "Delete Strategy",
                    "Are you sure you want to delete this strategy? This action cannot be undone.",
                    "Yes",
                    "No"
            );
            dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
                @Override
                public void onDialogPositiveClick(DialogFragment dialogFragment) {
                    allSavedStrategies.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveAllStrategiesToSharedPreferences();
                    Toast.makeText(getContext(), "Strategy deleted!", Toast.LENGTH_SHORT).show();
                    dialogFragment.dismiss();
                }

                @Override
                public void onDialogNegativeClick(DialogFragment dialogFragment) {
                    dialogFragment.dismiss();
                }
            });
            dialog.show(getParentFragmentManager(), "DeleteStrategyDialog");
        }
    }

    @Override
    public void onEditStrategy(int position, String currentStrategy) {
        if (position != RecyclerView.NO_POSITION && position < allSavedStrategies.size()) {
            showEditStrategyDialog(position, currentStrategy);
        }
    }

    private void showAddStrategyDialog() {
        // Replaced AlertDialog with CustomInputDialogFragment
        CustomInputDialogFragment dialog = CustomInputDialogFragment.newInstance(
                "Add New Strategy",
                "Enter your new coping strategy.",
                "New Strategy", // Hint for the input field
                "Add",
                "Cancel"
        );

        dialog.setListener(new CustomInputDialogFragment.OnInputDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment, String inputText) {
                String newStrategy = inputText.trim();
                if (!newStrategy.isEmpty()) {
                    if (!allSavedStrategies.contains(newStrategy)) {
                        allSavedStrategies.add(newStrategy);
                        saveAllStrategiesToSharedPreferences();
                        adapter.notifyItemInserted(allSavedStrategies.size() - 1);
                        Toast.makeText(getContext(), "Strategy added!", Toast.LENGTH_SHORT).show();
                        recyclerView.scrollToPosition(allSavedStrategies.size() - 1);
                    } else {
                        Toast.makeText(mainActivity, "Strategy already exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Strategy cannot be empty.", Toast.LENGTH_SHORT).show();
                }
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "AddStrategyDialog");
    }

    private void showEditStrategyDialog(int position, String currentStrategy) {
        // Replaced AlertDialog with CustomInputDialogFragment
        CustomInputDialogFragment dialog = CustomInputDialogFragment.newInstance(
                "Edit Strategy",
                "Edit your coping strategy.",
                currentStrategy, // Pre-fill with current strategy
                "Save",
                "Cancel"
        );

        dialog.setListener(new CustomInputDialogFragment.OnInputDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment, String inputText) {
                String updatedStrategy = inputText.trim();
                if (!updatedStrategy.isEmpty()) {
                    if (!allSavedStrategies.contains(updatedStrategy) || allSavedStrategies.get(position).equals(updatedStrategy)) {
                        allSavedStrategies.set(position, updatedStrategy);
                        saveAllStrategiesToSharedPreferences();
                        adapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), "Strategy updated!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mainActivity, "Strategy already exists", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Strategy cannot be empty.", Toast.LENGTH_SHORT).show();
                }
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "EditStrategyDialog");
    }


    @Override
    public void onResume() {
        mainActivity.toolbar.setTitle("My Coping Strategies");
        mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
        mainActivity.Fab.setVisibility(View.VISIBLE);
        super.onResume();
        loadAllSavedStrategies();
    }

    @Override
    public void onStrategyClick(String strategyText) {
        // Replaced AlertDialog with CustomMessageDialogFragment
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Coping Strategy Details",
                strategyText,
                "Close",
                null // No negative button needed for a simple close
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }

            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                // This won't be called as negative button is null
            }
        });
        dialog.show(getParentFragmentManager(), "StrategyDetailsDialog");
    }
}
