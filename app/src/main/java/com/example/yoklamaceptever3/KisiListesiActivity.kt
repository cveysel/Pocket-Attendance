package com.example.yoklamaceptever3

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class KisiListesiActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var listViewMembers: ListView
    private val memberNames = mutableListOf<String>()


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kisi_listesi)

        listViewMembers = findViewById(R.id.listViewMembers)

        // Intent'ten sınıf ID'sini al
        val classId = intent.getStringExtra("classId") ?: return

        // Sınıf üyelerini getir
        fetchClassMembers(classId)

        var buttonBack = findViewById<Button>(R.id.buttonBack)
        buttonBack.setOnClickListener{
            intent = Intent(this,DersListesiActivity::class.java)
            startActivity(intent)
        }
    }





    private fun fetchClassMembers(classId: String) {
        db.collection("Classes").document(classId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Kurucu ID'sini ve üye ID'lerini al
                    val ownerId = document.getString("ownerId") ?: ""
                    val members = document.get("members") as? List<String> ?: listOf()

                    // Önce kurucunun adını getir
                    fetchOwnerName(ownerId) { ownerName ->
                        memberNames.add("Kurucu: $ownerName")
                        // Ardından üye isimlerini getir
                        fetchMemberNames(members)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Sınıf bilgisi alınırken hata oluştu.", Toast.LENGTH_SHORT).show()
            }
    }

    // Kurucunun adını Realtime Database'den getir
    private fun fetchOwnerName(ownerId: String, callback: (String) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance().getReference("profile").child(ownerId)
        usersRef.child("adivesoyadi").get()
            .addOnSuccessListener { snapshot ->
                val ownerName = snapshot.value as? String ?: "Kurucu adı bulunamadı"
                callback(ownerName)
            }
            .addOnFailureListener {
                callback("Kurucu adı alınamadı")
            }
    }

    // Üye adlarını Realtime Database'den getir
    private fun fetchMemberNames(memberIds: List<String>) {
        val usersRef = FirebaseDatabase.getInstance().getReference("profile")

        memberIds.forEach { memberId ->
            usersRef.child(memberId).child("adivesoyadi").get()
                .addOnSuccessListener { snapshot ->
                    val memberName = snapshot.value as? String ?: "Bilinmeyen"
                    memberNames.add(memberName)

                    // Listeyi güncelle
                    if (memberNames.size == memberIds.size + 1) { // +1, kurucu adı için
                        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, memberNames)
                        listViewMembers.adapter = adapter
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Üye bilgisi alınamadı.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}


