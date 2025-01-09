package com.example.yoklamaceptever3
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DersListesiActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var listView: ListView
    private val classNames = mutableListOf<String>()
    private val classIds = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ders_listesi)

        listView = findViewById(R.id.listViewClasses)
        auth = FirebaseAuth.getInstance()

        fetchRegisteredClasses()

        var buttonBack2 = findViewById<Button>(R.id.buttonBack2)
        buttonBack2.setOnClickListener{
            intent = Intent(this,AnaSayfa::class.java)
            startActivity(intent)
        }
    }

    private fun fetchRegisteredClasses() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("Classes")
            .whereArrayContains("members", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        classNames.add(document.getString("className") ?: "Sınıf Adı Yok")
                        classIds.add(document.id)
                    }

                    val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, classNames)
                    listView.adapter = adapter

                    // Liste elemanlarına tıklanıldığında
                    listView.setOnItemClickListener { _, _, position, _ ->
                        val selectedClassId = classIds[position]
                        val intent = Intent(this, KisiListesiActivity::class.java)
                        intent.putExtra("classId", selectedClassId)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Kayıtlı ders bulunamadı.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Dersler getirilirken hata oluştu.", Toast.LENGTH_SHORT).show()
            }
    }
}
//  ************************************************



