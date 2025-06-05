package com.fedegst.boliapp

import android.animation.Animator
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvCantidad: TextView
    private lateinit var tvNumCoincidencia: TextView
    private lateinit var btnGenerar: CardView
    private lateinit var btnHistorial: CardView
    private lateinit var btnReset: CardView
    private lateinit var etNumFinal: EditText
    private lateinit var etCoincidencia: EditText
    private lateinit var animacion: LottieAnimationView
    private lateinit var etPremios: EditText
    private lateinit var randomizer: Randomizer
    private lateinit var overlayView: View
    private val historial = mutableListOf<Int>()
    private val contadorNumeros = mutableMapOf<Int, Int>()
    private var estadoGenerado = false
    private var cantidadCoincidencia = 0
    private var premios = 3
    private var estadoFinal = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvCantidad = findViewById(R.id.tvCantidad)
        tvNumCoincidencia = findViewById(R.id.tvNumeroCoincidencia)
        tvNumCoincidencia.makeInvisible()
        btnGenerar = findViewById(R.id.btnGenerarNumero)
        btnReset = findViewById(R.id.btnReset)
        etNumFinal = findViewById(R.id.etNumFinal)
        etCoincidencia = findViewById(R.id.etCoincidencia)
        etPremios = findViewById(R.id.etPremios)
        btnHistorial = findViewById(R.id.btnHistorial)
        animacion = findViewById(R.id.animation)
        overlayView = findViewById(R.id.overlay)

        btnHistorial.setOnClickListener {
            mostrarHistorial()
        }

        btnGenerar.setOnClickListener {
            etNumFinal.disable()
            etCoincidencia.disable()
            etPremios.disable()
            val finalValue = etNumFinal.text.toString().toIntOrNull() ?: 0
            val coincidencia = etCoincidencia.text.toString().toIntOrNull() ?: 3
            premios = etPremios.text.toString().toIntOrNull() ?: 3

            if (finalValue != 0) {
                estadoGenerado = true
                randomizer = Randomizer(1..finalValue)

                // Lanzamos una coroutine para la animación
                lifecycleScope.launch {
                    // Simula el efecto de "ruleta" durante 1 segundo
                    repeat(15) {
                        val temp = (1..finalValue).random()
                        tvCantidad.text = temp.toString()
                        delay(50L) // tiempo entre "números animados"
                    }

                    // Finalmente mostramos el número real
                    //val numero = randomizer.next()

                    val excluidos = numerosExcluidos(coincidencia)
                    var numero: Int

                    do {
                        numero = randomizer.next()
                    } while (numero in excluidos)

                    tvCantidad.text = numero.toString()

                    // Guardar en historial
                    historial.add(numero)

                    // Actualizar conteo
                    val cantidad = contadorNumeros.getOrDefault(numero, 0) + 1
                    contadorNumeros[numero] = cantidad

                    // Mostrar TextView si un número salió 5 veces
                    if (cantidad == coincidencia) {
                        cantidadCoincidencia++
                        tvNumCoincidencia.text = numero.toString()
                        tvNumCoincidencia.makeVisible()

                        if (cantidadCoincidencia == premios) {
                            estadoFinal = true
                            // 🎉 Animación FINAL: queda fija
                            overlayView.makeVisible()
                            animacion.apply {
                                makeVisible()
                                speed = 1f
                                playAnimation()
                                removeAllAnimatorListeners()
                                addAnimatorListener(object : Animator.AnimatorListener {
                                    override fun onAnimationStart(animation: Animator) {}

                                    override fun onAnimationEnd(animation: Animator) {
                                        mostrarHistorial()
                                    }

                                    override fun onAnimationCancel(animation: Animator) {}

                                    override fun onAnimationRepeat(animation: Animator) {
                                        cancelAnimation()
                                    }
                                })
                            }
                            return@launch
                        } else {
                            // Animación momentánea de coincidencia
                            animacion.apply {
                                makeVisible()
                                speed = 2f
                                playAnimation()
                            }
                        }
                    } else {
                        tvNumCoincidencia.makeInvisible()
                    }

                }
            } else {
                etNumFinal.enable()
                etCoincidencia.enable()
                etPremios.enable()
                Toast.makeText(
                    this,
                    "Debe ingresar el ultimo numero de la lista",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        btnReset.setOnClickListener {

            if (estadoGenerado) {
                initUI()
            }

        }
    }

    private fun initUI() {
        tvCantidad.text = " - "
        tvNumCoincidencia.makeInvisible()
        overlayView.makeGone()
        animacion.cancelAnimation()
        animacion.makeGone()

        // Restaurar estado de campos de entrada
        etNumFinal.text.clear()
        etCoincidencia.text.clear()
        etPremios.text.clear()
        etNumFinal.enable()
        etCoincidencia.enable()
        etPremios.enable()

        // Restaurar lógica del juego
        historial.clear()
        contadorNumeros.clear()
        estadoGenerado = false
        estadoFinal = false
        cantidadCoincidencia = 0
        premios = 3
    }

    private fun numerosExcluidos(coincidencia: Int): Set<Int> {
        return contadorNumeros.filterValues { it >= coincidencia }.keys
    }

    private fun mostrarHistorial() {
        val mensaje = if (historial.isEmpty()) {
            "No hay historial aún."
        } else {
            historial
                .groupBy { it } // Agrupa los iguales
                .map { (numero, lista) -> "• ${lista.joinToString(", ")}" } // Une los repetidos con coma
                .joinToString("\n") // Cada grupo en una línea
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Historial de Números")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar") { dialogInterface, _ ->
                if (estadoFinal) {
                    initUI()
                }
                dialogInterface.dismiss() // Cierra el diálogo
            }
            .create()

        dialog.show()
    }

    fun View.enable() {
        this.isEnabled = true
        this.alpha = 1f
    }

    fun View.disable() {
        this.isEnabled = false
        this.alpha = 0.5f
    }

    fun View.makeVisible() {
        this.visibility = View.VISIBLE
    }

    fun View.makeGone() {
        this.visibility = View.GONE
    }

    fun View.makeInvisible() {
        this.visibility = View.INVISIBLE
    }

}