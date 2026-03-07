package com.example.hubble.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentHomeBinding;
import com.example.hubble.adapter.ChannelListAdapter;
import com.example.hubble.adapter.ServerSidebarAdapter;
import com.example.hubble.viewmodel.MainViewModel;
import com.google.android.material.snackbar.Snackbar;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ChannelListAdapter channelAdapter;
    private ServerSidebarAdapter serverAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Share ViewModel with host Activity so data is consistent
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupServerSidebar(viewModel);
        setupChannelList(viewModel);
        setupToolbarActions(view);
    }

    private void setupServerSidebar(MainViewModel viewModel) {
        serverAdapter = new ServerSidebarAdapter();
        binding.rvServers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvServers.setAdapter(serverAdapter);

        viewModel.servers.observe(getViewLifecycleOwner(), servers -> {
            if (servers != null) serverAdapter.setServers(servers);
        });

        serverAdapter.setOnServerClickListener((server, position) ->
                viewModel.selectServer(server));

        binding.fabAddServer.setOnClickListener(v ->
                Snackbar.make(requireView(),
                        getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());
    }

    private void setupChannelList(MainViewModel viewModel) {
        channelAdapter = new ChannelListAdapter();
        binding.rvChannels.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChannels.setAdapter(channelAdapter);

        viewModel.selectedServer.observe(getViewLifecycleOwner(), server -> {
            if (server != null) binding.tvServerName.setText(server.getName());
        });

        viewModel.channels.observe(getViewLifecycleOwner(), items -> {
            if (items != null) channelAdapter.setItems(items);
        });
    }

    private void setupToolbarActions(View view) {
        binding.btnInvite.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        binding.btnEvents.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());

        binding.tilSearch.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
