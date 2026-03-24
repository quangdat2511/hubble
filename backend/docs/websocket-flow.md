# Luồng Realtime Nhắn Tin Văn Bản (Backend)

Tài liệu này mô tả chi tiết cách tính năng nhắn tin văn bản realtime đang hoạt động ở backend Hubble, dựa trên:
- Spring Boot WebSocket + STOMP
- Simple Broker `/topic`
- REST API cho thao tác ghi dữ liệu (send/edit/delete)
- WebSocket push event để cập nhật UI theo thời gian thực

---

## 1. Mục tiêu kiến trúc

Backend tách thành 2 kênh giao tiếp:

1. **REST (ghi/đọc dữ liệu chuẩn)**
	- Ghi dữ liệu: gửi tin, sửa tin, xóa tin
	- Đọc dữ liệu: lấy lịch sử tin nhắn theo channel

2. **WebSocket STOMP (đẩy sự kiện realtime)**
	- Đẩy sự kiện thay đổi tin nhắn đến tất cả client đang subscribe channel
	- Giảm độ trễ cập nhật UI, tránh phải poll liên tục

Mô hình này giúp:
- Validate nghiệp vụ chặt chẽ ở backend trước khi ghi DB
- Push realtime nhanh, đồng bộ đa thiết bị

---

## 2. Thành phần chính trong code

### 2.1 Cấu hình WebSocket

File: `src/main/java/com/hubble/configuration/WebSocketConfig.java`

- `@EnableWebSocketMessageBroker`
- Broker:
  - `enableSimpleBroker("/topic")`
  - `setApplicationDestinationPrefixes("/app")`
- Endpoint handshake:
  - `/ws`
- Inbound interceptor:
  - `ChannelSubscriptionInterceptor`

Ý nghĩa:
- Client subscribe vào `/topic/channel/{channelId}` để nhận sự kiện tin nhắn
- Prefix `/app` sẵn sàng cho hướng command qua STOMP (hiện tại write vẫn đi qua REST)

### 2.2 Security cho REST

Files:
- `src/main/java/com/hubble/configuration/SecurityConfig.java`
- `src/main/java/com/hubble/security/JwtAuthenticationFilter.java`

`SecurityConfig` cho phép:
- `/api/auth/**` public
- `/ws/**` permitAll ở lớp HTTP handshake

Lưu ý quan trọng:
- Handshake `/ws` permitAll, **nhưng subscribe topic bị chặn ở interceptor**
- JWT cho REST được xử lý bởi `JwtAuthenticationFilter`, đặt `UserPrincipal` vào `SecurityContext`

### 2.3 Security cho WebSocket SUBSCRIBE

File: `src/main/java/com/hubble/configuration/ChannelSubscriptionInterceptor.java`

Interceptor chỉ xử lý frame `SUBSCRIBE` vào destination `/topic/channel/{channelId}`:

1. Đọc `Authorization` native header (dạng `Bearer <token>`)
2. Validate JWT qua `JwtService`
3. Lấy `userId` từ token
4. Parse `channelId` từ destination
5. Check membership:
	- `channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)`
6. Nếu fail -> throw exception (chặn subscribe)

=> Đảm bảo chỉ thành viên channel mới nghe được sự kiện realtime của channel đó.

### 2.4 Ghi message + push event

Files:
- `src/main/java/com/hubble/controller/MessageController.java`
- `src/main/java/com/hubble/service/MessageService.java`
- `src/main/java/com/hubble/dto/event/MessageEvent.java`

REST endpoints:
- `POST /api/messages` (send)
- `PATCH /api/messages/{messageId}` (edit)
- `DELETE /api/messages/{messageId}` (soft delete)
- `GET /api/messages/{channelId}` (đọc lịch sử, có phân trang)

Trong `MessageService`, sau khi thao tác DB thành công, backend push:

```java
messagingTemplate.convertAndSend(
	 "/topic/channel/" + savedMessage.getChannelId(),
	 MessageEvent.builder().action("SEND"|"EDIT"|"DELETE").message(...).build()
)
```

---

## 3. Data contract realtime

### 3.1 Event envelope

`MessageEvent`:

```json
{
  "action": "SEND | EDIT | DELETE",
  "message": {
	 "id": "uuid",
	 "channelId": "uuid",
	 "authorId": "uuid",
	 "replyToId": "uuid|null",
	 "content": "string|null",
	 "type": "TEXT|...",
	 "isPinned": false,
	 "editedAt": "datetime|null",
	 "createdAt": "datetime|null"
  }
}
```

