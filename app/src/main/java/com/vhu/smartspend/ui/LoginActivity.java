package com.vhu.smartspend.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.vhu.smartspend.R;
import com.vhu.smartspend.dao.UserDAO;
import com.vhu.smartspend.model.User;
import com.vhu.smartspend.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private EditText edtUsername, edtPassword;
    private UserDAO userDAO;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        // Đã đăng nhập từ trước thì vào thẳng MainActivity
        if (session.isLoggedIn()) {
            goToMain(session.getUserId(), session.getRole());
            return;
        }

        setContentView(R.layout.activity_login);
        userDAO = new UserDAO(this);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String username = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString();

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

        User user = userDAO.login(username, password);
        if (user == null) {
            Toast.makeText(this, "Sai tên đăng nhập hoặc mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        session.saveSession(user);
        goToMain(user.getId(), user.getRole());
    }

    private void goToMain(int userId, String role) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("USER_ID", userId);
        i.putExtra("ROLE", role);
        startActivity(i);
        finish();
    }
}
