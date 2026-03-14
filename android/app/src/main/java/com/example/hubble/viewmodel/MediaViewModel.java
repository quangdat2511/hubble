package com.example.hubble.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.model.UploadResponse;
import com.example.hubble.data.repository.MediaRepository;

import java.util.ArrayList;
import java.util.List;

public class MediaViewModel extends AndroidViewModel {

    private final MediaRepository mediaRepository;

    private final MutableLiveData<List<String>> pendingAttachmentIds = new MutableLiveData<>(new ArrayList<>());

    public MediaViewModel(@NonNull Application application) {
        super(application);
        mediaRepository = new MediaRepository(application);
    }

    public LiveData<MediaRepository.UploadResult> uploadMedia(Uri fileUri) {
        return mediaRepository.uploadMedia(fileUri, "media");
    }

    public void addPendingAttachment(String attachmentId) {
        List<String> current = pendingAttachmentIds.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(attachmentId);
        pendingAttachmentIds.setValue(current);
    }

    public List<String> getPendingAttachmentIds() {
        List<String> ids = pendingAttachmentIds.getValue();
        return ids != null ? ids : new ArrayList<>();
    }

    public void clearPendingAttachments() {
        pendingAttachmentIds.setValue(new ArrayList<>());
    }
}