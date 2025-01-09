package com.example.yoklamaceptever3

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class KayitActivity : AppCompatActivity() {
    private  lateinit var auth : FirebaseAuth
    var databaseReference : DatabaseReference? = null
    var database : FirebaseDatabase?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_kayit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //kimlik doğrulama için referans
        auth = FirebaseAuth.getInstance()

        //Realtime Database'in bir örneğini elde etmek için
        database = FirebaseDatabase.getInstance()

        //Realtime Database'e erişmek için
        databaseReference = database?.reference!!.child("profile")

        //geri tuşu için kodlar
        var arrow = findViewById<Button>(R.id.button)
        arrow.setOnClickListener {
            intent = Intent(this, GirisActivity::class.java)
            startActivity(intent)
        }

        var baslik = findViewById<TextView>(R.id.textView2)
        var kayitbuton = findViewById<Button>(R.id.button3)


        kayitbuton.setOnClickListener {

            var text2 = findViewById<TextView>(R.id.text2)
            var text3 = findViewById<TextView>(R.id.text3)
            var text4 = findViewById<TextView>(R.id.text4)

            var adsoyad = text2.text.toString()
            var email = text3.text.toString()
            var parola = text4.text.toString()

            //null olup olmadığını kontrol etme
            if (TextUtils.isEmpty(adsoyad)) {
                text2.error = "İsim ve soyisim boş bırakılamaz!"
                return@setOnClickListener
            } else if (TextUtils.isEmpty(email)) {
                text3.error = "E-posta boş bırakılamaz!"
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(parola)) {
                text4.error = "Parola boş bırakılamaz!"
                return@setOnClickListener
            }

            //kullanıcıyı email ve şifre ile kaydetmek için
            auth.createUserWithEmailAndPassword(text3.text.toString(),text4.text.toString())
                .addOnCompleteListener(this){ task ->
                    if (task.isSuccessful){
                        //kullanıcı bilgilerini al
                        var currentUser = auth.currentUser
                        //kullanıcı id alıp id altında adı ve soyadını kaydediyor
                        var currentUserDatabase = currentUser?.let { it1-> databaseReference?.child(it1.uid) }
                        currentUserDatabase?.child("adivesoyadi")?.setValue(text2.text.toString())
                        currentUserDatabase?.child("email")?.setValue(email)
                        Toast.makeText(this@KayitActivity,"kayıt başarılı", Toast.LENGTH_LONG).show()
                    }
                    else{
                        Toast.makeText(this@KayitActivity,"kayıt başarısız", Toast.LENGTH_LONG).show()
                    } } }

    }
}