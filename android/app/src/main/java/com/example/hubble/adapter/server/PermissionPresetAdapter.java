package com.example.hubble.adapter.server;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hubble.R;
import com.example.hubble.databinding.ItemPresetCardBinding;

import java.util.List;

public class PermissionPresetAdapter extends RecyclerView.Adapter<PermissionPresetAdapter.PresetViewHolder> {

    private final List<PresetData> presets;

    public PermissionPresetAdapter(List<PresetData> presets) {
        this.presets = presets;
    }

    @NonNull
    @Override
    public PresetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPresetCardBinding binding = ItemPresetCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PresetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PresetViewHolder holder, int position) {
        holder.bind(presets.get(position));
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }

    static class PresetViewHolder extends RecyclerView.ViewHolder {
        private final ItemPresetCardBinding binding;

        PresetViewHolder(ItemPresetCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PresetData preset) {
            binding.tvPresetTitle.setText(preset.title);
            binding.tvPresetDescription.setText(preset.description);

            if (preset.extraDescription != null) {
                binding.tvPresetExtra.setVisibility(View.VISIBLE);
                binding.tvPresetExtra.setText(preset.extraDescription);
            } else {
                binding.tvPresetExtra.setVisibility(View.GONE);
            }

            binding.llFeatures.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            for (String feature : preset.features) {
                View featureView = inflater.inflate(R.layout.item_preset_feature,
                        binding.llFeatures, false);
                ((TextView) featureView.findViewById(R.id.tvFeature)).setText(feature);
                binding.llFeatures.addView(featureView);
            }
        }
    }

    /** Data holder for a permission preset card. */
    public static class PresetData {
        public final String title;
        public final String description;
        public final String extraDescription;
        public final List<String> features;

        public PresetData(String title, String description, String extraDescription,
                          List<String> features) {
            this.title = title;
            this.description = description;
            this.extraDescription = extraDescription;
            this.features = features;
        }
    }
}
