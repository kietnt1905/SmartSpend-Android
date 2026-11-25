package com.vhu.smartspend.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.database.UserDAO;
import com.vhu.smartspend.model.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtUsername, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView btnToLogin;

    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userDAO = new UserDAO(this);

        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnToLogin = findViewById(R.id.btnToLogin);

        btnRegister.setOnClickListener(v -> handleRegister());

        btnToLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // Kiểm tra dữ liệu rỗng
        if (TextUtils.isEmpty(fullName)) {
            edtFullName.setError("Vui lòng nhập họ và tên");
            edtFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            edtUsername.setError("Vui lòng nhập tên đăng nhập");
            edtUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        // Kiểm tra ràng buộc mật khẩu
        String passwordError = getPasswordError(password);
        if (passwordError != null) {
            edtPassword.setError(passwordError);
            edtPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không trùng khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        // Kiểm tra trùng username
        if (userDAO.checkUsernameExists(username)) {
            edtUsername.setError("Tên đăng nhập đã tồn tại trong hệ thống");
            edtUsername.requestFocus();
            return;
        }

        User newUser = new User(username, password, fullName, 5000000);
        boolean isSuccess = userDAO.registerUser(newUser);

        if (isSuccess) {
            Toast.makeText(this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Đăng ký thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Kiểm tra mật khẩu bằng vòng lặp cơ bản: >= 8 ký tự, có chữ hoa, số và ký tự đặc biệt.
     * Trả về thông báo lỗi nếu chưa đạt, hoặc null nếu hợp lệ.
     */
    public static String getPasswordError(String password) {
        if (password == null || password.length() < 8) {
            return "Mật khẩu phải từ 8 ký tự trở lên";
        }
        boolean hasUpper = false, hasDigit = false, hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        if (!hasUpper) return "Mật khẩu phải chứa ít nhất 1 chữ cái viết hoa";
        if (!hasDigit) return "Mật khẩu phải chứa ít nhất 1 chữ số";
        if (!hasSpecial) return "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt";
        return null;
    }
}