package com.example.macc_test

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.macc_test.databinding.FragmentSetUpWalkBinding

class SetUpWalkFragment : Fragment() {
    private var _binding: FragmentSetUpWalkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetUpWalkBinding.inflate(inflater, container, false)
        with(binding) {
            buttonSetupAll.isEnabled = binding.editTextNumberDecimal.text.isNotBlank()

            editTextNumberDecimal.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    buttonSetupAll.isEnabled = editTextNumberDecimal.text.isNotBlank()
                }
                override fun beforeTextChanged(s: CharSequence, start: Int,
                                               count: Int, after: Int) { }

                override fun onTextChanged(s: CharSequence, start: Int,
                                           before: Int, count: Int) {
                    buttonSetupAll.isEnabled = false
                }
            })

            buttonSetupAll.setOnClickListener {
                val amount = editTextNumberDecimal.text.toString().toInt()
                val action = SetUpWalkFragmentDirections.nextAction(amount)
                findNavController().navigate(action)
                Log.d("ckp_SetUpWalkFragment", "Steps amount: $amount")
//                val intent = Int(activity, StepCounter::class.java).apply{
//                    putExtra("amount", amount)
//                }
//                startActivity(intent)
                }
            return root
        }
    }

}