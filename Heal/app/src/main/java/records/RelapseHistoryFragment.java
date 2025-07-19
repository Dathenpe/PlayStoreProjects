package records;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ui.HomeFragment;

public class RelapseHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private RelapseHistoryAdapter adapter;
    private List<HomeFragment.RelapseEntry> relapseHistory;
    private TextView emptyStateTextView;
    private Gson gson = new Gson();
    private MainActivity mainActivity;
    private Context context;

    private static final String PREFS_RELAPSE_HISTORY = "relapse_history_prefs";
    private static final String KEY_RELAPSE_ENTRIES = "relapse_entries";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: CopingExercisesFragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_relapse_history, container, false);

        recyclerView = view.findViewById(R.id.relapse_history_recycler_view);
        emptyStateTextView = view.findViewById(R.id.empty_relapse_history_text_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadRelapseHistory();

        adapter = new RelapseHistoryAdapter(relapseHistory);
        recyclerView.setAdapter(adapter);

        updateEmptyState();

        return view;
    }

    private void loadRelapseHistory() {
        if (getContext() == null) {
            relapseHistory = new ArrayList<>();
            return;
        }
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_RELAPSE_HISTORY, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RELAPSE_ENTRIES, null);
        if (json != null) {
            Type type = new TypeToken<List<HomeFragment.RelapseEntry>>() {}.getType();
            relapseHistory = gson.fromJson(json, type);
            if (relapseHistory == null) {
                relapseHistory = new ArrayList<>();
            }
        } else {
            relapseHistory = new ArrayList<>();
        }
    }

    private void updateEmptyState() {
        if (relapseHistory.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Adapter for the RecyclerView
    private static class RelapseHistoryAdapter extends RecyclerView.Adapter<RelapseHistoryAdapter.ViewHolder> {
        private final List<HomeFragment.RelapseEntry> entries;

        public RelapseHistoryAdapter(List<HomeFragment.RelapseEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_relapse_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HomeFragment.RelapseEntry entry = entries.get(position);
            holder.timestampTextView.setText("Reset on: " + entry.getTimestamp());
            holder.durationTextView.setText("Duration: " + entry.getDuration());
            holder.reasonTextView.setText("Reason: " + entry.getReason());
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView timestampTextView;
            TextView durationTextView;
            TextView reasonTextView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                timestampTextView = itemView.findViewById(R.id.relapse_timestamp_text_view);
                durationTextView = itemView.findViewById(R.id.relapse_duration_text_view);
                reasonTextView = itemView.findViewById(R.id.relapse_reason_text_view);
            }
        }
    }
    @Override
    public void onResume(){
        mainActivity.toolbar.setTitle("My Relapse History");
        mainActivity.navigationView.setCheckedItem(R.id.nav_records);
        mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
        mainActivity.Fab.setVisibility(View.VISIBLE);
        super.onResume();
    }
}