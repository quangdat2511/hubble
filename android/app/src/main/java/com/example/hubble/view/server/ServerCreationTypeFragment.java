package com.example.hubble.view.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hubble.databinding.FragmentServerCreationTypeBinding;

public class ServerCreationTypeFragment extends Fragment {

    private FragmentServerCreationTypeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentServerCreationTypeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CreateServerActivity activity = (CreateServerActivity) requireActivity();

        binding.toolbar.setNavigationOnClickListener(v -> activity.finish());

        // "Create My Own" → audience selection screen
        binding.cardCreateOwn.setOnClickListener(v ->
                activity.navigateTo(new ServerAudienceFragment(), true));

        // Templates → skip audience, pre-fill server name
        setupTemplateCard(binding.cardTemplateGaming, "Gaming");
        setupTemplateCard(binding.cardTemplateSchool, "School Club");
        setupTemplateCard(binding.cardTemplateStudy, "Study Group");
        setupTemplateCard(binding.cardTemplateFriends, "Friends");
        setupTemplateCard(binding.cardTemplateArtists, "Artists & Creators");
        setupTemplateCard(binding.cardTemplateLocal, "Local Community");

        // Join server
        binding.btnJoinServer.setOnClickListener(v ->
                activity.navigateTo(new JoinServerFragment(), true));
    }

    private void setupTemplateCard(View card, String templateName) {
        card.setOnClickListener(v -> {
            CreateServerActivity activity = (CreateServerActivity) requireActivity();
            activity.getCreateServerViewModel().setServerName(templateName);
            activity.getCreateServerViewModel().setServerType("community");
            activity.navigateTo(new CustomizeServerFragment(), true);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
