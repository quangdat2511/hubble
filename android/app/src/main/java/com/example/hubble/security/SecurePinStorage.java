package com.example.hubble.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecurePinStorage {

    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "hubble_app_lock_pin_key";
    private static final String PREFS_NAME = "HubbleSecurePrefs";
    private static final String KEY_PIN_IV = "secure_pin_iv";
    private static final String KEY_PIN_VALUE = "secure_pin_value";
    private static final int GCM_TAG_LENGTH = 128;

    private final SharedPreferences preferences;

    public SecurePinStorage(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasStoredPin() {
        return !TextUtils.isEmpty(preferences.getString(KEY_PIN_IV, null))
                && !TextUtils.isEmpty(preferences.getString(KEY_PIN_VALUE, null));
    }

    public boolean savePin(String pin) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
            byte[] encrypted = cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8));
            preferences.edit()
                    .putString(KEY_PIN_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString(KEY_PIN_VALUE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getPin() {
        String iv = preferences.getString(KEY_PIN_IV, null);
        String encrypted = preferences.getString(KEY_PIN_VALUE, null);
        if (TextUtils.isEmpty(iv) || TextUtils.isEmpty(encrypted)) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new GCMParameterSpec(
                    GCM_TAG_LENGTH,
                    Base64.decode(iv, Base64.NO_WRAP)
            ));
            byte[] decrypted = cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            clearPin();
            return null;
        }
    }

    public boolean verifyPin(String pin) {
        String storedPin = getPin();
        return storedPin != null && storedPin.equals(pin);
    }

    public void clearPin() {
        preferences.edit()
                .remove(KEY_PIN_IV)
                .remove(KEY_PIN_VALUE)
                .apply();
    }

    private SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        SecretKey existingKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        if (existingKey != null) {
            return existingKey;
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
        );
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return keyGenerator.generateKey();
    }
}
