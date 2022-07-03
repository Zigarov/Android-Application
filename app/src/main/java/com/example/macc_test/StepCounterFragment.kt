package com.example.macc_test

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.macc_test.databinding.FragmentStepCounterBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StepCounterFragment : Fragment(), SensorEventListener {
    private var _binding: FragmentStepCounterBinding? = null
    private val binding get() = _binding!!
    private var sensorManager: SensorManager? = null
    private var running = false
    private var totalSteps = 0f
    private var previousTotalSteps = 0f
    private var currentSteps: Int = 0
    private var amount: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val safeArgs: StepCounterFragmentArgs by navArgs()
        amount = safeArgs.amount
        sensorManager =  requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepCounterBinding.inflate(inflater, container, false)
        with(binding) {
            tvMaxSteps.text = amount.toString()
            circularProgressBar.progressMax = amount.toFloat()
            resetSteps()
            loadData()
            tvStepsTaken.text = 0.toString()

//            buttonStepCounter.isVisible = false
            buttonStepCounter.setOnClickListener {
//              Stop the current walk and navigate back to home
                running = false
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    Log.d("ckp_StepCounterFragment_db", "User Instance: ${currentUser.email}")
                    insertRunInDb(currentUser.email!!)
                } else {
                    Log.d("ckp_StepCounterFragment", "Impossible to save run in db, currentUser == null!")
                }
                findNavController().navigate(StepCounterFragmentDirections.nextAction())
            }
            return root
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if(stepSensor == null){
            Toast.makeText(activity, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        }
        else{
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(running){
            totalSteps = event!!.values[0]
            currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()

            with(binding){
                tvStepsTaken.text = ("$currentSteps")

                circularProgressBar.apply {
                    setProgressWithAnimation(currentSteps.toFloat())
                }

                if(currentSteps.toFloat() >= circularProgressBar.progressMax) {
                    textViewFine.textSize = 22f
                    textViewFine.text = "Grande! Hai completato l'allenamento!"
                    gifImageViewStepCounter.setImageResource(R.drawable.win2)
                    //buttonStepCounter.isVisible = true //bottone per tornare indietro diventa visibile
                }
            }

        }
    }

    private fun resetSteps() {
        with(binding){
            tvStepsTaken.setOnClickListener{
                Toast.makeText(activity, "Long tap to reset steps", Toast.LENGTH_SHORT).show()
            }

            tvStepsTaken.setOnLongClickListener {

                previousTotalSteps = totalSteps
                tvStepsTaken.text = 0.toString()
                circularProgressBar.apply {
                    setProgressWithAnimation(0.toFloat())
                }
                saveData()
                true
            }
        }
    }

    private fun saveData() {
        val sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("key1", previousTotalSteps)
        editor.apply()
    }

    private fun loadData(){
        val sharedPreferences = requireActivity().getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("key1", 0f)
        Log.d("ckp_StepCounter", "Steps Saved $savedNumber")
        previousTotalSteps = savedNumber
    }

    private fun insertRunInDb(currentUser: String) = CoroutineScope(Dispatchers.IO).launch {
        try{
            val db = FirebaseFirestore.getInstance()
            val _t = Timestamp.now()
            val timestamp = _t.seconds
            val date = _t.toDate()
            db
                .collection("users")
                .document(currentUser)
                .collection("stepsLog")
                .document(timestamp.toString())
                .set(
                    hashMapOf(
                        "timestamp" to timestamp,
                        "date" to date,
                        "steps" to currentSteps
                    )
                ).await()
            Log.d("ckp_StepCounterFragment", "Run inserted in $currentUser collection!")

            db.collection("users").document(currentUser)
                .update("score", FieldValue.increment(currentSteps.toLong()))
                .await()
            Log.d("ckp", "Personal Score Incremented!")

        } catch (e: Exception) {
            Log.d("ckp_StepCounterFragment", "Failed to insert Run in db! ${e.message}")
        }
    }
}