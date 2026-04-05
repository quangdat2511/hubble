package com.example.hubble.view.me;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hubble.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class QrHubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_hub);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        MaterialCardView cardScanQr = findViewById(R.id.cardScanQr);
        MaterialCardView cardMyQr = findViewById(R.id.cardMyQr);

        toolbar.setNavigationOnClickListener(v -> finish());
        cardScanQr.setOnClickListener(v ->
                startActivity(new Intent(this, ScanQrActivity.class)));
        cardMyQr.setOnClickListener(v ->
                startActivity(new Intent(this, MyQrActivity.class)));
    }
}
