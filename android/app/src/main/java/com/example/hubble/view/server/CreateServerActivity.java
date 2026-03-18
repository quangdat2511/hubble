package com.example.hubble.view.server;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.hubble.R;
import com.example.hubble.data.model.ServerItem;
import com.example.hubble.data.repository.ServerRepository;
import com.example.hubble.viewmodel.CreateServerViewModel;
import com.example.hubble.viewmodel.CreateServerViewModelFactory;

public class CreateServerActivity extends AppCompatActivity {

    public static final String EXTRA_SERVER_ID = "extra_server_id";
    public static final String EXTRA_SERVER_NAME = "extra_server_name";

    private CreateServerViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_server);

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
