package com.example.neonguard

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/*



Futuras Implementações - NeonGuard
Ajuste visual para modo Dark Neon (UI/UX)
Implementação de Biometria (Digital/Rosto) para desativar alarme DONE
Barra de ajuste de sensibilidade (Threshold) na tela DONE
Modo Carregador/Fone (disparar ao desconectar)
Log/Histórico de horários de disparos




 */
class MainActivity : AppCompatActivity(), SensorEventListener {

    // 1. Definição dos Estados
    enum class EstadoAlarme {
        DESATIVADO, PREPARANDO, ATIVADO, DISPARADO
    }
    //region MINHAS VARIAVEIS

    private val flashRunnable = object : Runnable {
        override fun run() {
            try {
                isFlashOn = !isFlashOn
                cameraId?.let { cameraManager.setTorchMode(it, isFlashOn) }
                flashHandler.postDelayed(this, 200)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    private var estadoAtual = EstadoAlarme.DESATIVADO

    //region MINHAS VARIAVEIS


// ... outras variáveis ...

    // 2. Variáveis de UI e Sensores
    private lateinit var statusText: TextView
    private lateinit var btnActivate: Button
    private lateinit var layoutPrincipal: android.widget.LinearLayout // Verifique se o seu XML usa ConstraintLayout
    private lateinit var seekBar: SeekBar
    private lateinit var txtSensibilidade: TextView

    private lateinit var sensorManager: SensorManager
    private lateinit var cameraManager: CameraManager
    private var accelerometer: Sensor? = null
    private var mediaPlayer: MediaPlayer? = null

    private var cameraId: String? = null
    private var isFlashOn = false
    private val flashHandler = Handler(Looper.getMainLooper())

    private var threshold = 11.5f // Ajustado para ser sensível mas nem tanto

    // ... todas as outras variáveis aqui ...
//endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialização de UI (Certifique-se que os IDs no XML batem com estes)
        statusText = findViewById(R.id.statusText)
        btnActivate = findViewById(R.id.btnActivate)
        layoutPrincipal = findViewById(R.id.main) // Geralmente o ID do layout principal é 'main'

        // Inicialização de Sensores
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: Exception) {
            e.printStackTrace()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Lógica do Botão Único
        btnActivate.setOnClickListener {
            when (estadoAtual) {
                EstadoAlarme.DESATIVADO -> iniciarArmamento()
                else -> verificarBiometriaParaDesativar()

                        //resetarAlarme() vamos testar a biometria
            }
        }
        // Dentro do onCreate
        txtSensibilidade = findViewById(R.id.txtSensibilidade)
        seekBar = findViewById(R.id.seekBarSensibilidade)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                // Ajustamos o threshold dinamicamente
                threshold = (progress + 10).toFloat()
                txtSensibilidade.text = "SENSIBILIDADE: $threshold"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })


    }

    private fun iniciarArmamento() {
        estadoAtual = EstadoAlarme.PREPARANDO
        layoutPrincipal.setBackgroundColor(ContextCompat.getColor(this, R.color.neon_amber)) // Laranja de Alerta
        statusText.setTextColor(ContextCompat.getColor(this, R.color.cyber_black))
        btnActivate.text = "CANCELAR"

        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundosRestantes = millisUntilFinished / 1000 + 1
                statusText.text = "BOOTING SYSTEM... ($segundosRestantes s)"
            }

            override fun onFinish() {
                if (estadoAtual == EstadoAlarme.PREPARANDO) {
                    // ESTA LINHA É A CHAVE: Ela chama a função que estava cinza!
                    armarSistema()
                }
            }
        }.start()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || estadoAtual != EstadoAlarme.ATIVADO) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        if (acceleration > threshold) {
            dispararAlerta()
        }
    }

    private fun dispararAlerta() {
        estadoAtual = EstadoAlarme.DISPARADO

        // Fundo Neon Red (Alerta)
        layoutPrincipal.setBackgroundColor(ContextCompat.getColor(this, R.color.neon_red))

        statusText.text = "⚠️ SECURITY BREACH! ⚠️"
        statusText.setTextColor(Color.WHITE)

        // Botão com Borda Vermelha
        btnActivate.text = "AUTHORIZE SHUTDOWN"
        btnActivate.setBackgroundResource(R.drawable.btn_cyber_alerta) // O outro arquivo drawable!
        btnActivate.backgroundTintList = null
        btnActivate.setTextColor(Color.WHITE)

        iniciarFlash()

        // Som e Volume
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarme)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(longArrayOf(0, 500, 200, 500), 0)
    }

    private fun resetarAlarme() {
        // Parar hardware
        sensorManager.unregisterListener(this)
        pararFlash()
        mediaPlayer?.let { if(it.isPlaying) it.stop(); it.release() }
        mediaPlayer = null
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Visual Idle
        estadoAtual = EstadoAlarme.DESATIVADO
        layoutPrincipal.setBackgroundColor(ContextCompat.getColor(this, R.color.cyber_black))

        statusText.text = "NEONGUARD: IDLE"
        statusText.setTextColor(ContextCompat.getColor(this, R.color.neon_purple))

        btnActivate.text = "INITIALIZE"
        // Volta para o estilo padrão ou cinza
        btnActivate.setBackgroundResource(R.drawable.btn_cyber_vigiando)
        btnActivate.backgroundTintList = ContextCompat.getColorStateList(this, R.color.cyber_gray)
        btnActivate.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan))
    }

    // --- Funções do Flash ---
    private fun iniciarFlash() { flashHandler.post(flashRunnable) }

    private fun pararFlash() {
        flashHandler.removeCallbacks(flashRunnable)
        try {
            cameraId?.let { cameraManager.setTorchMode(it, false) }
        } catch (e: Exception) { }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onPause() {
        super.onPause()
        if (estadoAtual != EstadoAlarme.DISPARADO) {
            sensorManager.unregisterListener(this)
        }
    }

    private fun verificarBiometriaParaDesativar() {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // SE A DIGITAL FOR CORRETA, DESLIGA O ALARME
                    resetarAlarme()
                    Toast.makeText(applicationContext, "Alarme Desativado!", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Erro: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desativar NeonGuard")
            .setSubtitle("Use sua digital para desligar o alarme")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun armarSistema() {
        estadoAtual = EstadoAlarme.ATIVADO

        // Fundo Cyber Night (Quase preto)
        layoutPrincipal.setBackgroundColor(ContextCompat.getColor(this, R.color.cyber_night))

        // Texto em Magenta Neon
        statusText.text = "SYSTEM: VIGILANT"
        statusText.setTextColor(ContextCompat.getColor(this, R.color.neon_magenta))

        // Botão com Borda Ciana e Arredondado
        btnActivate.text = "ABORT SYSTEM"
        btnActivate.setBackgroundResource(R.drawable.btn_cyber_vigiando) // O arquivo que você criou!
        btnActivate.backgroundTintList = null // Libera para usar o seu drawable
        btnActivate.setTextColor(ContextCompat.getColor(this, R.color.neon_cyan))

        // Ativa Sensores
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Toast.makeText(this, "PROTOCOL INITIALIZED", Toast.LENGTH_SHORT).show()
    }



}