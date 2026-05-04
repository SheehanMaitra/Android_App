package com.example.photosapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.photosapp.adapter.PhotoGridAdapter;
import com.example.photosapp.data.LibraryRepository;
import com.example.photosapp.databinding.ActivityAlbumBinding;
import com.example.photosapp.databinding.DialogMovePhotoBinding;
import com.example.photosapp.model.Album;
import com.example.photosapp.model.PhotoItem;
import com.example.photosapp.util.ImageLoader;
import com.example.photosapp.util.IntentExtras;

import java.util.ArrayList;
import java.util.List;

public final class AlbumActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handlePickedPhoto);

    private ActivityAlbumBinding binding;
    private LibraryRepository repository;
    private PhotoGridAdapter photoAdapter;
    private String albumId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlbumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        albumId = getIntent().getStringExtra(IntentExtras.ALBUM_ID);
        if (albumId == null) {
            finish();
            return;
        }

        repository = LibraryRepository.getInstance(this);
        photoAdapter = new PhotoGridAdapter(new PhotoGridAdapter.PhotoActionListener() {
            @Override
            public void onPhotoOpen(PhotoItem photo) {
                openPhoto(photo);
            }

            @Override
            public void onPhotoMove(PhotoItem photo) {
                showMovePhotoDialog(photo);
            }

            @Override
            public void onPhotoRemove(PhotoItem photo) {
                confirmRemovePhoto(photo);
            }
        });

        binding.recyclerPhotos.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerPhotos.setAdapter(photoAdapter);

        binding.buttonAddPhoto.setOnClickListener(view -> openDocumentLauncher.launch(new String[]{"image/*"}));
        binding.buttonBackHome.setOnClickListener(view -> finish());

        refreshAlbum();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAlbum();
    }

    private void refreshAlbum() {
        try {
            Album album = repository.getAlbum(albumId);
            binding.textAlbumTitle.setText(album.getName());
            binding.textAlbumCount.setText(getString(R.string.photo_count_label, album.getPhotoCount()));
            photoAdapter.setPhotos(new ArrayList<>(album.getPhotos()));
            boolean isEmpty = album.getPhotos().isEmpty();
            binding.textEmptyPhotos.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
        } catch (RuntimeException exception) {
            showMessage(exception.getMessage());
            finish();
        }
    }

    private void handlePickedPhoto(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Some providers do not support persistable permissions.
        }

        try {
            repository.addPhotoToAlbum(albumId, uri.toString(), ImageLoader.resolveDisplayName(this, uri));
            refreshAlbum();
        } catch (RuntimeException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void openPhoto(PhotoItem photo) {
        Intent intent = new Intent(this, PhotoViewerActivity.class);
        intent.putExtra(IntentExtras.ALBUM_ID, albumId);
        intent.putExtra(IntentExtras.PHOTO_ID, photo.getId());
        intent.putExtra(IntentExtras.PHOTO_INDEX, repository.getPhotoIndex(albumId, photo.getId()));
        startActivity(intent);
    }

    private void confirmRemovePhoto(PhotoItem photo) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_photo)
                .setMessage(getString(R.string.remove_photo_confirm, photo.getDisplayName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    try {
                        repository.removePhoto(albumId, photo.getId());
                        refreshAlbum();
                    } catch (RuntimeException exception) {
                        showMessage(exception.getMessage());
                    }
                })
                .show();
    }

    private void showMovePhotoDialog(PhotoItem photo) {
        List<Album> availableAlbums = new ArrayList<>();
        for (Album album : repository.getAlbums()) {
            if (!album.getId().equals(albumId)) {
                availableAlbums.add(album);
            }
        }

        if (availableAlbums.isEmpty()) {
            showMessage(getString(R.string.no_other_albums));
            return;
        }

        DialogMovePhotoBinding dialogBinding = DialogMovePhotoBinding.inflate(LayoutInflater.from(this));
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                buildAlbumNames(availableAlbums)
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spinnerAlbums.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setTitle(R.string.move_photo)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.move, (dialog, which) -> {
                    int position = dialogBinding.spinnerAlbums.getSelectedItemPosition();
                    if (position < 0 || position >= availableAlbums.size()) {
                        showMessage(getString(R.string.select_target_album));
                        return;
                    }
                    try {
                        repository.movePhoto(albumId, availableAlbums.get(position).getId(), photo.getId());
                        refreshAlbum();
                    } catch (RuntimeException exception) {
                        showMessage(exception.getMessage());
                    }
                })
                .show();
    }

    private List<String> buildAlbumNames(List<Album> albums) {
        List<String> names = new ArrayList<>();
        for (Album album : albums) {
            names.add(album.getName());
        }
        return names;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

