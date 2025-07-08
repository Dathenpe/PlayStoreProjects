package ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.DisplayMetrics; // Import DisplayMetrics

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;
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

    private Button buttonNewCanvas;

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
        View galleryScrollView = view.findViewById(R.id.art_corner_coordinator_layout);

        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                galleryScrollView.setVisibility(View.GONE);
            } else {
                loadingProgressBar.setVisibility(View.GONE);
                galleryScrollView.setVisibility(View.VISIBLE);
            }
        });

        buttonNewCanvas = view.findViewById(R.id.buttonNewCanvas);
        recyclerViewArtwork = view.findViewById(R.id.recyclerViewArtwork);
        emptyStateArtworkTextView = view.findViewById(R.id.emptyStateArtworkTextView);

        artworkList = new ArrayList<>();
        // Pass the OnArtworkClickListener to the adapter
        artworkAdapter = new ArtworkAdapter(artworkList, this::showArtworkDetailsDialog);
        recyclerViewArtwork.setLayoutManager(new GridLayoutManager(context, 2));
        recyclerViewArtwork.setAdapter(artworkAdapter);

        buttonNewCanvas.setOnClickListener(v -> {
            DrawingCanvasFragment drawingCanvasFragment = new DrawingCanvasFragment();
            // Pass existing artwork names to the drawing fragment for unique name generation
            Bundle args = new Bundle();
            args.putSerializable("existingArtworkNames", (Serializable) getExistingArtworkNames());
            drawingCanvasFragment.setArguments(args);

            // Set this fragment as the target fragment for the DrawingCanvasFragment
            // This is how DrawingCanvasFragment will call onDrawingSaved on this fragment
            drawingCanvasFragment.setTargetFragment(this, 0);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, drawingCanvasFragment, "DrawingCanvasFragmentTag")
                    .addToBackStack(null)
                    .commit();
        });

        loadArtwork(); // Initial load of artwork
        updateEmptyStateVisibility();
    }

    /**
     * Retrieves a set of all current artwork names.
     * This is used by DrawingCanvasFragment to generate unique names.
     * @return A Set of strings, each representing an artwork name.
     */
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
        // Sort by creation timestamp in descending order (newest first)
        Collections.sort(artworkList, (e1, e2) -> Long.compare(e2.getCreationTimestampMillis(), e1.getCreationTimestampMillis()));

        Log.d(TAG, "Loaded " + artworkList.size() + " artwork entries from SharedPreferences.");
        for (ArtworkEntry entry : artworkList) {
            Log.d(TAG, "Artwork entry URI from SharedPreferences: " + entry.getImageUri() + ", Name: " + entry.getArtworkName());
            try {
                File file = new File(Uri.parse(entry.getImageUri()).getPath());
                if (!file.exists()) {
                    Log.w(TAG, "File does not exist for URI: " + entry.getImageUri() + ". Path: " + file.getAbsolutePath());
                } else {
                    Log.d(TAG, "File exists for URI: " + entry.getImageUri() + ". Path: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing URI or checking file existence for: " + entry.getImageUri(), e);
            }
        }

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

        // Check if this is an existing artwork being updated or a new one
        // We can identify an update by checking if the imageUri already exists in our list
        boolean updatedExisting = false;
        for (int i = 0; i < artworkList.size(); i++) {
            ArtworkEntry existingEntry = artworkList.get(i);
            if (existingEntry.getImageUri().equals(imageUri)) {
                // Update existing entry
                existingEntry.setArtworkName(artworkName);
                existingEntry.setTimestamp(timestamp); // Update timestamp as well
                existingEntry.setCreationTimestampMillis(System.currentTimeMillis()); // Update creation time
                updatedExisting = true;
                Toast.makeText(getContext(), "Artwork '" + artworkName + "' updated!", Toast.LENGTH_SHORT).show();
                break;
            }
        }

        if (!updatedExisting) {
            // Add as a new entry if not an update
            ArtworkEntry newEntry = new ArtworkEntry(imageUri, timestamp, System.currentTimeMillis(), artworkName);
            artworkList.add(newEntry);
            Toast.makeText(getContext(), "New artwork '" + artworkName + "' added to gallery!", Toast.LENGTH_SHORT).show();
        }

        saveArtworkList();
        loadArtwork(); // Reload to sort and refresh display
        updateEmptyStateVisibility();
    }

    /**
     * Displays a dialog with the selected artwork, and options to edit or delete it.
     * @param entry The ArtworkEntry to display.
     */
    private void showArtworkDetailsDialog(ArtworkEntry entry) {
        if (getContext() == null) return;

        // Use the custom TransparentDialog style for the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.TransparentDialog);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_artwork_view, null); // Use the custom layout
        builder.setView(dialogView);

        ImageView detailImageView = dialogView.findViewById(R.id.detailImageViewArtwork);
        TextView detailNameTextView = dialogView.findViewById(R.id.detailTextViewArtworkName);
        TextView detailTimestampTextView = dialogView.findViewById(R.id.detailTextViewArtworkTimestamp);
        Button buttonEdit = dialogView.findViewById(R.id.buttonEditArtwork);
        Button buttonDelete = dialogView.findViewById(R.id.buttonDeleteArtwork);

        // Load image using Glide
        RequestOptions requestOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Do not cache, always load fresh
                .skipMemoryCache(true) // Do not use memory cache
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image);

        Glide.with(getContext())
                .load(Uri.parse(entry.getImageUri())) // Load using the Uri object directly
                .apply(requestOptions)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Glide load failed for detail view: " + entry.getImageUri() + ", Exception: " + (e != null ? e.getMessage() : "null"), e);
                        return false; // Let Glide handle the error drawable
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Glide resource ready for detail view: " + entry.getImageUri());
                        return false; // Let Glide display the resource
                    }
                })
                .into(detailImageView);

        detailNameTextView.setText(entry.getArtworkName());
        detailTimestampTextView.setText(entry.getTimestamp());

        AlertDialog dialog = builder.create();

        // Set the dialog window's width to account for padding
        Window window = dialog.getWindow();
        if (window != null && getContext() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            // Get screen width
            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (getActivity() != null) {
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            } else {
                displayMetrics.widthPixels = getResources().getDisplayMetrics().widthPixels;
            }
            // Set dialog width to MATCH_PARENT, letting the XML layout's padding handle the margins
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            // Ensure no extra margins are applied by the window itself
            layoutParams.horizontalMargin = 0;

            window.setAttributes(layoutParams);
            // Ensure the background is transparent so the rounded corners of the card are visible
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
        if (window != null && getContext() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            DisplayMetrics displayMetrics = new DisplayMetrics();
            if (getActivity() != null) {
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            } else {
                displayMetrics.widthPixels = getResources().getDisplayMetrics().widthPixels;
            }

            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.horizontalMargin = 0;

            window.setAttributes(layoutParams);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.getDecorView().setPadding(0, 0, 0, 0); // 👈 Add this
        }

        dialog.show();
    }

    /**
     * Navigates to the DrawingCanvasFragment to edit the selected artwork.
     * @param entry The ArtworkEntry to be edited.
     */
    private void editArtwork(ArtworkEntry entry) {
        DrawingCanvasFragment drawingCanvasFragment = new DrawingCanvasFragment();
        Bundle args = new Bundle();
        args.putString("imageUriToLoad", entry.getImageUri()); // Pass the URI to load
        args.putString("artworkNameToLoad", entry.getArtworkName()); // Pass the name to load
        // Pass existing artwork names to the drawing fragment for unique name generation
        args.putSerializable("existingArtworkNames", (Serializable) getExistingArtworkNames());
        drawingCanvasFragment.setArguments(args);
        drawingCanvasFragment.setTargetFragment(this, 0); // Still use setTargetFragment for callback

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, drawingCanvasFragment, "DrawingCanvasFragmentTag")
                .addToBackStack(null)
                .commit();
    }

    /**
     * Shows a confirmation dialog before deleting an artwork.
     * @param entry The ArtworkEntry to be deleted.
     */
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

    /**
     * Deletes the artwork file from storage and removes its entry from the list.
     * @param entry The ArtworkEntry to delete.
     */
    private void deleteArtwork(ArtworkEntry entry) {
        if (getContext() == null) return;

        File fileToDelete = new File(Uri.parse(entry.getImageUri()).getPath());
        boolean deleted = false;
        if (fileToDelete.exists()) {
            deleted = fileToDelete.delete();
            if (deleted) {
                Log.d(TAG, "Successfully deleted file: " + fileToDelete.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to delete file: " + fileToDelete.getAbsolutePath());
            }
        } else {
            Log.w(TAG, "Attempted to delete non-existent file: " + fileToDelete.getAbsolutePath());
            deleted = true; // Consider it "deleted" if it doesn't exist
        }

        if (deleted) {
            artworkList.remove(entry);
            saveArtworkList();
            loadArtwork(); // Reload to refresh display
            updateEmptyStateVisibility();
            Toast.makeText(getContext(), "Artwork '" + entry.getArtworkName() + "' deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to delete artwork file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mainActivity != null) {
            mainActivity.MenuTrigger.setVisibility(View.VISIBLE);
            mainActivity.Fab.setVisibility(View.VISIBLE);
            mainActivity.shakeView(mainActivity.Fab);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Art Corner");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.invertShakeView(mainActivity.Fab);
        }
        loadArtwork(); // Reload artwork to ensure the list is up-to-date when returning to fragment
    }

    public static class ArtworkEntry implements Serializable { // Make ArtworkEntry Serializable
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

        public String getImageUri() {
            return imageUri;
        }

        public void setImageUri(String imageUri) {
            this.imageUri = imageUri;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public long getCreationTimestampMillis() {
            return creationTimestampMillis;
        }

        public void setCreationTimestampMillis(long creationTimestampMillis) {
            this.creationTimestampMillis = creationTimestampMillis;
        }

        public String getArtworkName() {
            return artworkName != null ? artworkName : "Untitled Artwork";
        }

        public void setArtworkName(String artworkName) {
            this.artworkName = artworkName;
        }
    }

    // Interface for handling artwork item clicks
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

            // Load image directly using the Uri object
            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Do not cache, always load fresh
                    .skipMemoryCache(true) // Do not use memory cache
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image);


            Glide.with(holder.imageView.getContext())
                    .load(Uri.parse(entry.getImageUri())) // Load using the Uri object directly
                    .apply(requestOptions)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Glide load failed for path: " + entry.getImageUri() + ", Exception: " + (e != null ? e.getMessage() : "null"), e);
                            return false; // Let Glide handle the error drawable
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d(TAG, "Glide resource ready for path: " + entry.getImageUri());
                            return false; // Let Glide display the resource
                        }
                    })
                    .into(holder.imageView);

            holder.timestampTextView.setText(entry.getTimestamp());
            holder.artworkNameTextView.setText(entry.getArtworkName());

            // Set the click listener for the item view
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
