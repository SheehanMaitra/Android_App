package com.example.photosapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.photosapp.adapter.TagAdapter;
import com.example.photosapp.data.LibraryRepository;
import com.example.photosapp.databinding.ActivityPhotoViewerBinding;
import com.example.photosapp.databinding.DialogAddTagBinding;
import com.example.photosapp.model.Album;
import com.example.photosapp.model.PhotoItem;
import com.example.photosapp.model.PhotoTag;
import com.example.photosapp.model.TagType;
import com.example.photosapp.util.ImageLoader;
import com.example.photosapp.util.IntentExtras;

import java.util.ArrayList;
import java.util.List;

public final class PhotoViewerActivity extends AppCompatActivity {

    private ActivityPhotoViewerBinding binding;
    private LibraryRepository repository;
    private TagAdapter tagAdapter;
    private String albumId;
    private int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotoViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        albumId = getIntent().getStringExtra(IntentExtras.ALBUM_ID);
        currentIndex = getIntent().getIntExtra(IntentExtras.PHOTO_INDEX, -1);
        if (albumId == null) {
            finish();
            return;
        }

        repository = LibraryRepository.getInstance(this);
        tagAdapter = new TagAdapter();

        binding.recyclerTags.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTags.setAdapter(tagAdapter);

        binding.buttonPrev.setOnClickListener(view -> showPhotoAt(currentIndex - 1));
        binding.buttonNext.setOnClickListener(view -> showPhotoAt(currentIndex + 1));
        binding.buttonAddTag.setOnClickListener(view -> showAddTagDialog());
        binding.buttonDeleteTag.setOnClickListener(view -> showDeleteTagDialog());
        binding.buttonBackToAlbum.setOnClickListener(view -> finish());

        if (currentIndex < 0) {
            String photoId = getIntent().getStringExtra(IntentExtras.PHOTO_ID);
            if (photoId != null) {
                currentIndex = repository.getPhotoIndex(albumId, photoId);
            }
        }

        showPhotoAt(currentIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showPhotoAt(currentIndex);
    }

    private void showPhotoAt(int index) {
        try {
            Album album = repository.getAlbum(albumId);
            List<PhotoItem> photos = album.getPhotos();
            if (photos.isEmpty()) {
                finish();
                return;
            }
            currentIndex = Math.max(0, Math.min(index, photos.size() - 1));
            PhotoItem photo = photos.get(currentIndex);
            binding.textViewerAlbum.setText(album.getName());
            binding.textViewerName.setText(photo.getDisplayName());
            binding.textViewerCounter.setText(getString(
                    R.string.photo_position_label,
                    currentIndex + 1,
                    photos.size()
            ));
            binding.recyclerTags.setVisibility(photo.getTags().isEmpty()
                    ? android.view.View.GONE
                    : android.view.View.VISIBLE);
            binding.textNoTags.setVisibility(photo.getTags().isEmpty()
                    ? android.view.View.VISIBLE
                    : android.view.View.GONE);
            tagAdapter.setTags(new ArrayList<>(photo.getTags()));

            binding.imagePhoto.post(() -> binding.imagePhoto.setImageBitmap(
                    ImageLoader.loadFullImage(
                            this,
                            photo.getUriString(),
                            Math.max(binding.imagePhoto.getWidth(), 900),
                            Math.max(binding.imagePhoto.getHeight(), 900)
                    )
            ));

            binding.buttonPrev.setEnabled(currentIndex > 0);
            binding.buttonNext.setEnabled(currentIndex < photos.size() - 1);
        } catch (RuntimeException exception) {
            showMessage(exception.getMessage());
            finish();
        }
    }

    private void showAddTagDialog() {
        PhotoItem photo = getCurrentPhoto();
        if (photo == null) {
            return;
        }

        DialogAddTagBinding dialogBinding = DialogAddTagBinding.inflate(LayoutInflater.from(this));
        ArrayAdapter<TagType> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TagType.values()
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerTagType.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_tag)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    try {
                        TagType type = (TagType) dialogBinding.spinnerTagType.getSelectedItem();
                        repository.addTag(albumId, photo.getId(), type, dialogBinding.editTagValue.getText().toString());
                        showPhotoAt(currentIndex);
                    } catch (RuntimeException exception) {
                        showMessage(exception.getMessage());
                    }
                })
                .show();
    }

    private void showDeleteTagDialog() {
        PhotoItem photo = getCurrentPhoto();
        if (photo == null) {
            return;
        }
        List<PhotoTag> tags = photo.getTags();
        if (tags.isEmpty()) {
            showMessage(getString(R.string.no_tags_to_delete));
            return;
        }

        String[] items = new String[tags.size()];
        for (int i = 0; i < tags.size(); i++) {
            items[i] = tags.get(i).toString();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_tag)
                .setItems(items, (dialog, which) -> {
                    try {
                        repository.deleteTag(albumId, photo.getId(), which);
                        showPhotoAt(currentIndex);
                    } catch (RuntimeException exception) {
                        showMessage(exception.getMessage());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private PhotoItem getCurrentPhoto() {
        try {
            Album album = repository.getAlbum(albumId);
            if (album.getPhotos().isEmpty() || currentIndex < 0 || currentIndex >= album.getPhotos().size()) {
                return null;
            }
            return album.getPhotos().get(currentIndex);
        } catch (RuntimeException exception) {
            showMessage(exception.getMessage());
            return null;
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

