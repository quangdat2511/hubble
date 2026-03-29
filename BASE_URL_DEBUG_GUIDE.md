# Hướng Dẫn Sử Dụng BASE_URL_DEBUG

Trong dự án Android, giá trị BASE_URL được đọc từ local.properties (key BASE_URL_DEBUG) cho bản build debug.

## Trường hợp 1: Không có dòng BASE_URL_DEBUG trong local.properties

Nếu local.properties không có dòng BASE_URL_DEBUG, app debug sẽ tự động fallback về backend đã deploy trên Railway(backend của nhánh main):

Áp dụng khi:
- Muốn test nhanh trên mọi máy mà không cần chạy backend local.
- Muốn dùng môi trường ổn định đã deploy.

## Trường hợp 2: Dùng ngrok để test trên 2 máy khác nhau, nhưng cùng 1 backend local

Mục tiêu: Backend chạy trên máy A, nhưng cả máy A và máy B đều gọi chung vào backend đó thông qua 1 URL ngrok.

### Cách làm

### Bước 1: Cài ngrok

Tải tại ngrok.com, tạo tài khoản miễn phí, lấy authtoken, sau đó chạy:

```bash
ngrok config add-authtoken <YOUR_TOKEN>
```

### Bước 2: Chạy backend local trên máy bạn

Chạy backend trong IntelliJ ở cổng 8080.

### Bước 3: Expose backend ra internet bằng ngrok

```bash
ngrok http 8080
```

Ngrok sẽ trả về 1 URL tương tự như này:

https://bill-uncombable-veneratively.ngrok-free.dev

### Bước 4: Cả 2 người cùng dùng URL đó trong local.properties

Trên của máy người host backend, và máy của người cùng team(không chạy backend):

BASE_URL_DEBUG=https://bill-uncombable-veneratively.ngrok-free.dev/

Lưu ý:
- Luôn để dấu / ở cuối URL.
## Trường hợp 3: Tự host backend trên máy mình bằng 10.0.2.2

Dùng khi chạy Android Emulator và backend cũng chạy trên máy tính của bạn.

```properties
BASE_URL_DEBUG=http://10.0.2.2:8080/
```

Ghi chú:
- 10.0.2.2 chỉ dùng cho Android Emulator.

## Kết luận nhanh
- Các chức năng realtime chỉ hoạt động đúng khi và chỉ khi các máy cùng dùng chung 1 backend (dù là local hay đã deploy).
- Không có BASE_URL_DEBUG -> dùng Railway.
- BASE_URL_DEBUG là URL ngrok -> nhiều máy cùng test 1 backend local.
- BASE_URL_DEBUG là 10.0.2.2 -> chạy 2 emulator trên cùng 1 máy, phù hợp test local nhanh.
