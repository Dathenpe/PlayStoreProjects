package com.f9ld3.Zion.ui.blog;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.f9ld3.Zion.R;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder> {

    private final List<Uri> mediaUris;

    public MediaPreviewAdapter(List<Uri> mediaUris) {
        this.mediaUris = mediaUris;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_preview, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        Uri uri = mediaUris.get(position);
        holder.bind(uri);
    }

    @Override
    public int getItemCount() {
        return mediaUris.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView previewImage;
        private final ImageView playIcon;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            previewImage = itemView.findViewById(R.id.preview_image);
            playIcon = itemView.findViewById(R.id.play_icon);
        }

        void bind(Uri uri) {
            Glide.with(itemView.getContext())
                    .load(uri)
                    .into(previewImage);

            String mediaType = itemView.getContext().getContentResolver().getType(uri);
            if (mediaType != null && mediaType.startsWith("video/")) {
                playIcon.setVisibility(View.VISIBLE);
            } else {
                playIcon.setVisibility(View.GONE);
            }
        }
    }
}