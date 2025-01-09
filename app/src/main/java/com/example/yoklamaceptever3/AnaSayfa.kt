package com.example.yoklamaceptever3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore

class AnaSayfa : AppCompatActivity() {
    private lateinit var auth:FirebaseAuth
    var databaseReference :DatabaseReference?=null
    var database :FirebaseDatabase?=null
    val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ana_sayfa)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var textViewWelcome = findViewById<TextView>(R.id.textViewWelcome)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        databaseReference = database?.reference!!.child("profile")
        var currentUser = auth.currentUser
        var userRef = databaseReference?.child(currentUser?.uid!!)
        textViewWelcome.text = userRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                textViewWelcome.text =
                    snapshot.child("adivesoyadi").value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }).toString()

        var Profile = findViewById<Button>(R.id.button9)
        var addClass = findViewById<Button>(R.id.button4)
        var joinClass = findViewById<Button>(R.id.button7)
        var exit = findViewById<Button>(R.id.button6)
        var yoklama = findViewById<Button>(R.id.yoklama)


        exit.setOnClickListener{
            auth.signOut()
            startActivity(Intent(this@AnaSayfa,GirisActivity::class.java))
        }
        yoklama.setOnClickListener{
            intent = Intent(this,Yoklama::class.java)
            startActivity(intent)
        }
        //**********************************************************************************
        // Firebase Firestore'a sınıf ekleyen fonksiyon
        fun createClass(className: String) {
            val ownerId = auth.currentUser?.uid ?: return
            val inviteCode = (100000..999999).random().toString()
            val accession = false

            // Sınıf verileri
            val classData = hashMapOf(
                "className" to className,
                "ownerId" to ownerId,
                "inviteCode" to inviteCode,
                "members" to listOf(ownerId),
                "accession" to accession
            )

            // Firestore'a sınıf ekleme
            db.collection("Classes")
                .add(classData)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(this, "Sınıf başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                    val invite = findViewById<TextView>(R.id.invite)
                    invite.text = "Davet Kodunuz : $inviteCode"

                    // Yeni oluşturulan sınıfın altına "Yoklamalar" koleksiyonunu ekleme
                    val classId = documentReference.id

                    // Örnek kullanıcı eklemeden doğrudan Yoklamalar koleksiyonunu oluşturuyoruz
                    val yoklamaData = hashMapOf(
                        "date" to "2024-11-12", // Dinamik tarih eklemek için güncelleyebilirsiniz

                        "participants" to emptyList<HashMap<String, Any>>() // Başlangıçta katılımcı listesi boş
                    )

                    // Yoklamalar koleksiyonuna belge ekle
                    db.collection("Classes")
                        .document(classId)
                        .collection("Yoklamalar")
                        .add(yoklamaData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Yoklama başarıyla eklendi ve katılımcılar listesi boş olarak oluşturuldu!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Yoklama ekleme hatası: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Sınıf oluşturulurken hata oluştu.", Toast.LENGTH_SHORT).show()
                    Log.e("Firestore", "Sınıf oluşturma hatası: ${e.message}")
                }
        }






        fun showCreateClassDialog() {
                // Dialog için layout inflater ile tasarımı şişir
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_class, null)
                val editTextClassName = dialogView.findViewById<EditText>(R.id.editTextClassName)
                val buttonCreateClass = dialogView.findViewById<Button>(R.id.buttonCreateClass)

                // Dialog oluştur
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create()

                dialog.show()

            // Oluştur butonuna tıklandığında
            buttonCreateClass.setOnClickListener {
                val className = editTextClassName.text.toString()

                if (className.isNotEmpty()) {
                    createClass(className) // Firebase'e sınıf kaydetme fonksiyonunu çağır
                    dialog.dismiss() // Dialogu kapat
                }
                else {
                    Toast.makeText(this, "Lütfen bir sınıf adı girin", Toast.LENGTH_SHORT).show()
                }
            }
        }

        addClass.setOnClickListener{
            showCreateClassDialog()
        }



        //*********************************************************************************
        // Firebase Firestore'a sınıfa üye ekleyen fonksiyon
        fun joinClass(inviteCode: String) {
            val userId = auth.currentUser?.uid ?: return

            // Sınıf davet kodu ile Firestore'dan sınıf belgesini bul
            db.collection("Classes")
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val classDoc = documents.first()
                        val membersList = classDoc.get("members") as? MutableList<String> ?: mutableListOf()

                        // Kullanıcı zaten üye değilse listeye ekle
                        if (!membersList.contains(userId)) {
                            membersList.add(userId)
                            classDoc.reference.update("members", membersList)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sınıfa başarıyla katıldınız!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Katılma işlemi sırasında hata oluştu.", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Zaten bu sınıfa katıldınız.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Geçersiz sınıf kodu.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Hata oluştu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // "Sınıfa Katıl" dialogunu gösteren fonksiyon
        fun showJoinClassDialog() {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_class, null)
            val editTextClassCode = dialogView.findViewById<EditText>(R.id.editTextClassCode)

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.show()

            dialogView.findViewById<Button>(R.id.buttonJoinClass).setOnClickListener {
                val classCode = editTextClassCode.text.toString()
                if (classCode.isNotEmpty()) {
                    joinClass(classCode)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Lütfen bir sınıf kodu girin", Toast.LENGTH_SHORT).show()
                }
            }
        }
        joinClass.setOnClickListener {
            showJoinClassDialog()
        }

        val KayitliDersler = findViewById<Button>(R.id.button8)

        KayitliDersler.setOnClickListener {
            startActivity(Intent(this, DersListesiActivity::class.java))
        }
//*******************************************************************************************************************





    }
}

