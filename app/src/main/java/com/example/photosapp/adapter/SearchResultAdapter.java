package com.example.photosapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photosapp.databinding.ItemSearchResultBinding;
import com.example.photosapp.model.PhotoTag;
import com.example.photosapp.model.SearchMatch;
import com.example.photosapp.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public final class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    public interface OnSearchResultClickListener {
        void onSearchResultClicked(SearchMatch match);
    }

    private final List<SearchMatch> matches = new ArrayList<>();
    private final OnSearchResultClickListener listener;

    public SearchResultAdapter(OnSearchResultClickListener listener) {
        this.listener = listener;
    }

    public void setMatches(List<SearchMatch> updatedMatches) {
        matches.clear();
        matches.addAll(updatedMatches);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new SearchResultViewHolder(ItemSearchResultBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        holder.bind(matches.get(position));
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    final class SearchResultViewHolder extends RecyclerView.ViewHolder {

        private final ItemSearchResultBinding binding;

        SearchResultViewHolder(ItemSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SearchMatch match) {
            binding.textResultName.setText(match.getPhoto().getDisplayName());
            binding.textResultAlbum.setText("Album: " + match.getAlbumName());
            binding.textResultTags.setText(buildTagSummary(match));
            binding.imageResultThumb.setImageBitmap(
                    ImageLoader.loadThumbnail(binding.getRoot().getContext(), match.getPhoto().getUriString(), 220)
            );
            binding.getRoot().setOnClickListener(view -> listener.onSearchResultClicked(match));
        }

        private String buildTagSummary(SearchMatch match) {
            List<PhotoTag> tags = match.getPhoto().getTags();
            if (tags.isEmpty()) {
                return "Tags: none";
            }
            StringBuilder builder = new StringBuilder("Tags: ");
            for (int i = 0; i < tags.size(); i++) {
                builder.append(tags.get(i));
                if (i < tags.size() - 1) {
                    builder.append("  |  ");
                }
            }
            return builder.toString();
        }
    }
}

