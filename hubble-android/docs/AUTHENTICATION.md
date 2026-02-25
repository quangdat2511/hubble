# Authentication System

Hệ thống xác thực của Hubble sử dụng **Firebase Authentication** + **Cloud Firestore**, triển khai theo kiến trúc **MVVM** (Java).

---

## Mục lục

1. [Kiến trúc](#kiến-trúc)
2. [Luồng điều hướng](#luồng-điều-hướng)
3. [Data Layer](#data-layer)
4. [ViewModel Layer](#viewmodel-layer)
5. [View Layer](#view-layer)
6. [Cấu hình Firebase](#cấu-hình-firebase)
7. [Design System](#design-system)

---

## Kiến trúc

```
View (Activity)
    │  observe LiveData
    ▼
AuthViewModel          ← duy nhất một instance per screen scope
    │  delegates to
    ▼
AuthRepository         ← wraps Firebase SDK, posts AuthResult<T>
    │
    ├─ FirebaseAuth     (email, phone, signOut)
    └─ FirebaseFirestore (users/{uid})
```

**Quy tắc:**
- Activity chỉ quan sát `LiveData`, không gọi Firebase trực tiếp.
- Repository không import `android.view.*` hay `Context` ngoài `Activity` (bắt buộc cho `PhoneAuthProvider`).
- `AuthResult<T>` bọc mọi kết quả async (`LOADING | SUCCESS<T> | ERROR`).

---

## Luồng điều hướng

```
SplashActivity
    ├─ user != null ──► MainActivity
    └─ user == null ──► LoginActivity
                            ├─ [Email tab]  email + password ──────────────────► MainActivity
                            ├─ [Phone tab]  phone ──► OtpActivity ──────────────► MainActivity
                            ├─ Quên MK ──► ForgotPasswordActivity (gửi email reset)
                            └─ Đăng ký ──► RegisterActivity ────────────────────► MainActivity
```

---

## Data Layer

### `AuthResult<T>`

Generic wrapper cho mọi operation async:

| Factory | Ý nghĩa |
|---------|---------|
| `AuthResult.loading()` | Đang xử lý |
| `AuthResult.success(data)` | Thành công, `data` chứa payload |
| `AuthResult.error(message)` | Thất bại, `message` là lý do |

### `UserModel`

POJO lưu vào Firestore tại `users/{uid}`:

| Field | Type | Mô tả                               |
|-------|------|-------------------------------------|
| `uid` | `String` | Firebase UID, trùng với document ID |
| `username` | `String` | Tên hiển thị                        |
| `email` | `String?` | Nullable nếu đăng ký bằng phone     |
| `phone` | `String?` | Nullable nếu đăng ký bằng email     |
| `createdAt` | `long` | Unix timestamp (ms)                 |

### `AuthRepository`

| Method | Firebase call | LiveData nhận |
|--------|--------------|--------------|
| `loginWithEmail(email, password, ld)` | `signInWithEmailAndPassword` | `AuthResult<FirebaseUser>` |
| `registerWithEmail(email, password, username, ld)` | `createUserWithEmailAndPassword` → write Firestore | `AuthResult<FirebaseUser>` |
| `sendPhoneOtp(phone, activity, ld)` | `PhoneAuthProvider.verifyPhoneNumber` | `AuthResult<String>` *(verificationId)* |
| `resendPhoneOtp(phone, activity, token, ld)` | `verifyPhoneNumber` + `ForceResendingToken` | `AuthResult<String>` |
| `verifyOtp(verificationId, code, ld)` | `signInWithCredential` | `AuthResult<FirebaseUser>` |
| `sendPasswordResetEmail(email, ld)` | `sendPasswordResetEmail` | `AuthResult<Void>` |
| `logout()` | `signOut()` | — |

---

## ViewModel Layer

### `AuthViewModel extends ViewModel`

Một instance duy nhất, được chia sẻ thông qua `ViewModelProvider`.

**LiveData được expose:**

| LiveData | Kiểu | Quan sát bởi |
|----------|------|-------------|
| `loginState` | `AuthResult<FirebaseUser>` | `LoginActivity` |
| `registerState` | `AuthResult<FirebaseUser>` | `RegisterActivity` |
| `otpSendState` | `AuthResult<String>` | `LoginActivity` |
| `otpVerifyState` | `AuthResult<FirebaseUser>` | `OtpActivity` |
| `forgotPasswordState` | `AuthResult<Void>` | `ForgotPasswordActivity` |

**Lưu ý quan trọng:** `storedVerificationId` được giữ trong ViewModel (sống sót qua config change), **không** truyền qua Intent. `OtpActivity` chỉ cần gọi `authViewModel.verifyOtp(code)`.

---

## View Layer

### `SplashActivity`

- Delay 1800 ms → kiểm tra `getCurrentUser()` → route sang `LoginActivity` hoặc `MainActivity`.
- `noHistory="true"` trong Manifest → không thể back về Splash.

### `LoginActivity`

- **Tab Email**: validate email format + password not empty → `loginWithEmail`.
- **Tab Phone**: validate E.164 (`+` prefix) → `sendPhoneOtp` → navigate `OtpActivity` với `EXTRA_PHONE_NUMBER`.
- Observe `loginState` & `otpSendState`.

### `RegisterActivity`

**Validation (client-side):**

| Rule | Error string |
|------|-------------|
| Display name không rỗng | `error_empty_name` |
| Email hợp lệ | `error_invalid_email` |
| Password ≥ 6 ký tự | `error_password_too_short` |
| Confirm password khớp | `error_password_mismatch` |

Sau khi Firebase tạo user → ghi `UserModel` vào Firestore → `MainActivity`.

### `OtpActivity`

- Nhận `EXTRA_PHONE_NUMBER` qua Intent.
- 6 ô input: auto-focus-next khi nhập, auto-back khi xóa.
- `CountDownTimer` 60 s → enable "Gửi lại" khi hết giờ.
- `verifyOtp(code)` dùng `storedVerificationId` từ ViewModel.

### `ForgotPasswordActivity`

Hai trạng thái UI:

| Trạng thái | View visible |
|---|---|
| Mặc định | `layoutForm` |
| Sau khi gửi thành công | `layoutSuccess` (ẩn form, hiện icon ✉ + thông báo) |

### `MainActivity`

- Session guard: nếu user null → `navigateToLogin()`.
- Hiển thị `displayName` (fallback: email → "User").
- Nút Logout: `authViewModel.logout()` → `navigateToLogin()` (clear back stack).

---

## Cấu hình Firebase

> Cần thực hiện trong [Firebase Console](https://console.firebase.google.com) trước khi test.

### 1. Bật Sign-in methods

`Authentication → Sign-in method`:
- ✅ Email/Password
- ✅ Phone

### 2. Tạo Firestore Database

`Firestore Database → Create database` (test mode hoặc production với rules phù hợp).

**Security rules gợi ý:**
```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

### 3. SHA-1 Fingerprint (bắt buộc cho Phone Auth)

```bash
# Debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Đăng ký fingerprint tại: `Project settings → Your apps → Android app → Add fingerprint`.

---

## Design System

| Token | Hex | Dùng cho |
|-------|-----|---------|
| `color_background` | `#23272A` | Nền tất cả màn hình |
| `color_surface` | `#2C2F33` | Card, TabLayout |
| `color_surface_elevated` | `#36393F` | Nút secondary |
| `color_primary` | `#5865F2` | Nút chính, tab indicator, focus ring |
| `color_primary_dark` | `#4752C4` | Pressed state của nút chính |
| `color_text_primary` | `#FFFFFF` | Văn bản chính |
| `color_text_secondary` | `#B9BBBE` | Label, placeholder |
| `color_text_hint` | `#72767D` | Hint text |
| `color_error` | `#ED4245` | Lỗi validation |
| `color_success` | `#57F287` | Thông báo thành công |
| `color_link` | `#00AFF4` | Link, Resend OTP |

**Styles sẵn có:**

| Style | Dùng cho |
|-------|---------|
| `Widget.Hubble.Button.Primary` | Nút hành động chính (Login, Register, Verify) |
| `Widget.Hubble.Button.Secondary` | Nút phụ (Back to login, Logout) |
| `Widget.Hubble.TextInputLayout` | Tất cả ô nhập liệu |
