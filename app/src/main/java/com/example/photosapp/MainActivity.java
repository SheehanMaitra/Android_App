package com.example.photosapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.photosapp.adapter.AlbumAdapter;
import com.example.photosapp.data.LibraryRepository;
import com.example.photosapp.databinding.ActivityMainBinding;
import com.example.photosapp.databinding.DialogTextInputBinding;
import com.example.photosapp.model.Album;
import com.example.photosapp.util.IntentExtras;

import java.util.ArrayList;

public final class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private LibraryRepository repository;
    private AlbumAdapter albumAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = LibraryRepository.getInstance(this);
        albumAdapter = new AlbumAdapter(album -> binding.textSelectedAlbum.setText(
                getString(R.string.selected_album, album.getName())
        ));

        binding.recyclerAlbums.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAlbums.setAdapter(albumAdapter);

        binding.buttonCreateAlbum.setOnClickListener(view ->
                showAlbumNameDialog(getString(R.string.create_album), "", name -> {
                    repository.createAlbum(name);
                    refreshAlbums();
                })
        );

        binding.buttonOpenAlbum.setOnClickListener(view -> openSelectedAlbum());
        binding.buttonRenameAlbum.setOnClickListener(view -> renameSelectedAlbum());
        binding.buttonDeleteAlbum.setOnClickListener(view -> deleteSelectedAlbum());
        binding.buttonSearchPhotos.setOnClickListener(view ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        refreshAlbums();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAlbums();
    }

    private void refreshAlbums() {
        albumAdapter.setAlbums(new ArrayList<>(repository.getAlbums()));
        Album selectedAlbum = albumAdapter.getSelectedAlbum();
        binding.textSelectedAlbum.setText(selectedAlbum == null
                ? getString(R.string.no_album_selected)
                : getString(R.string.selected_album, selectedAlbum.getName()));
        boolean isEmpty = albumAdapter.getItemCount() == 0;
        binding.textEmptyAlbums.setVisibility(isEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void openSelectedAlbum() {
        Album selected = requireSelectedAlbum();
        if (selected == null) {
            return;
        }
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra(IntentExtras.ALBUM_ID, selected.getId());
        startActivity(intent);
    }

    private void renameSelectedAlbum() {
        Album selected = requireSelectedAlbum();
        if (selected == null) {
            return;
        }
        showAlbumNameDialog(getString(R.string.rename_album), selected.getName(), name -> {
            repository.renameAlbum(selected.getId(), name);
            refreshAlbums();
        });
    }

    private void deleteSelectedAlbum() {
        Album selected = requireSelectedAlbum();
        if (selected == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_album)
                .setMessage(getString(R.string.delete_album_confirm, selected.getName()))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        repository.deleteAlbum(selected.getId());
                        refreshAlbums();
                    } catch (RuntimeException exception) {
                        showMessage(exception.getMessage());
                    }
                })
                .show();
    }

    private Album requireSelectedAlbum() {
        Album selected = albumAdapter.getSelectedAlbum();
        if (selected == null) {
            showMessage(getString(R.string.select_album_first));
        }
        return selected;
    }

    private void showAlbumNameDialog(String title, String initialValue, TextSubmitCallback callback) {
        DialogTextInputBinding dialogBinding = DialogTextInputBinding.inflate(LayoutInflater.from(this));
        EditText editText = dialogBinding.editTextValue;
        editText.setHint(R.string.album_name_hint);
        editText.setText(initialValue);
        editText.setSelection(editText.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    try {
                        callback.onTextSubmitted(editText.getText().toString());
                    } catch (RuntimeException exception) {
                        showMessage(exception.getMessage());
                    }
                })
                .show();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private interface TextSubmitCallback {
        void onTextSubmitted(String value);
    }
}

