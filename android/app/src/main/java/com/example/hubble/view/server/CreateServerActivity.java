package com.example.hubble.view.server;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.server.ServerItem;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.viewmodel.CreateServerViewModel;
import com.example.hubble.viewmodel.CreateServerViewModelFactory;

public class CreateServerActivity extends AppCompatActivity {

    public static final String EXTRA_SERVER_ID = "extra_server_id";
    public static final String EXTRA_SERVER_NAME = "extra_server_name";

    private CreateServerViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_server);

        // Apply system-bar insets to the root view
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, wi) -> {
            Insets bars = wi.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        ServerRepository repository = new ServerRepository(this);
        CreateServerViewModelFactory factory = new CreateServerViewModelFactory(repository);
        viewModel = new ViewModelProvider(this, factory).get(CreateServerViewModel.class);

        if (savedInstanceState == null) {
            navigateTo(new ServerCreationTypeFragment(), false);
        }
    }

    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        var transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    public CreateServerViewModel getCreateServerViewModel() {
        return viewModel;
    }

    public void onServerCreated(ServerItem server) {
        Intent result = new Intent();
        result.putExtra(EXTRA_SERVER_ID, server.getId());
        result.putExtra(EXTRA_SERVER_NAME, server.getName());
        setResult(RESULT_OK, result);
        finish();
    }
}
