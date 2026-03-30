package com.example.hubble.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.hubble.data.repository.FriendRepository;

public class FriendViewModelFactory implements ViewModelProvider.Factory {
    private final FriendRepository repository;

    public FriendViewModelFactory(FriendRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FriendViewModel.class)) {
            return (T) new FriendViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}