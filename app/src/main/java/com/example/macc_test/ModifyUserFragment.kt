package com.example.macc_test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.macc_test.databinding.FragmentModifyUserBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ModifyUserFragment : Fragment() {
    private var _user: FirebaseUser? = null
    private var _binding: FragmentModifyUserBinding? = null
    private val user get() = _user!!
    private val binding get() = _binding!!
    private var newFullname: String? = null
    private var newPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _user = Firebase.auth.currentUser
        if (_user == null){
            Log.d("ckp_ModifyUserFragment", "User not found!")
            activity?.recreate()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentModifyUserBinding.inflate(inflater, container, false)

        with(binding) {
            btnUpdateUserData.setOnClickListener {
                newFullname = txtUpdateFullname.editText?.text.toString()
                newPassword = txtUpdatePassword.editText?.text.toString()
                if ((newFullname != "")&&(newPassword != "")) {
//                    Log.d("ckp_ModifyUserFragment", newFullname)
                    updateFullName()
                    updatePassword()
                }
            }
            return root
        }
    }
    private fun updateFullName() = CoroutineScope(Dispatchers.IO).launch{
        try {
            val profileUpdates = userProfileChangeRequest {
                displayName = newFullname!!
            }
            user.updateProfile(profileUpdates).await()
            Firebase.firestore.collection("users").document(user.email!!)
                .update("fullname", newFullname).await()
            findNavController().navigate(ModifyUserFragmentDirections.nextAction(newFullname))
        }
        catch(e: Exception) {
            withContext(Dispatchers.Main) {
                Log.d("ckp", e.message!!)
            }
        }
    }
    private fun updatePassword() = CoroutineScope(Dispatchers.IO).launch {
        user.updatePassword(newPassword!!).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("ckp_ModifyUserFragment", "User password updated.")
//                findNavController().navigate(ModifyUserFragmentDirections.nextAction(newFullname))
            }
        }
    }
}

