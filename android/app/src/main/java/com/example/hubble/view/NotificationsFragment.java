package com.example.hubble.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.R;
import com.example.hubble.databinding.FragmentNotificationsBinding;
import com.google.android.material.snackbar.Snackbar;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnNotificationsMore.setOnClickListener(v ->
                Snackbar.make(view, getString(R.string.main_coming_soon),
                        Snackbar.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
