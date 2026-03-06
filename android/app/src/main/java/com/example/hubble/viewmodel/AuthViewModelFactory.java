package com.example.hubble.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.AuthRepository;

/**
 * Factory for creating AuthViewModel with injected dependencies.
 * Enables Dependency Inversion: ViewModel no longer creates its own Repository.
 */
public class AuthViewModelFactory implements ViewModelProvider.Factory {

    private final AuthRepository repository;

    public AuthViewModelFactory(AuthRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(AuthViewModel.class)) {
            return (T) new AuthViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
