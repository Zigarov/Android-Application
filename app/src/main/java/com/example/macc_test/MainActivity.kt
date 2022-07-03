package com.example.macc_test

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.macc_test.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.ui.auth.data.model.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    //val auth = FirebaseAuth.getInstance().currentUser

    private val usersCollectionRef = Firebase.firestore.collection("users")

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        onSignInResult(res)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        auth = Firebase.auth
        binding = ActivityMainBinding.inflate(layoutInflater)
        //setContentView(binding.root)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val user = auth.currentUser
        if (user==null){
            signIN()
        } else {
            setContentView(binding.root)
//            val uiProfile = ProfileFragment()
//            val uiHome = HomeFragment()
//            val uiStats = StatsFragment()
//            val uiSocial = SocialFragment()
            //val fragments = listOf(uiStats, uiHome, uiHome, uiSocial, uiProfile)
            val host: NavHostFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainer) as NavHostFragment? ?: return

            // Set up Action Bar
            val navController = host.navController
            setupBottomNavMenu(binding.btmNavView, navController)

//            binding.btmNavView.background = null
//            binding.btmNavView.menu.getItem(2).isEnabled = false
//            binding.btmNavView.setOnItemSelectedListener {
//                when(it.itemId) {
//                    R.id.home -> setCurrentFragment(uiHome)
//                    R.id.stats -> setCurrentFragment(uiStats)
//                    R.id.social -> setCurrentFragment(uiSocial)
//                    R.id.profile -> setCurrentFragment(uiProfile)
//                }
//                true
//            }
            binding.btmNavView.selectedItemId = R.id.ProfileFragment

            binding.fabWalk.setOnClickListener{
                Log.d("ckp_MainActivity", "fab clicked!")
//                navController.navigate(R.id.SetUpWalkFragment, null)
                binding.btmNavView.selectedItemId = R.id.SetUpWalkFragment
                binding.btmNavView.menu.getItem(2).isVisible = false
            }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                val dest: String = try {
                    resources.getResourceName(destination.id)
                } catch (e: Resources.NotFoundException) {
                    destination.id.toString()
                }
//                Toast.makeText(this@MainActivity, "Navigated to $dest", Toast.LENGTH_SHORT).show()
                Log.d("ckp", "Navigated to $dest")
            }
        }

    }

    private fun setupBottomNavMenu(btmNavView: BottomNavigationView, navController: NavController) {
        btmNavView.background = null
//      btmNavView.selectedItemId = R.id.home
//      btmNavView.setOnItemSelectedListener {
//          when(it.itemId) {
//              R.id.home -> setCurrentFragment(uiHome)
//              R.id.stats -> setCurrentFragment(uiStats)
//              R.id.social -> setCurrentFragment(uiSocial)
//              R.id.profile -> setCurrentFragment(uiProfile)
//          }
//          true

        btmNavView.setupWithNavController(navController)
        btmNavView.menu.getItem(2).isVisible = false
    }

    private fun signIN() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            //AuthUI.IdpConfig.PhoneBuilder().build(),
            //AuthUI.IdpConfig.GoogleBuilder().build(),
            //AuthUI.IdpConfig.FacebookBuilder().build(),
            //AuthUI.IdpConfig.TwitterBuilder().build()
        )

        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.LoginTheme)
            .build()
        signInLauncher.launch(signInIntent)
        Log.d("ckp_MainActivity", "Firebase UI Started!")
        // [END auth_fui_create_intent]
    }

    @SuppressLint("RestrictedApi")
    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            if (response != null) {
                if (response.isNewUser) {
                    Log.d("ckp", "New User Detected!")
                    newUser(response.user)
                }
                else{
                    Log.d("ckp", "Welcome back ${response.email}!")
                }
                this.onStart()
            }
        } else {
            Log.d("ckp","Sign IN Failed")
            //recreate()
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
        }
    }
    @SuppressLint("RestrictedApi")
    private fun newUser(user: User) = CoroutineScope(Dispatchers.IO).launch {
        try{
            usersCollectionRef
                .document(user.email!!)
                .set(hashMapOf(
                    "email" to user.email!!,
                    "fullname" to user.name!!,
                    "score" to 0,
                    "nFollowers" to 0,
                    "nFollowing" to 0
                ))
                .await()
            Log.d("ckp", "New user ${user.email!!} added to db!")
        }
        catch (e: Exception) {
            withContext(Dispatchers.Main) {
               Log.d("ckp_MainActivity", "Failed to add new user in db: ${e.message}")
            }
        }
    }
}