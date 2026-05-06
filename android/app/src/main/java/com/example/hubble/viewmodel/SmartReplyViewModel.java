package com.example.hubble.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.hubble.data.repository.SmartReplyRepository;

public class SmartReplyViewModel extends AndroidViewModel {

    private final SmartReplyRepository smartReplyRepository;

    private final MediatorLiveData<SmartReplyRepository.SmartReplyResult> smartReplyState = new MediatorLiveData<>();

    public SmartReplyViewModel(@NonNull Application application) {
        super(application);
        smartReplyRepository = new SmartReplyRepository(application);
        // Mặc định vừa mở lên là trạng thái IDLE (Ẩn)
        smartReplyState.setValue(SmartReplyRepository.SmartReplyResult.idle());
    }

    public LiveData<SmartReplyRepository.SmartReplyResult> getSmartReplyState() {
        return smartReplyState;
    }

    public void fetchSmartReply(String content) {
        LiveData<SmartReplyRepository.SmartReplyResult> repoSource = smartReplyRepository.fetchSmartReply(content);

        smartReplyState.addSource(repoSource, result -> {
            smartReplyState.setValue(result);
            if (result.status != SmartReplyRepository.SmartReplyResult.Status.LOADING) {
                smartReplyState.removeSource(repoSource);
            }
        });
    }

    public void hideSmartReply() {
        smartReplyState.setValue(SmartReplyRepository.SmartReplyResult.idle());
    }
}