package com.example.macc_test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macc_test.databinding.FragmentHomeBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class HomeFragment : Fragment() {
    private val TAG: String = "ckp_HOME"
    private var _user: FirebaseUser? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val currentUser get() = _user!!
    private var logList = mutableListOf<Run>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _user = Firebase.auth.currentUser
        if (_user == null){
           Log.d(TAG, "User not found!")
           activity?.recreate()
        }
//        val myUserRef = Firebase.firestore.collection("users").document(currentUser.email!!)
        friendsLogList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = LogAdapter(logList, mutableListOf<Int>())
        
        return binding.root
    }
    private fun friendsLogList() = CoroutineScope(Dispatchers.IO).launch {
        val currentUserRef = Firebase.firestore.collection("users").document(currentUser.email!!)
        userLog(currentUserRef, -1)
//        Log.d("ckp", "currentUserRef = $currentUserRef")
        val friends = currentUserRef.collection("friends").get().await()
        Log.d("ckp_friendsLogList","friends of ${currentUser.displayName} acquired!")
        val l = friends.documents.size
        for (i in friends.documents.indices) {
            val userRef = friends.documents[i].getDocumentReference("userRef")!!
            Log.d("ckp_documents", "$userRef")
            userLog(userRef, i, (l-1))
        }
    }

    private fun userLog(userRef: DocumentReference, id: Int, l: Int = 0) = CoroutineScope(Dispatchers.IO).launch {
        val user = userRef.get().await().toObject<User>()
        if (user != null) {
            Log.d(TAG, "$userRef -> ${user.fullname}")
            val userLog = userRef.collection("stepsLog").get().await()
            Log.d(TAG, "${userLog.documents.size} runs found for ${user.fullname}")
            for (doc in userLog.documents) {
                val date = doc.data!!["date"] as Timestamp
                val steps = doc.data!!["steps"] as Long
                val ts = doc.data!!["timestamp"] as Long

                val run = Run(
                    user,
                    date,
                    steps.toInt(),
                    ts.toInt()
                )
                logList.add(run)
            }
            if (id == l) {
                logList.sortByDescending { it.timestamp }
                Log.d("ckp","${logList}")
                withContext(Dispatchers.Main) {
                    binding.recyclerView.adapter!!.notifyDataSetChanged()
                }
            }
        }
        else {
            Log.d(TAG, "Mannaia")
        }
    }
}