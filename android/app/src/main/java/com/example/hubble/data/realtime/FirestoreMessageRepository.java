package com.example.hubble.data.realtime;

import com.example.hubble.data.model.MessageDto;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FirestoreMessageRepository {

    public interface MessagesListener {
        void onMessages(List<MessageDto> messages);
        void onError(String error);
    }

    private static final int DEFAULT_LIMIT = 50;

    private final FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;

    public FirestoreMessageRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void sendMessage(String channelId, String authorId, String authorName, String content) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("channelId", channelId);
        msg.put("authorId", authorId);
        msg.put("authorName", authorName);
        msg.put("content", content);
        msg.put("type", "TEXT");
        msg.put("isPinned", false);
        msg.put("editedAt", null);
        msg.put("createdAt", FieldValue.serverTimestamp());

        db.collection("channels")
                .document(channelId)
                .collection("messages")
                .add(msg);
    }

    /**
     * Lắng nghe tin nhắn real-time. Callback kích hoạt ngay lần đầu (load lịch sử)
     * và mỗi khi có tin mới — thay thế hoàn toàn WebSocket STOMP.
     */
    public void listenMessages(String channelId, MessagesListener listener) {
        removeListener();

        listenerRegistration = db.collection("channels")
                .document(channelId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(DEFAULT_LIMIT)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    List<MessageDto> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        MessageDto dto = toMessageDto(doc);
                        if (dto != null) messages.add(dto);
                    }
                    listener.onMessages(messages);
                });
    }

    public void removeListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private MessageDto toMessageDto(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        MessageDto dto = new MessageDto();
        dto.setId(doc.getId());
        dto.setChannelId(doc.getString("channelId"));
        dto.setAuthorId(doc.getString("authorId"));
        dto.setContent(doc.getString("content"));
        dto.setType(doc.getString("type"));

        Object isPinned = doc.get("isPinned");
        if (isPinned instanceof Boolean) dto.setIsPinned((Boolean) isPinned);

        Timestamp createdAt = doc.getTimestamp("createdAt");
        if (createdAt != null) dto.setCreatedAt(formatTimestamp(createdAt));

        Timestamp editedAt = doc.getTimestamp("editedAt");
        if (editedAt != null) dto.setEditedAt(formatTimestamp(editedAt));

        return dto;
    }

    private String formatTimestamp(Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(timestamp.toDate());
    }
}
