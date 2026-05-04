package com.example.photosapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photosapp.databinding.ItemTagBinding;
import com.example.photosapp.model.PhotoTag;

import java.util.ArrayList;
import java.util.List;

public final class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {

    private final List<PhotoTag> tags = new ArrayList<>();

    public void setTags(List<PhotoTag> updatedTags) {
        tags.clear();
        tags.addAll(updatedTags);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new TagViewHolder(ItemTagBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        holder.bind(tags.get(position));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    static final class TagViewHolder extends RecyclerView.ViewHolder {

        private final ItemTagBinding binding;

        TagViewHolder(ItemTagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PhotoTag tag) {
            binding.textTag.setText(tag.toString());
        }
    }
}

