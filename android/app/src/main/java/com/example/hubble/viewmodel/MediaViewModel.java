package com.example.hubble.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.repository.MediaRepository;

import java.util.ArrayList;
import java.util.List;

public class MediaViewModel extends AndroidViewModel {

    private final MediaRepository mediaRepository;

    public MediaViewModel(@NonNull Application application) {
        super(application);
        mediaRepository = new MediaRepository(application);
    }

    public LiveData<MediaRepository.UploadResult> uploadMedia(Uri fileUri) {
        return mediaRepository.uploadMedia(fileUri, "media");
    }
}