package com.vhu.smartspend.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.dao.UserDAO;

public class RegisterActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword, edtConfirm;
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        userDAO = new UserDAO(this);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirm = findViewById(R.id.edtConfirm);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnBack = findViewById(R.id.btnBack);

        btnRegister.setOnClickListener(v -> attemptRegister());
        btnBack.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString();
        String confirm = edtConfirm.getText().toString();

        if (TextUtils.isEmpty(username)) {
            edtUsername.setError("Vui lòng nhập tên đăng nhập");
            edtUsername.requestFocus();
            return;
        }
        if (username.length() < 3) {
            edtUsername.setError("Tên đăng nhập tối thiểu 3 ký tự");
            edtUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            edtConfirm.setError("Mật khẩu nhập lại không khớp");
            edtConfirm.requestFocus();
            return;
        }
        if (userDAO.isUsernameExists(username)) {
            edtUsername.setError("Tên đăng nhập đã tồn tại");
            edtUsername.requestFocus();
            return;
        }

        long id = userDAO.register(username, password);
        if (id > 0) {
            Toast.makeText(this, "Đăng ký thành công, hãy đăng nhập", Toast.LENGTH_SHORT).show();
            finish(); // quay về LoginActivity
        } else {
            Toast.makeText(this, "Đăng ký thất bại, thử lại sau", Toast.LENGTH_SHORT).show();
        }
    }
}
