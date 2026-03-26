package com.example.hubble.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.hubble.data.repository.SessionRepository;

public class SessionViewModelFactory implements ViewModelProvider.Factory {
    private final SessionRepository repository;

    public SessionViewModelFactory(SessionRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SessionViewModel.class)) {
            return (T) new SessionViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}