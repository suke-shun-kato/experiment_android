package xyz.goodistory.xxperiment_android

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import xyz.goodistory.xxperiment_android.databinding.ActivityCamera2Binding

class Camera2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityCamera2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCamera2Binding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}