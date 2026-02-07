package com.example.neonguard


import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isAlarmActive = false

    private lateinit var statusText: TextView
    private lateinit var btnActivate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnActivate = findViewById(R.id.btnActivate)

        // Inicializa os sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        btnActivate.setOnClickListener {
            if (!isAlarmActive) {
                ativarAlarme()
            } else {
                desativarAlarme()
            }
        }
    }

    private fun ativarAlarme() {
        isAlarmActive = true
        btnActivate.text = "DESATIVAR"
        btnActivate.backgroundTintList = getColorStateList(android.R.color.holo_red_dark)
        statusText.text = "SISTEMA ARMADO"
        statusText.setTextColor(Color.RED)

        // Começa a ouvir o sensor
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        Toast.makeText(this, "Não mexa no celular!", Toast.LENGTH_SHORT).show()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun desativarAlarme() {
        isAlarmActive = false
        btnActivate.text = "ATIVAR"
        btnActivate.backgroundTintList = getColorStateList(android.R.color.darker_gray)
        statusText.text = "SISTEMA DESARMADO"
        statusText.setTextColor(Color.GREEN)

        // Para de ouvir o sensor para economizar bateria
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isAlarmActive && event != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Se o movimento sair do normal (celular parado na mesa)
            if (Math.abs(x) > 1.5 || Math.abs(y) > 1.5) {
                dispararAlerta()
            }
        }
    }

    private fun dispararAlerta() {
        // Por enquanto apenas muda o texto, na segunda colocamos o som!
        statusText.text = "ALERTA: MOVIMENTO DETECTADO!"
        statusText.setTextColor(Color.YELLOW)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        // Importante: desativar ao sair do app para não gastar bateria à toa
        if (isAlarmActive) sensorManager.unregisterListener(this)
    }
}