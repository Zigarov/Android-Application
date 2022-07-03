package com.example.macc_test

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.macc_test.databinding.FragmentSocialBinding
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SocialFragment : Fragment() {
  private val TAG: String = "ckp_SocialFragment"
  private var _user: FirebaseUser? = null
  private var _binding: FragmentSocialBinding? = null
  private val binding get() = _binding!!
  private val user get() = _user!!
  private lateinit var users: CollectionReference

  private var imagesList = mutableListOf<Int>()
  private var placesList = mutableListOf<Int>()
  private var usersList = mutableListOf<User>()
  private var mappaUsersScore = mutableMapOf<String, Int>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _user = Firebase.auth.currentUser
    if (_user == null){
      Log.d("ckp_ModifyUserFragment", "User not found!")
      activity?.recreate()
    }
    users = Firebase.firestore.collection("users")

  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = FragmentSocialBinding.inflate(inflater, container, false)

    with(binding) {
      searchUser.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
          Log.d("ckp_SocialFragment", "Searching $query")
          searchUser.clearFocus()
          CoroutineScope(Dispatchers.IO).launch {
            try {
              val querySnapshot = query?.let {
                users
                  .whereEqualTo("email", it)
                  .get()
                  .await()
              }
              if (querySnapshot != null) {
                val resultMap = mutableMapOf<String, Int>()
                usersList = mutableListOf<User>()
                for (doc in querySnapshot.documents) {
                  val user = doc.toObject<User>()
                  if (user != null) {
                    resultMap[user.fullname] = user.score
                    usersList.add(user)
                  }
                  val mail = user?.fullname.toString()
                  Log.d("ckp_SocialFragment", mail)
                }
                withContext(Dispatchers.Main) {
//                                    resultMap =
//                                        resultMap.toList().sortedByDescending { (String, Int) -> Int }.toMap() as MutableMap<String, Int>
//                                    postToList()
//                                    postPlaces()
//                  binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                  binding.recyclerView.adapter = RecyclerAdapter(resultMap, mutableListOf<Int>(), mutableListOf<Int>(), usersList)
                  binding.tvTitle.text = "RICERCA UTENTE"
                  binding.tvDesc.isVisible = false
                  binding.tvDesc2.isVisible = false
                }

              }
              else {
                Log.d("ckp_SocialFragment", "Nothing Found")
              }
//
//                            withContext(Dispatchers.Main) {
//                                if (querySnapshot != null) {
//                                    for (doc in querySnapshot.documents) {
//                                        val user = doc.toObject<User>()
//                                        val mail = user?.email.toString()
//                                        Log.d("ckp_SocialFragment", mail)
//                                    }
//                                }
//                                else {
//                                    Log.d("ckp_SocialFragment", "Nothing Found")
//                                }
//                            }
            }
            catch (e: Exception) {
              Log.d("ckp_SocialFragment", "Query Failed! ${e.message}")
            }
          }
          return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
          return false
        }

      })
      retrieveFriendsData(user.email!!)

      return root
    }
  }

  private fun addToList(image: Int) {
    imagesList.add(image)
  }

  private fun addToPlaces(image: Int){
    placesList.add(image)
  }

  /*gestione avatar utenti*/
  private fun postToList() {
    for(i in 1..mappaUsersScore.size){
      addToList(R.mipmap.ic_launcher_round)
    }
  }

  /*gestione podio*/
  private fun postPlaces() {
    for (i in 1..mappaUsersScore.size) {
      if (i == 1) {
        addToPlaces(R.drawable.first)
      }
      if (i == 2) {
        addToPlaces(R.drawable.second)
      }
      if (i == 3) {
        addToPlaces(R.drawable.third)
      }

      if (i > 3) {
        addToPlaces(R.drawable.bianco)
      }

    }
  }

  private fun retrieveFriendsData(currentUser: String) = CoroutineScope(Dispatchers.IO).launch {
    try{
      val myUser = users.document(currentUser).get().await().toObject<User>()
      if (myUser != null) {
        mappaUsersScore[myUser.fullname] = myUser.score
        usersList = mutableListOf<User>(myUser)
      }
      val friendsColl = users.document(currentUser).collection("friends").get().await()
      for (doc in friendsColl.documents) {
        val friend = doc.getDocumentReference("userRef")?.get()?.await()?.toObject<User>()
//                Log.d(TAG, "${friend}")
        if (friend != null) {
          usersList.add(friend)
          mappaUsersScore[friend.fullname] = friend.score
        }
      }
      withContext(Dispatchers.Main) {
        mappaUsersScore =
          mappaUsersScore.toList().sortedByDescending { (String, Int) -> Int }.toMap() as MutableMap<String, Int>
        postToList()
        postPlaces()
        Log.d("ckp", "${usersList}")
        usersList.sortByDescending { it.score }
        Log.d("ckp", "${usersList}")
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = RecyclerAdapter(mappaUsersScore, imagesList, placesList, usersList)
      }
    } catch (e: Exception) {
      Log.d(TAG, "Failed to retrieve LeaderBoard Data! ${e.message}")
    }
  }
}