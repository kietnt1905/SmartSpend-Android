package com.vhu.smartspend.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.UserDAO;
import com.vhu.smartspend.model.User;
import com.vhu.smartspend.utils.CategoryUiHelper;
import com.vhu.smartspend.utils.CurrencyUtils;
import com.vhu.smartspend.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends Fragment {

    private TextView tvUserFullName, tvUsername, tvMonthlyLimit;
    private ImageView imgAvatar;
    private SessionManager sessionManager;
    private UserDAO userDAO;
    private double currentMonthlyLimit = 4500000;

    private ActivityResultLauncher<String[]> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {
                        }

                        String uriString = uri.toString();
                        int userId = sessionManager.getUserId();
                        if (userId > 0) {
                            userDAO.updateAvatarUri(userId, uriString);
                        }
                        sessionManager.updateAvatarUri(uriString);
                        displayAvatar(uriString);
                        Toast.makeText(requireContext(), "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        sessionManager = new SessionManager(requireContext());
        userDAO = new UserDAO(requireContext());

        tvUserFullName = view.findViewById(R.id.tvUserFullName);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvMonthlyLimit = view.findViewById(R.id.tvMonthlyLimit);
        imgAvatar = view.findViewById(R.id.imgAvatar);

        bindUserInfo();
        loadMonthlyLimit();

        view.findViewById(R.id.layoutProfileAvatar).setOnClickListener(v -> showProfileOptionsDialog());
        view.findViewById(R.id.rowMonthlyLimit).setOnClickListener(v -> showEditMonthlyLimitDialog());
        view.findViewById(R.id.btnManageCategory).setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), CategoryManageActivity.class)));
        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutConfirmDialog());

        return view;
    }

    private void bindUserInfo() {
        String fullName = sessionManager.getFullName();
        String username = sessionManager.getUsername();
        tvUserFullName.setText(!fullName.isEmpty() ? fullName : "Người dùng");
        tvUsername.setText(!username.isEmpty() ? "@" + username : "@smartspend");

        String avatarUri = sessionManager.getAvatarUri();
        if (avatarUri.isEmpty() && sessionManager.getUserId() > 0) {
            User user = userDAO.getUserById(sessionManager.getUserId());
            if (user != null && user.getAvatarUri() != null) {
                avatarUri = user.getAvatarUri();
                sessionManager.updateAvatarUri(avatarUri);
            }
        }
        displayAvatar(avatarUri);
    }

    private void displayAvatar(String uriString) {
        if (imgAvatar == null || !isAdded()) return;
        if (uriString != null && !uriString.trim().isEmpty()) {
            try {
                Uri uri = Uri.parse(uriString);
                imgAvatar.setImageURI(uri);
                imgAvatar.setPadding(0, 0, 0, 0);
                imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        imgAvatar.setImageResource(R.drawable.ic_setting_person);
        int pad = CategoryUiHelper.dp(requireContext(), 14);
        imgAvatar.setPadding(pad, pad, pad, pad);
        imgAvatar.setScaleType(ImageView.ScaleType.CENTER);
    }

    private void loadMonthlyLimit() {
        User user = userDAO.getUserById(sessionManager.getUserId());
        if (user != null) {
            currentMonthlyLimit = user.getMonthlyBudgetLimit();
        }
        tvMonthlyLimit.setText(CurrencyUtils.formatVND(currentMonthlyLimit));
    }

    private void showProfileOptionsDialog() {
        List<String> optionsList = new ArrayList<>();
        optionsList.add("Chọn ảnh đại diện mới");
        optionsList.add("Đổi tên hiển thị");

        String currentAvatar = sessionManager.getAvatarUri();
        boolean hasAvatar = currentAvatar != null && !currentAvatar.trim().isEmpty();
        if (hasAvatar) {
            optionsList.add("Xóa ảnh đại diện");
        }

        String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Cập nhật thông tin hồ sơ")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if ("Chọn ảnh đại diện mới".equals(selected)) {
                        pickImageLauncher.launch(new String[]{"image/*"});
                    } else if ("Đổi tên hiển thị".equals(selected)) {
                        showEditFullNameDialog();
                    } else if ("Xóa ảnh đại diện".equals(selected)) {
                        removeAvatar();
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void removeAvatar() {
        int userId = sessionManager.getUserId();
        if (userId > 0) {
            userDAO.updateAvatarUri(userId, null);
        }
        sessionManager.updateAvatarUri(null);
        displayAvatar(null);
        Toast.makeText(requireContext(), "Đã xóa ảnh đại diện", Toast.LENGTH_SHORT).show();
    }

    private void showEditFullNameDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setText(sessionManager.getFullName());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi tên hiển thị")
                .setView(wrapDialogInput(input))
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (sessionManager.getUserId() > 0) {
                        userDAO.updateFullName(sessionManager.getUserId(), newName);
                    }
                    sessionManager.updateFullName(newName);
                    tvUserFullName.setText(newName);
                    Toast.makeText(requireContext(), "Đã cập nhật tên hiển thị!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);
        EditText edtOldPass = dialogView.findViewById(R.id.edtOldPassword);
        EditText edtNewPass = dialogView.findViewById(R.id.edtNewPassword);
        EditText edtConfirmPass = dialogView.findViewById(R.id.edtConfirmNewPassword);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Thay đổi mật khẩu")
                .setView(dialogView)
                .setPositiveButton("Cập nhật", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPass = edtOldPass.getText().toString().trim();
            String newPass = edtNewPass.getText().toString().trim();
            String confirmPass = edtConfirmPass.getText().toString().trim();

            if (oldPass.isEmpty()) {
                edtOldPass.setError("Vui lòng nhập mật khẩu hiện tại");
                edtOldPass.requestFocus();
                return;
            }

            int userId = sessionManager.getUserId();
            if (!userDAO.checkPassword(userId, oldPass)) {
                edtOldPass.setError("Mật khẩu hiện tại không chính xác");
                edtOldPass.requestFocus();
                return;
            }

            String passError = RegisterActivity.getPasswordError(newPass);
            if (passError != null) {
                edtNewPass.setError(passError);
                edtNewPass.requestFocus();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                edtConfirmPass.setError("Mật khẩu xác nhận không trùng khớp");
                edtConfirmPass.requestFocus();
                return;
            }

            if (userDAO.updatePassword(userId, newPass)) {
                Toast.makeText(requireContext(), "Thay đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Thay đổi mật khẩu thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
            }
        }));

        dialog.show();
    }

    private void showEditMonthlyLimitDialog() {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf((long) currentMonthlyLimit));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Hạn mức chi tiêu tháng")
                .setMessage("Nhập số tiền hạn mức (VNĐ):")
                .setView(wrapDialogInput(input))
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) return;
                    try {
                        double newLimit = Double.parseDouble(raw);
                        if (newLimit <= 0) {
                            Toast.makeText(requireContext(), "Hạn mức phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (sessionManager.getUserId() > 0) {
                            userDAO.updateMonthlyLimit(sessionManager.getUserId(), newLimit);
                        }
                        currentMonthlyLimit = newLimit;
                        tvMonthlyLimit.setText(CurrencyUtils.formatVND(currentMonthlyLimit));
                        Toast.makeText(requireContext(), "Đã cập nhật hạn mức tháng!", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất tài khoản không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private FrameLayout wrapDialogInput(EditText input) {
        int pad = CategoryUiHelper.dp(requireContext(), 20);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);
        return container;
    }
}