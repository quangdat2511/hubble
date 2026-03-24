package com.example.hubble.data.repository;

import com.example.hubble.data.model.auth.AuthResult;

/**
 * Generic callback interface for Repository → ViewModel communication
 * Decouples Repository from MutableLiveData
 */
public interface RepositoryCallback<T> {
    void onResult(AuthResult<T> result);
}
