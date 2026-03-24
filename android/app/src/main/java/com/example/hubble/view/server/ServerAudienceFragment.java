package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.databinding.FragmentServerAudienceBinding;

public class ServerAudienceFragment extends Fragment {

    private FragmentServerAudienceBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentServerAudienceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CreateServerActivity activity = (CreateServerActivity) requireActivity();

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        binding.cardCommunity.setOnClickListener(v -> {
            activity.getCreateServerViewModel().setServerType("community");
            activity.navigateTo(new CustomizeServerFragment(), true);
        });

        binding.cardFriends.setOnClickListener(v -> {
            activity.getCreateServerViewModel().setServerType("friends");
            activity.navigateTo(new CustomizeServerFragment(), true);
        });

        binding.btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
