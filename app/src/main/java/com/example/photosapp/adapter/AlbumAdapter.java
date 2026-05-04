package com.example.photosapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photosapp.databinding.ItemAlbumBinding;
import com.example.photosapp.model.Album;

import java.util.ArrayList;
import java.util.List;

public final class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    public interface OnAlbumSelectedListener {
        void onAlbumSelected(Album album);
    }

    private final List<Album> albums = new ArrayList<>();
    private final OnAlbumSelectedListener listener;
    private String selectedAlbumId;

    public AlbumAdapter(OnAlbumSelectedListener listener) {
        this.listener = listener;
    }

    public void setAlbums(List<Album> updatedAlbums) {
        albums.clear();
        albums.addAll(updatedAlbums);
        if (selectedAlbumId != null && findById(selectedAlbumId) == null) {
            selectedAlbumId = null;
        }
        notifyDataSetChanged();
    }

    public String getSelectedAlbumId() {
        return selectedAlbumId;
    }

    public Album getSelectedAlbum() {
        return selectedAlbumId == null ? null : findById(selectedAlbumId);
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new AlbumViewHolder(ItemAlbumBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        holder.bind(albums.get(position));
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    private Album findById(String albumId) {
        for (Album album : albums) {
            if (album.getId().equals(albumId)) {
                return album;
            }
        }
        return null;
    }

    final class AlbumViewHolder extends RecyclerView.ViewHolder {

        private final ItemAlbumBinding binding;

        AlbumViewHolder(ItemAlbumBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Album album) {
            binding.textAlbumName.setText(album.getName());
            int count = album.getPhotoCount();
            binding.textAlbumCount.setText(count == 1 ? "1 photo" : count + " photos");
            binding.getRoot().setSelected(album.getId().equals(selectedAlbumId));
            binding.getRoot().setOnClickListener(view -> {
                selectedAlbumId = album.getId();
                notifyDataSetChanged();
                listener.onAlbumSelected(album);
            });
        }
    }
}

