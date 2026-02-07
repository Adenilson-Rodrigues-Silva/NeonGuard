package com.example.neonguard




import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Este bloco cria o "atraso" antes de mudar de tela
        Handler(Looper.getMainLooper()).postDelayed({

            // Cria a intenção de ir da Splash para a Main
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Aplica uma animação suave de fade entre as telas
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            // Finaliza a splash para o usuário não voltar pra ela ao apertar "voltar"
            finish()

        }, 4000) // 4000ms = 4 segundos (tempo ideal para uma splash de impacto)
    }
}
/*
val logo = findViewById<ImageView>(R.id.splashLogo)
logo.alpha = 0f
logo.animate().alpha(1f).setDuration(1500).start()
}*/