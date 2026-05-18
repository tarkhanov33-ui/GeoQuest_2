package com.example.geoquest.auth

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.geoquest.R
import com.example.geoquest.core.MenuActivity
import com.google.android.material.button.MaterialButton

class RegisterActivity : AppCompatActivity() {

    private val registerHelper = FirestoreRegisterHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etCity = findViewById<EditText>(R.id.etCity)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        tvGoToLogin.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val fullName = etFullName.text.toString().trim()
            val city = etCity.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please agree with Terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userMap = hashMapOf(
                "username" to username,
                "fullName" to fullName,
                "city" to city,
                "email" to email,
                "score" to 0L,
                "questsCompleted" to 0L,
                "xp" to 0L,
                "level" to 1L,
                "profileImageUrl" to "",
                "friends" to arrayListOf<String>()
            )

            registerHelper.registerUser(userMap, password) { isSuccess, message ->
                if (isSuccess) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MenuActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}