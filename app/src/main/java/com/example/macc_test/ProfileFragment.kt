package com.example.macc_test

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.macc_test.databinding.FragmentProfileBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.getApplicationContext
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URI

class ProfileFragment : Fragment() {
    private var _user: FirebaseUser? = null
    private var _binding: FragmentProfileBinding? = null
    private val currentUser get() = _user!!
    private val binding get() = _binding!!
    private var user: User = User()
    private var following: Boolean = false
    private lateinit var imageUri: Uri
    private var autenticated: Boolean = true

    private val selectImageFromGalleryResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                binding.profileImg.setImageURI(imageUri)
                uploadImage()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _user = Firebase.auth.currentUser!!
        if (_user == null) {
            Log.d("ckp_ProfileFragment_user", "User not found!")
            activity?.recreate()
        }
        val safeArgs: ProfileFragmentArgs by navArgs()

        if (safeArgs.email == null) {
            autenticated = true
            getUserData(currentUser.email!!)
        } else {
            user = User(
                safeArgs.fullname!!,
                safeArgs.email!!,
                safeArgs.score,
                safeArgs.followers,
                safeArgs.following
            )
            downloadProfileImg()
            if (user.email != currentUser.email) {
                autenticated = false
                checkFollow()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        with(binding) {
            if (user.fullname != fullName.text) {
                fullName.text = user.fullname
            }
            if (user.email != email.text.toString()) {
                email.text = user.email
            }
            tvScore.text = "Score: ${user.score}"
            tvFollowers.text = "followers: ${user.nFollowers}"
            tvFollowing.text = "followers: ${user.nFollowing}"

            if (autenticated) {
                buttonSignOut.setOnClickListener {
                    activity?.let { it1 ->
                        AuthUI.getInstance()
                            .signOut(it1)
                            .addOnCompleteListener {
                                Log.d("ckp_ProfileFragment", "Sing Out completed!")
                                activity?.finish()
                                startActivity(Intent(activity, MainActivity::class.java))
                            }
                    }
                }

                profileImg.setOnClickListener {
//                    val intent = Intent()
//                    intent.type = "images/*"
//                    intent.action = Intent.ACTION_GET_CONTENT
//                    startActivityForResult(intent, 100)
//                    Intent(Intent.ACTION_GET_CONTENT).also {
//                        it.type = "image/*"
//                        startActivityForResult(it, 100)
//                    }
//                  Caller
                    selectImageFromGalleryResult.launch("image/*")
                }
            } else {
                buttonSignOut.isVisible = false
            }

            btnUpdateAccount.setOnClickListener {
                // [START auth_fui_delete]
                if (autenticated) {
                    findNavController().navigate(ProfileFragmentDirections.nextAction())
                } else {
                    follow()
                }
            }
            return binding.root
        }

    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if(requestCode == 100 && resultCode == RESULT_OK) {
//            data?.data?.let {
//                imageUri = it
//                uploadImage()
//            }
//        }
//    }

    private fun downloadProfileImg() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val maxDownloadSize = 5L * 1024 * 1024
            val bytes =
                Firebase.storage.reference.child("images/${user.email}").getBytes(maxDownloadSize)
                    .await()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            withContext(Dispatchers.Main) {
                binding.profileImg.setImageBitmap(bmp)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.profileImg.setImageResource(R.drawable.tree)
                if (currentUser.email!! == user.email) Toast.makeText(
                    requireContext(),
                    "Tap on the Tree to upload new profile Img",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun uploadImage() = CoroutineScope(Dispatchers.IO).launch {
        try {
            imageUri.let {
                Firebase.storage.reference.child("images/${currentUser.email!!}").putFile(imageUri)
                    .await()

                val profileUpdates = userProfileChangeRequest {
                    photoUri = imageUri
                }

                currentUser.updateProfile(profileUpdates).await()
//                withContext(Dispatchers.Main){
//                    binding.profileImg.setImageURI(imageUri)
//                }

            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
//                Toast.makeText(getApplicationContext(), e.message, Toast.LENGTH_SHORT).show()
//                Crasha se si rimuove il commento .-.
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkFollow() = CoroutineScope(Dispatchers.IO).launch {
        val friendsCollection = Firebase.firestore.collection("users").document(currentUser.email!!)
            .collection("friends")
        friendsCollection.document(user.email).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                Log.d("ckp", "${doc.data}")
                Log.d("ckp_ProfileFragment", "Following $doc")
                binding.btnUpdateAccount.text = "unFollow"
                following = true
            } else {
                Log.d("ckp", "No such document exists!")
                binding.btnUpdateAccount.text = "Follow"
                following = false
            }
        }
            .addOnFailureListener {
                Log.d("ckp_ProfileFragment", "get failed with ", it)
            }
    }

    private fun follow() = CoroutineScope(Dispatchers.IO).launch {
        val currentUserReference =
            Firebase.firestore.collection("users").document(currentUser.email!!)
        val friendsCollectionReference = currentUserReference.collection("friends")
        val userReference = Firebase.firestore.collection("users").document(user.email)
        if (following) {
            friendsCollectionReference.document(user.email).delete()
                .addOnSuccessListener {
                    Log.d("ckp", "${user.email} removed from friends.")
                    currentUserReference.update("nFollowing", FieldValue.increment(-1))
                }
                .addOnFailureListener {
                    Log.d("ckp", "get failed with ", it)
                }
            userReference.collection("followers").document(currentUser.email!!).delete()
                .addOnSuccessListener {
                    Log.d("ckp", "${currentUser.email!!} removed from ${user.email} followers.")
                    userReference.update("nFollowers", FieldValue.increment(-1))
                    user.nFollowers--
                }
                .addOnFailureListener {
                    Log.d("ckp", "get failed with ", it)
                }
            withContext(Dispatchers.Main) {
                binding.tvFollowers.text = "followers: ${user.nFollowers}"
                binding.btnUpdateAccount.text = "Follow"
            }

        } else {
            friendsCollectionReference.document(user.email)
                .set(hashMapOf("userRef" to userReference))
                .addOnSuccessListener {
                    Log.d("ckp", "${user.email} added to ${currentUser.email} following.")
                    currentUserReference.update("nFollowing", FieldValue.increment(1))
                }
                .addOnFailureListener {
                    Log.d("ckp", "get failed with ", it)
                }
            userReference.collection("followers").document(currentUser.email!!)
                .set(hashMapOf("userRef" to currentUserReference))
                .addOnSuccessListener {
                    Log.d("ckp", "${currentUser.email!!} added to ${user.email} followers.")
                    userReference.update("nFollowers", FieldValue.increment(1))
                    user.nFollowers++
                }
                .addOnFailureListener {
                    Log.d("ckp", "get failed with ", it)
                }
            withContext(Dispatchers.Main) {
                binding.tvFollowers.text = "followers: ${user.nFollowers}"
                binding.btnUpdateAccount.text = "unFollow"
            }
        }
        following = !following
    }

    private fun getUserData(mail: String) {
        Firebase.firestore.collection("users").document(mail).get()
            .addOnSuccessListener {
                user = it.toObject<User>()!!
                Log.d("ckp", "User Data Loaded From db! $user")
                downloadProfileImg()
                with(binding) {
                    fullName.text = user.fullname
                    email.text = user.email
                    tvScore.text = "Score: ${user.score}"
                    tvFollowers.text = "followers: ${user.nFollowers}"
                    tvFollowing.text = "following: ${user.nFollowing}"
                }
            }
            .addOnFailureListener {
                Log.d("ckp", "Failed to find user data in db: ${it.message}")
            }

    }
}
