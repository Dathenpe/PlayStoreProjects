package funcorner;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import drawing.DrawingCanvasFragment;
import viewmodels.GeneralViewModel;

public class ArtCornerFragment extends Fragment implements DrawingCanvasFragment.OnDrawingSavedListener {

    private static final String TAG = "ArtCornerFragment";

    private MainActivity mainActivity;
    private Context context;

    private RecyclerView recyclerViewArtwork;
    private ArtworkAdapter artworkAdapter;
    private List<ArtworkEntry> artworkList;
    private TextView emptyStateArtworkTextView;

    private FloatingActionButton buttonNewCanvas; // Changed to FloatingActionButton

    private static final String PREFS_ARTWORK = "artwork_prefs";
    private static final String KEY_ARTWORK_ENTRIES = "artwork_entries";
    private Gson gson = new Gson();


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        } else {
            Toast.makeText(context, "Error: Fragment attached to wrong activity", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_art_corner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Art Corner");
        }

        ProgressBar loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        // *** CORRECTED: Reference the content view that should be hidden/shown ***
        View galleryContentView = view.findViewById(R.id.gallery_content_view);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                galleryContentView.setVisibility(View.GONE); // Hide content
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                galleryContentView.setVisibility(View.VISIBLE); // Show content
            }
        });

        buttonNewCanvas = view.findViewById(R.id.buttonNewCanvas);
        recyclerViewArtwork = view.findViewById(R.id.recyclerViewArtwork);
        emptyStateArtworkTextView = view.findViewById(R.id.emptyStateArtworkTextView);

        artworkList = new ArrayList<>();
        artworkAdapter = new ArtworkAdapter(artworkList, this::showArtworkDetailsDialog);
        recyclerViewArtwork.setLayoutManager(new GridLayoutManager(context, 2));
        recyclerViewArtwork.setAdapter(artworkAdapter);

        buttonNewCanvas.setOnClickListener(v -> {
            DrawingCanvasFragment drawingCanvasFragment = new DrawingCanvasFragment();
            Bundle args = new Bundle();
            args.putSerializable("existingArtworkNames", (Serializable) getExistingArtworkNames());
            drawingCanvasFragment.setArguments(args);
            drawingCanvasFragment.setTargetFragment(this, 0);

            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.fragment_container, drawingCanvasFragment, "DrawingCanvasFragmentTag")
                    .addToBackStack(null)
                    .commit();
        });

        loadArtwork();
        updateEmptyStateVisibility();
    }

    private Set<String> getExistingArtworkNames() {
        Set<String> names = new HashSet<>();
        for (ArtworkEntry entry : artworkList) {
            names.add(entry.getArtworkName());
        }
        return names;
    }

    private void updateEmptyStateVisibility() {
        if (artworkList.isEmpty()) {
            emptyStateArtworkTextView.setVisibility(View.VISIBLE);
            recyclerViewArtwork.setVisibility(View.GONE);
        } else {
            emptyStateArtworkTextView.setVisibility(View.GONE);
            recyclerViewArtwork.setVisibility(View.VISIBLE);
        }
    }

    private void loadArtwork() {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_ARTWORK, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ARTWORK_ENTRIES, null);
        if (json != null) {
            Type type = new TypeToken<List<ArtworkEntry>>() {}.getType();
            artworkList = gson.fromJson(json, type);
            if (artworkList == null) {
                artworkList = new ArrayList<>();
            }
        } else {
            artworkList = new ArrayList<>();
        }
        Collections.sort(artworkList, (e1, e2) -> Long.compare(e2.getCreationTimestampMillis(), e1.getCreationTimestampMillis()));
        artworkAdapter.updateArtwork(artworkList);
        updateEmptyStateVisibility();
    }

    private void saveArtworkList() {
        if (getContext() == null) return;
        android.content.SharedPreferences prefs = getContext().getSharedPreferences(PREFS_ARTWORK, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(artworkList);
        editor.putString(KEY_ARTWORK_ENTRIES, json);
        editor.apply();
        Log.d(TAG, "Artwork list saved to SharedPreferences.");
    }

    @Override
    public void onDrawingSaved(String imageUri, String artworkName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        boolean updatedExisting = false;
        for (int i = 0; i < artworkList.size(); i++) {
            ArtworkEntry existingEntry = artworkList.get(i);
            if (existingEntry.getImageUri().equals(imageUri)) {
                existingEntry.setArtworkName(artworkName);
                existingEntry.setTimestamp(timestamp);
                existingEntry.setCreationTimestampMillis(System.currentTimeMillis());
                updatedExisting = true;
                Toast.makeText(getContext(), "Artwork '" + artworkName + "' updated!", Toast.LENGTH_SHORT).show();
                break;
            }
        }

        if (!updatedExisting) {
            ArtworkEntry newEntry = new ArtworkEntry(imageUri, timestamp, System.currentTimeMillis(), artworkName);
            artworkList.add(newEntry);
            Toast.makeText(getContext(), "New artwork '" + artworkName + "' added to gallery!", Toast.LENGTH_SHORT).show();
        }

        saveArtworkList();
        loadArtwork();
        updateEmptyStateVisibility();
    }

    private void showArtworkDetailsDialog(ArtworkEntry entry) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.TransparentDialog);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_artwork_view, null);
        builder.setView(dialogView);

        ImageView detailImageView = dialogView.findViewById(R.id.detailImageViewArtwork);
        TextView detailNameTextView = dialogView.findViewById(R.id.detailTextViewArtworkName);
        TextView detailTimestampTextView = dialogView.findViewById(R.id.detailTextViewArtworkTimestamp);
        Button buttonEdit = dialogView.findViewById(R.id.buttonEditArtwork);
        Button buttonDelete = dialogView.findViewById(R.id.buttonDeleteArtwork);

        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image);

        Glide.with(getContext())
                .load(Uri.parse(entry.getImageUri()))
                .apply(requestOptions)
                .into(detailImageView);

        detailNameTextView.setText(entry.getArtworkName());
        detailTimestampTextView.setText(entry.getTimestamp());

        AlertDialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        buttonEdit.setOnClickListener(v -> {
            dialog.dismiss();
            editArtwork(entry);
        });

        buttonDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmAndDeleteArtwork(entry);
        });

        dialog.show();
    }

    private void editArtwork(ArtworkEntry entry) {
        DrawingCanvasFragment drawingCanvasFragment = new DrawingCanvasFragment();
        Bundle args = new Bundle();
        args.putString("imageUriToLoad", entry.getImageUri());
        args.putString("artworkNameToLoad", entry.getArtworkName());
        args.putSerializable("existingArtworkNames", (Serializable) getExistingArtworkNames());
        drawingCanvasFragment.setArguments(args);
        drawingCanvasFragment.setTargetFragment(this, 0);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, drawingCanvasFragment, "DrawingCanvasFragmentTag")
                .addToBackStack(null)
                .commit();
    }

    private void confirmAndDeleteArtwork(ArtworkEntry entry) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Artwork")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage("Are you sure you want to delete '" + entry.getArtworkName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteArtwork(entry))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteArtwork(ArtworkEntry entry) {
        if (getContext() == null) return;

        File fileToDelete = new File(Uri.parse(entry.getImageUri()).getPath());
        boolean deleted = false;
        if (fileToDelete.exists()) {
            deleted = fileToDelete.delete();
        } else {
            Log.w(TAG, "Attempted to delete non-existent file: " + fileToDelete.getAbsolutePath());
            deleted = true;
        }

        if (deleted) {
            artworkList.remove(entry);
            saveArtworkList();
            loadArtwork();
            updateEmptyStateVisibility();
            Toast.makeText(getContext(), "Artwork '" + entry.getArtworkName() + "' deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to delete artwork file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Art Corner");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
        loadArtwork();
    }

    public static class ArtworkEntry implements Serializable {
        private String imageUri;
        private String timestamp;
        private long creationTimestampMillis;
        private String artworkName;

        public ArtworkEntry(String imageUri, String timestamp, long creationTimestampMillis, String artworkName) {
            this.imageUri = imageUri;
            this.timestamp = timestamp;
            this.creationTimestampMillis = creationTimestampMillis;
            this.artworkName = artworkName;
        }

        public String getImageUri() { return imageUri; }
        public void setImageUri(String imageUri) { this.imageUri = imageUri; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public long getCreationTimestampMillis() { return creationTimestampMillis; }
        public void setCreationTimestampMillis(long creationTimestampMillis) { this.creationTimestampMillis = creationTimestampMillis; }
        public String getArtworkName() { return artworkName != null ? artworkName : "Untitled Artwork"; }
        public void setArtworkName(String artworkName) { this.artworkName = artworkName; }
    }

    public interface OnArtworkClickListener {
        void onArtworkClick(ArtworkEntry entry);
    }

    private class ArtworkAdapter extends RecyclerView.Adapter<ArtworkAdapter.ArtworkViewHolder> {
        private List<ArtworkEntry> localArtworkList;
        private OnArtworkClickListener clickListener;

        public ArtworkAdapter(List<ArtworkEntry> artworkList, OnArtworkClickListener clickListener) {
            this.localArtworkList = artworkList;
            this.clickListener = clickListener;
        }

        public void updateArtwork(List<ArtworkEntry> newArtworkList) {
            this.localArtworkList = newArtworkList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ArtworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artwork_grid, parent, false);
            return new ArtworkViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ArtworkViewHolder holder, int position) {
            ArtworkEntry entry = localArtworkList.get(position);

            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image);

            Glide.with(holder.imageView.getContext())
                    .load(Uri.parse(entry.getImageUri()))
                    .apply(requestOptions)
                    .into(holder.imageView);

            holder.timestampTextView.setText(entry.getTimestamp());
            holder.artworkNameTextView.setText(entry.getArtworkName());

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onArtworkClick(entry);
                }
            });
        }

        @Override
        public int getItemCount() {
            return localArtworkList.size();
        }

        public class ArtworkViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView timestampTextView;
            public TextView artworkNameTextView;

            public ArtworkViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageViewArtwork);
                timestampTextView = itemView.findViewById(R.id.textViewArtworkTimestamp);
                artworkNameTextView = itemView.findViewById(R.id.textViewArtworkName);
            }
        }
    }
}
