package com.example.hubble.viewmodel.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.data.repository.DmRepository;
import com.example.hubble.data.repository.ServerRepository;

@SuppressWarnings("unused")
public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final DmRepository dmRepository;
    private final ServerRepository serverRepository;
    private final Context appContext;

    public MainViewModelFactory(Context appContext, DmRepository dmRepository, ServerRepository serverRepository) {
        this.appContext = appContext.getApplicationContext();
        this.dmRepository = dmRepository;
        this.serverRepository = serverRepository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(appContext, dmRepository, serverRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}


