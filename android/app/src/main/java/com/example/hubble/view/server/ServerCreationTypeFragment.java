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

        // Templates → skip audience, pre-fill server name + type
        setupTemplateCard(binding.cardTemplateGaming, "Gaming", "gaming");
        setupTemplateCard(binding.cardTemplateSchool, "Câu lạc bộ trường", "school");
        setupTemplateCard(binding.cardTemplateStudy, "Nhóm học tập", "study");
        setupTemplateCard(binding.cardTemplateFriends, "Bạn bè", "friends");
        setupTemplateCard(binding.cardTemplateArtists, "Nghệ sĩ & Nhà sáng tạo", "artists");
        setupTemplateCard(binding.cardTemplateLocal, "Cộng đồng địa phương", "local");
    }

    private void setupTemplateCard(View card, String templateName, String templateType) {
        card.setOnClickListener(v -> {
            CreateServerActivity activity = (CreateServerActivity) requireActivity();
            activity.getCreateServerViewModel().setServerName(templateName);
            activity.getCreateServerViewModel().setServerType(templateType);
            activity.navigateTo(new CustomizeServerFragment(), true);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