### 3.2 Ý nghĩa `action`

- `SEND`: có tin nhắn mới
- `EDIT`: nội dung tin đã sửa
- `DELETE`: message bị soft delete (`isDeleted=true` trong DB), payload thường tối thiểu id

Client nên xử lý theo upsert/remove/mark-delete tùy thiết kế UI.

---

## 4. Trình tự end-to-end

### 4.1 Luồng gửi tin nhắn

1. Client A gọi `POST /api/messages` kèm JWT
2. `MessageController` lấy `currentUserId` từ `UserPrincipal`
3. `MessageService.sendMessage`:
	- verify channel tồn tại
	- verify A là member channel
	- map request -> entity
	- save DB
	- map entity -> `MessageResponse`
	- `convertAndSend("/topic/channel/{channelId}", MessageEvent{action="SEND"})`
4. Tất cả client đang subscribe topic của channel (A, B, các session khác) nhận event ngay
5. Client cập nhật UI chat

### 4.2 Luồng subscribe topic

1. Client mở WebSocket đến `/ws`
2. Client gửi STOMP SUBSCRIBE đến `/topic/channel/{channelId}` kèm native header `Authorization: Bearer ...`
3. `ChannelSubscriptionInterceptor` validate token + membership
4. Hợp lệ -> subscribe thành công
5. Không hợp lệ -> bị reject

---

## 5. Trách nhiệm từng kênh (API + Socket)

### REST đảm nhiệm

- Source of truth cho write/read
- Validation nghiệp vụ (channel tồn tại, user có quyền, ...)
- Lưu dữ liệu DB

### WebSocket đảm nhiệm

- Truyền event cập nhật nhanh đến subscriber
- Không thay thế write validation

Khuyến nghị client:
- Gửi tin qua REST
- Nghe update qua WebSocket
- Có fallback reload REST khi cần (ví dụ reconnect, missed events)

---

## 6. Bảo mật

1. Handshake `/ws` permitAll không đồng nghĩa ai cũng nghe được dữ liệu
2. Quyền nghe dữ liệu được enforce tại SUBSCRIBE interceptor
3. Mọi channel subscription đều verify:
	- JWT hợp lệ
	- user là member của channel

Nếu bỏ qua bước này, user có thể đoán channelId và nghe trộm dữ liệu.

---

## 7. Điều kiện dừng và edge cases

1. **Client không gửi Authorization ở SUBSCRIBE**
	- Interceptor reject

2. **Token hết hạn / sai**
	- Reject subscribe

3. **Không phải member channel**
	- Reject subscribe

4. **ChannelId không hợp lệ trên destination**
	- Reject subscribe

5. **Client mất kết nối websocket**
	- Khuyến nghị client auto reconnect + resubscribe
	- Sau reconnect, có thể pull REST để đồng bộ lại nếu cần

---

## 8. Điểm cần lưu ý ở phiên bản hiện tại

1. `GET /api/messages/{channelId}` hiện tại chưa check membership trong service.
	- Nên bổ sung verify `existsByChannelIdAndUserId(channelId, currentUserId)` tương tự send.

2. Đang dùng SimpleBroker in-memory (`enableSimpleBroker`).
	- 1 instance: ổn
	- Multi-instance scale out: cần broker ngoài (RabbitMQ/Redis STOMP relay) để đồng bộ event giữa các node.

3. `sendMessage` chưa annotate `@Transactional`.
	- Thường vẫn chạy được vì 1 save + push ngay sau đó.
	- Nếu cần đảm bảo giao dịch phức tạp hơn (attachments, side effects), nên xem lại transaction boundary rõ ràng.

---

## 9. Checklist test nhanh

1. A và B là member cùng channel DM
2. A/B cùng subscribe `/topic/channel/{channelId}` với token hợp lệ
3. A gửi REST `POST /api/messages`
4. A và B đều nhận event `SEND` gần như đồng thời
5. B edit/xóa -> A nhận `EDIT`/`DELETE`
6. User C (không phải member) subscribe topic đó -> phải bị reject

---

## 10. Tóm tắt một câu

Realtime text chat backend hiện hoạt động theo mô hình **REST write + STOMP push**, trong đó **authorization cho socket được enforce ở SUBSCRIBE interceptor theo membership channel**, và event được broadcast qua `/topic/channel/{channelId}` sau mỗi thao tác send/edit/delete.
