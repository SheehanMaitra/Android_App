package com.example.photosapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.photosapp.adapter.SearchResultAdapter;
import com.example.photosapp.data.LibraryRepository;
import com.example.photosapp.databinding.ActivitySearchBinding;
import com.example.photosapp.model.SearchMatch;
import com.example.photosapp.model.TagType;
import com.example.photosapp.util.IntentExtras;

import java.util.ArrayList;
import java.util.List;

public final class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private LibraryRepository repository;
    private SearchResultAdapter resultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = LibraryRepository.getInstance(this);
        resultsAdapter = new SearchResultAdapter(this::openSearchMatch);

        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSearchResults.setAdapter(resultsAdapter);

        ArrayAdapter<TagType> tagTypeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TagType.values()
        );
        tagTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFirstType.setAdapter(tagTypeAdapter);
        binding.spinnerSecondType.setAdapter(tagTypeAdapter);

        binding.checkboxUseSecondTag.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.layoutSecondTag.setVisibility(isChecked ? android.view.View.VISIBLE : android.view.View.GONE)
        );

        binding.buttonRunSearch.setOnClickListener(view -> runSearch());
        binding.buttonClearSearch.setOnClickListener(view -> clearSearch());
        binding.buttonBackHome.setOnClickListener(view -> finish());

        wireAutocomplete();
        clearSearch();
    }

    private void wireAutocomplete() {
        binding.editFirstValue.setThreshold(1);
        binding.editSecondValue.setThreshold(1);

        binding.spinnerFirstType.setOnItemSelectedListener(new SimpleItemSelectedListener(() ->
                updateSuggestions(binding.editFirstValue, (TagType) binding.spinnerFirstType.getSelectedItem())
        ));
        binding.spinnerSecondType.setOnItemSelectedListener(new SimpleItemSelectedListener(() ->
                updateSuggestions(binding.editSecondValue, (TagType) binding.spinnerSecondType.getSelectedItem())
        ));

        binding.editFirstValue.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updateSuggestions(binding.editFirstValue, (TagType) binding.spinnerFirstType.getSelectedItem());
            }
        });
        binding.editSecondValue.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updateSuggestions(binding.editSecondValue, (TagType) binding.spinnerSecondType.getSelectedItem());
            }
        });
    }

    private void updateSuggestions(android.widget.AutoCompleteTextView view, TagType type) {
        List<String> suggestions = repository.getAutocompleteValues(type, view.getText().toString());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
        );
        view.setAdapter(adapter);
        if (view.hasFocus() && !suggestions.isEmpty()) {
            view.showDropDown();
        }
    }

    private void runSearch() {
        try {
            TagType firstType = (TagType) binding.spinnerFirstType.getSelectedItem();
            String firstValue = binding.editFirstValue.getText().toString();
            TagType secondType = binding.checkboxUseSecondTag.isChecked()
                    ? (TagType) binding.spinnerSecondType.getSelectedItem()
                    : null;
            String secondValue = binding.checkboxUseSecondTag.isChecked()
                    ? binding.editSecondValue.getText().toString()
                    : "";

            if (binding.checkboxUseSecondTag.isChecked() && secondValue.trim().isEmpty()) {
                showMessage(getString(R.string.second_tag_required));
                return;
            }

            List<SearchMatch> matches = repository.search(
                    firstType,
                    firstValue,
                    secondType,
                    secondValue,
                    binding.radioAnd.isChecked()
            );
            resultsAdapter.setMatches(matches);
            binding.textSearchSummary.setText(getString(R.string.search_results_count, matches.size()));
            binding.textNoSearchResults.setVisibility(matches.isEmpty()
                    ? android.view.View.VISIBLE
                    : android.view.View.GONE);
        } catch (RuntimeException exception) {
            showMessage(exception.getMessage());
        }
    }

    private void clearSearch() {
        binding.spinnerFirstType.setSelection(0);
        binding.spinnerSecondType.setSelection(0);
        binding.editFirstValue.setText("");
        binding.editSecondValue.setText("");
        binding.checkboxUseSecondTag.setChecked(false);
        binding.radioAnd.setChecked(true);
        binding.layoutSecondTag.setVisibility(android.view.View.GONE);
        resultsAdapter.setMatches(new ArrayList<>());
        binding.textSearchSummary.setText(getString(R.string.search_results_count, 0));
        binding.textNoSearchResults.setVisibility(android.view.View.VISIBLE);
        updateSuggestions(binding.editFirstValue, (TagType) binding.spinnerFirstType.getSelectedItem());
        updateSuggestions(binding.editSecondValue, (TagType) binding.spinnerSecondType.getSelectedItem());
    }

    private void openSearchMatch(SearchMatch match) {
        Intent intent = new Intent(this, PhotoViewerActivity.class);
        intent.putExtra(IntentExtras.ALBUM_ID, match.getAlbumId());
        intent.putExtra(IntentExtras.PHOTO_ID, match.getPhoto().getId());
        intent.putExtra(IntentExtras.PHOTO_INDEX, match.getPhotoIndex());
        startActivity(intent);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private static final class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {

        private final Runnable callback;

        SimpleItemSelectedListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
            callback.run();
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
