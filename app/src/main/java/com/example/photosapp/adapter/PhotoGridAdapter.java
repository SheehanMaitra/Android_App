package com.example.photosapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photosapp.R;
import com.example.photosapp.databinding.ItemPhotoGridBinding;
import com.example.photosapp.model.PhotoItem;
import com.example.photosapp.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public final class PhotoGridAdapter extends RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder> {

    public interface PhotoActionListener {
        void onPhotoOpen(PhotoItem photo);
        void onPhotoMove(PhotoItem photo);
        void onPhotoRemove(PhotoItem photo);
    }

    private final List<PhotoItem> photos = new ArrayList<>();
    private final PhotoActionListener listener;

    public PhotoGridAdapter(PhotoActionListener listener) {
        this.listener = listener;
    }

    public void setPhotos(List<PhotoItem> updatedPhotos) {
        photos.clear();
        photos.addAll(updatedPhotos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new PhotoViewHolder(ItemPhotoGridBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(photos.get(position));
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    final class PhotoViewHolder extends RecyclerView.ViewHolder {

        private final ItemPhotoGridBinding binding;

        PhotoViewHolder(ItemPhotoGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PhotoItem photo) {
            binding.textPhotoName.setText(photo.getDisplayName());
            binding.imageThumbnail.setImageBitmap(
                    ImageLoader.loadThumbnail(binding.getRoot().getContext(), photo.getUriString(), 320)
            );
            binding.getRoot().setOnClickListener(view -> listener.onPhotoOpen(photo));
            binding.buttonPhotoMenu.setOnClickListener(view -> {
                PopupMenu menu = new PopupMenu(view.getContext(), view);
                menu.inflate(R.menu.menu_photo_actions);
                menu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_view_photo) {
                        listener.onPhotoOpen(photo);
                        return true;
                    }
                    if (itemId == R.id.action_move_photo) {
                        listener.onPhotoMove(photo);
                        return true;
                    }
                    if (itemId == R.id.action_remove_photo) {
                        listener.onPhotoRemove(photo);
                        return true;
                    }
                    return false;
                });
                menu.show();
            });
        }
    }
}

