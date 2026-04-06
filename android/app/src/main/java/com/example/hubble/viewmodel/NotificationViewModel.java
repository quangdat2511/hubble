package com.example.hubble.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hubble.data.model.auth.AuthResult;
import com.example.hubble.data.model.notify.NotificationResponse;
import com.example.hubble.data.repository.NotificationRepository;

import java.util.List;

public class NotificationViewModel extends ViewModel {

    private final NotificationRepository repository;

    private final MutableLiveData<AuthResult<List<NotificationResponse>>> _notifications = new MutableLiveData<>();
    public final LiveData<AuthResult<List<NotificationResponse>>> notifications = _notifications;

    private final MutableLiveData<AuthResult<Long>> _unreadCount = new MutableLiveData<>();
    public final LiveData<AuthResult<Long>> unreadCount = _unreadCount;

    private final MutableLiveData<AuthResult<Void>> _markReadState = new MutableLiveData<>();
    public final LiveData<AuthResult<Void>> markReadState = _markReadState;

    public NotificationViewModel(NotificationRepository repository) {
        this.repository = repository;
    }

    public void loadNotifications(int page, int size) {
        _notifications.setValue(AuthResult.loading());
        repository.getNotifications(page, size, result -> _notifications.postValue(result));
    }

    public void loadUnreadCount() {
        repository.getUnreadCount(result -> _unreadCount.postValue(result));
    }

    public void markAsRead(String notificationId) {
        repository.markAsRead(notificationId, result -> {
            _markReadState.postValue(result);
            if (result.isSuccess()) {
                loadNotifications(0, 20);
                loadUnreadCount();
            }
        });
    }

    public void markAllAsRead() {
        repository.markAllAsRead(result -> {
            _markReadState.postValue(result);
            if (result.isSuccess()) {
                loadNotifications(0, 20);
                loadUnreadCount();
            }
        });
    }

    public void resetMarkReadState() {
        _markReadState.setValue(null);
    }
}
