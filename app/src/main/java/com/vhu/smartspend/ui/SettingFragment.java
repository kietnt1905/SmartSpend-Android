package com.vhu.smartspend.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.vhu.smartspend.R;
import com.vhu.smartspend.utils.CsvExporter;
import com.vhu.smartspend.utils.SessionManager;
import com.vhu.smartspend.utils.ThemeHelper;

public class SettingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_setting, container, false);

        SwitchCompat switchDark = v.findViewById(R.id.switchDark);
        Button btnExport = v.findViewById(R.id.btnExportCsv);

        // Đặt trạng thái trước rồi mới gán listener để không kích hoạt nhầm
        switchDark.setChecked(ThemeHelper.isDarkMode(requireContext()));
        switchDark.setOnCheckedChangeListener((btn, checked) ->
                ThemeHelper.setDarkMode(requireContext(), checked)); // Activity tự recreate

        btnExport.setOnClickListener(x -> {
            try {
                int userId = new SessionManager(requireContext()).getUserId();
                String path = CsvExporter.export(requireContext(), userId);
                Toast.makeText(requireContext(), "Đã xuất: " + path, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Xuất CSV thất bại: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
        return v;
    }
}
