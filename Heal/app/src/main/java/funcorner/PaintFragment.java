package funcorner;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
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
import ui.CustomMessageDialogFragment;
import viewmodels.GeneralViewModel;

public class PaintFragment extends Fragment implements DrawingCanvasFragment.OnDrawingSavedListener {

    private static final String TAG = "PaintFragment";

    private MainActivity mainActivity;
    private RecyclerView recyclerViewArtwork;
    private ArtworkAdapter artworkAdapter;
    private List<ArtworkEntry> artworkList;
    private TextView emptyStateArtworkTextView;
    private FloatingActionButton buttonNewCanvas;
    private Gson gson = new Gson();

    private static final String PREFS_ARTWORK = "artwork_prefs";
    private static final String KEY_ARTWORK_ENTRIES = "artwork_entries";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_art_corner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();
        setupViewModel(view);
        buttonNewCanvas.setOnClickListener(v -> openDrawingCanvas(null));
        loadArtwork();
    }

    private void initializeViews(View view) {
        recyclerViewArtwork = view.findViewById(R.id.recyclerViewArtwork);
        emptyStateArtworkTextView = view.findViewById(R.id.emptyStateArtworkTextView);
        buttonNewCanvas = view.findViewById(R.id.buttonNewCanvas);
    }

    private void setupRecyclerView() {
        artworkList = new ArrayList<>();
        artworkAdapter = new ArtworkAdapter(artworkList, this::showArtworkDetailsDialog);
        recyclerViewArtwork.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerViewArtwork.setAdapter(artworkAdapter);
    }

    private void setupViewModel(View view) {
        ProgressBar loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        View galleryContentView = view.findViewById(R.id.gallery_content_view);
        GeneralViewModel viewModel = new ViewModelProvider(this).get(GeneralViewModel.class);
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            galleryContentView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            buttonNewCanvas.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
    }

    private void openDrawingCanvas(@Nullable ArtworkEntry entry) {
        DrawingCanvasFragment drawingCanvasFragment = new DrawingCanvasFragment();
        Bundle args = new Bundle();
        args.putSerializable("existingArtworkNames", (Serializable) getExistingArtworkNames());
        if (entry != null) {
            args.putString("imageUriToLoad", entry.getImageUri());
            args.putString("artworkNameToLoad", entry.getArtworkName());
        }
        drawingCanvasFragment.setArguments(args);
        drawingCanvasFragment.setTargetFragment(this, 0);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, drawingCanvasFragment, "DrawingCanvasFragmentTag")
                .addToBackStack(null)
                .commit();
    }

    private Set<String> getExistingArtworkNames() {
        Set<String> names = new HashSet<>();
        for (ArtworkEntry entry : artworkList) {
            names.add(entry.getArtworkName());
        }
        return names;
    }

    private void updateEmptyStateVisibility() {
        boolean isEmpty = artworkList.isEmpty();
        emptyStateArtworkTextView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerViewArtwork.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void loadArtwork() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_ARTWORK, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ARTWORK_ENTRIES, null);
        Type type = new TypeToken<List<ArtworkEntry>>() {}.getType();
        artworkList = gson.fromJson(json, type);
        if (artworkList == null) artworkList = new ArrayList<>();
        Collections.sort(artworkList, (e1, e2) -> Long.compare(e2.getCreationTimestampMillis(), e1.getCreationTimestampMillis()));
        artworkAdapter.updateArtwork(artworkList);
        updateEmptyStateVisibility();
    }

    private void saveArtworkList() {
        if (getContext() == null) return;
        SharedPreferences.Editor editor = getContext().getSharedPreferences(PREFS_ARTWORK, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_ARTWORK_ENTRIES, gson.toJson(artworkList)).apply();
    }

    @Override
    public void onDrawingSaved(String imageUri, String artworkName) {
        boolean updated = false;
        for (ArtworkEntry entry : artworkList) {
            if (entry.getImageUri().equals(imageUri)) {
                entry.setArtworkName(artworkName);
                entry.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                entry.setCreationTimestampMillis(System.currentTimeMillis());
                updated = true;
                break;
            }
        }
        if (!updated) {
            artworkList.add(new ArtworkEntry(imageUri, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), System.currentTimeMillis(), artworkName));
        }
        saveArtworkList();
        loadArtwork();
    }

    private void showArtworkDetailsDialog(ArtworkEntry entry) {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.TransparentDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_artwork_view, null);
        builder.setView(dialogView);

        ImageView detailImageView = dialogView.findViewById(R.id.detailImageViewArtwork);
        TextView detailNameTextView = dialogView.findViewById(R.id.detailTextViewArtworkName);
        TextView detailTimestampTextView = dialogView.findViewById(R.id.detailTextViewArtworkTimestamp);
        Button buttonEdit = dialogView.findViewById(R.id.buttonEditArtwork);
        Button buttonDelete = dialogView.findViewById(R.id.buttonDeleteArtwork);

        Glide.with(getContext()).load(Uri.parse(entry.getImageUri()))
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                .into(detailImageView);
        detailNameTextView.setText(entry.getArtworkName());
        detailTimestampTextView.setText(entry.getTimestamp());

        AlertDialog dialog = builder.create();
        buttonEdit.setOnClickListener(v -> {
            dialog.dismiss();
            openDrawingCanvas(entry);
        });
        buttonDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmAndDeleteArtwork(entry);
        });
        dialog.show();
    }

    private void confirmAndDeleteArtwork(ArtworkEntry entry) {
        if (getContext() == null) return;
        CustomMessageDialogFragment dialog = CustomMessageDialogFragment.newInstance(
                "Delete Artwork",
                "Are you sure you want to delete '" + entry.getArtworkName() + "'? This cannot be undone.",
                "Delete",
                "Cancel"
        );
        dialog.setListener(new CustomMessageDialogFragment.OnMessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialogFragment) {
                deleteArtwork(entry);
            }
            @Override
            public void onDialogNegativeClick(DialogFragment dialogFragment) {
                dialogFragment.dismiss();
            }
        });
        dialog.show(getParentFragmentManager(), "DeleteArtworkConfirmation");
    }

    private void deleteArtwork(ArtworkEntry entry) {
        if (getContext() == null) return;
        File fileToDelete = new File(Uri.parse(entry.getImageUri()).getPath());
        if (fileToDelete.exists() && fileToDelete.delete()) {
            artworkList.remove(entry);
            saveArtworkList();
            loadArtwork();
            Toast.makeText(getContext(), "Artwork deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Failed to delete artwork file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainActivity != null) {
            mainActivity.toolbar.setTitle("Paint");
            mainActivity.MenuTrigger.setVisibility(View.GONE);
            mainActivity.Fab.setVisibility(View.GONE);
        }
        loadArtwork();
    }

    public static class ArtworkEntry implements Serializable {
        private String imageUri, timestamp, artworkName;
        private long creationTimestampMillis;
        public ArtworkEntry(String imageUri, String timestamp, long creationTimestampMillis, String artworkName) {
            this.imageUri = imageUri;
            this.timestamp = timestamp;
            this.creationTimestampMillis = creationTimestampMillis;
            this.artworkName = artworkName;
        }
        public String getImageUri() { return imageUri; }
        public String getTimestamp() { return timestamp; }
        public long getCreationTimestampMillis() { return creationTimestampMillis; }
        public String getArtworkName() { return artworkName; }
        public void setArtworkName(String artworkName) { this.artworkName = artworkName; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public void setCreationTimestampMillis(long creationTimestampMillis) { this.creationTimestampMillis = creationTimestampMillis; }
    }

    public interface OnArtworkClickListener { void onArtworkClick(ArtworkEntry entry); }

    private class ArtworkAdapter extends RecyclerView.Adapter<ArtworkAdapter.ArtworkViewHolder> {
        private List<ArtworkEntry> localArtworkList;
        private OnArtworkClickListener clickListener;
        ArtworkAdapter(List<ArtworkEntry> list, OnArtworkClickListener listener) {
            this.localArtworkList = list;
            this.clickListener = listener;
        }
        void updateArtwork(List<ArtworkEntry> newList) {
            this.localArtworkList = newList;
            notifyDataSetChanged();
        }
        @NonNull @Override
        public ArtworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ArtworkViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artwork_grid, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull ArtworkViewHolder holder, int position) {
            ArtworkEntry entry = localArtworkList.get(position);
            Glide.with(holder.imageView.getContext()).load(Uri.parse(entry.getImageUri()))
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true))
                    .into(holder.imageView);
            holder.timestampTextView.setText(entry.getTimestamp());
            holder.artworkNameTextView.setText(entry.getArtworkName());
            holder.itemView.setOnClickListener(v -> clickListener.onArtworkClick(entry));
        }
        @Override public int getItemCount() { return localArtworkList.size(); }
        class ArtworkViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            TextView timestampTextView, artworkNameTextView;
            ArtworkViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageViewArtwork);
                timestampTextView = itemView.findViewById(R.id.textViewArtworkTimestamp);
                artworkNameTextView = itemView.findViewById(R.id.textViewArtworkName);
            }
        }
    }
}
