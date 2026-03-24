package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentJoinServerBinding;
import com.google.android.material.snackbar.Snackbar;

public class JoinServerFragment extends Fragment {

    private FragmentJoinServerBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentJoinServerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        binding.btnJoin.setOnClickListener(v -> {
            String link = binding.etInviteLink.getText() != null
                    ? binding.etInviteLink.getText().toString().trim() : "";
            if (link.isEmpty()) {
                binding.tilInviteLink.setError(getString(R.string.error_empty_invite));
                return;
            }
            binding.tilInviteLink.setError(null);
            Snackbar.make(requireView(),
                    getString(R.string.main_coming_soon),
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
