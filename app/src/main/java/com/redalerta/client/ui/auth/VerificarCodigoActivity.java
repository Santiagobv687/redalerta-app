package com.redalerta.client.ui.auth;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.redalerta.client.R;
import com.redalerta.client.databinding.ActivityVerificarCodigoBinding;
import com.redalerta.client.viewmodel.AuthViewModel;

import java.util.Locale;

public class VerificarCodigoActivity extends AppCompatActivity {

    public static final String EXTRA_CORREO = "extra_correo_verificacion";
    private static final long TIEMPO_ESPERA_REENVIO_MS = 60_000L;

    private ActivityVerificarCodigoBinding binding;
    private AuthViewModel viewModel;
    private EditText[] otpBoxes;
    private String correoUsuario = "";
    private boolean actualizandoProgramaticamente = false;

    private CountDownTimer temporizadorReenvio;
    private boolean enfriamientoReenvioActivo = false;
    private boolean peticionEnCurso = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerificarCodigoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        correoUsuario = getIntent().getStringExtra(EXTRA_CORREO);
        if (correoUsuario == null) {
            correoUsuario = "";
        }
        binding.tvCorreoIngresado.setText(correoUsuario);

        otpBoxes = new EditText[]{
                binding.etOtp1,
                binding.etOtp2,
                binding.etOtp3,
                binding.etOtp4,
                binding.etOtp5,
                binding.etOtp6
        };

        configurarCajasOtp();

        // Regresar a la primera pantalla para editar el correo
        View.OnClickListener volverAEditarListener = v -> finish();
        binding.tvVolverPaso1.setOnClickListener(volverAEditarListener);
        binding.btnEditarCorreo.setOnClickListener(volverAEditarListener);

        // Verificar el código de 6 dígitos
        binding.btnVerificarCodigo.setOnClickListener(v -> intentarVerificarCodigo());

        // Reenviar un nuevo código al mismo correo (solo cuando termine la cuenta regresiva)
        binding.btnReenviarCodigo.setOnClickListener(v -> {
            if (!correoUsuario.isEmpty() && !enfriamientoReenvioActivo && !peticionEnCurso) {
                limpiarErrorOtp();
                limpiarCajasOtp();
                iniciarTemporizadorReenvio();
                viewModel.solicitarRecuperacion(correoUsuario);
            }
        });

        observarViewModel();

        // Iniciar inmediatamente la cuenta regresiva de 60 segundos al entrar a la pantalla
        iniciarTemporizadorReenvio();
    }

    private void iniciarTemporizadorReenvio() {
        cancelarTemporizadorReenvio();
        enfriamientoReenvioActivo = true;
        actualizarEstadoBotonReenvio();

        temporizadorReenvio = new CountDownTimer(TIEMPO_ESPERA_REENVIO_MS, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                long segundosRestantes = Math.max(1L, (millisUntilFinished + 999L) / 1_000L);
                long minutos = segundosRestantes / 60L;
                long segundos = segundosRestantes % 60L;
                String tiempoFormateado = String.format(Locale.US, "%02d:%02d", minutos, segundos);
                binding.btnReenviarCodigo.setText(
                        getString(R.string.rec_paso2_btn_reenviar_contador, tiempoFormateado)
                );
            }

            @Override
            public void onFinish() {
                enfriamientoReenvioActivo = false;
                binding.btnReenviarCodigo.setText(getString(R.string.rec_paso2_btn_reenviar));
                actualizarEstadoBotonReenvio();
            }
        }.start();
    }

    private void actualizarEstadoBotonReenvio() {
        boolean habilitado = !enfriamientoReenvioActivo && !peticionEnCurso;
        binding.btnReenviarCodigo.setEnabled(habilitado);

        if (habilitado) {
            int colorActivo = ContextCompat.getColor(this, R.color.ra_light_text);
            int colorBordeActivo = ContextCompat.getColor(this, R.color.ra_light_label);
            int colorFondoActivo = ContextCompat.getColor(this, R.color.ra_light_card);
            binding.btnReenviarCodigo.setTextColor(colorActivo);
            binding.btnReenviarCodigo.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ra_red)));
            binding.btnReenviarCodigo.setStrokeColor(ColorStateList.valueOf(colorBordeActivo));
            binding.btnReenviarCodigo.setBackgroundTintList(ColorStateList.valueOf(colorFondoActivo));
        } else {
            int colorInactivo = ContextCompat.getColor(this, R.color.ra_light_text_secondary);
            int colorBordeInactivo = ContextCompat.getColor(this, R.color.ra_light_border);
            int colorFondoInactivo = ContextCompat.getColor(this, R.color.ra_light_bg);
            binding.btnReenviarCodigo.setTextColor(colorInactivo);
            binding.btnReenviarCodigo.setIconTint(ColorStateList.valueOf(colorInactivo));
            binding.btnReenviarCodigo.setStrokeColor(ColorStateList.valueOf(colorBordeInactivo));
            binding.btnReenviarCodigo.setBackgroundTintList(ColorStateList.valueOf(colorFondoInactivo));
        }
    }

    private void cancelarTemporizadorReenvio() {
        if (temporizadorReenvio != null) {
            temporizadorReenvio.cancel();
            temporizadorReenvio = null;
        }
    }

    private void configurarCajasOtp() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int index = i;
            EditText box = otpBoxes[i];

            box.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (actualizandoProgramaticamente) return;
                    limpiarErrorOtp();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (actualizandoProgramaticamente) return;

                    String texto = s.toString().trim();

                    // Si el usuario pegó varios dígitos (ej. el código completo de 6 dígitos)
                    if (texto.length() > 1) {
                        distribuirTextoPegado(index, texto);
                        return;
                    }

                    // Si ingresó 1 dígito, avanzar automáticamente a la siguiente casilla
                    if (texto.length() == 1) {
                        if (index < otpBoxes.length - 1) {
                            otpBoxes[index + 1].requestFocus();
                        } else {
                            ocultarTeclado(box);
                        }
                    }
                }
            });

            // Manejar tecla retroceso (Backspace) en casilla vacía para volver a la anterior
            box.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && box.getText().length() == 0
                        && index > 0) {
                    EditText prevBox = otpBoxes[index - 1];
                    prevBox.requestFocus();
                    prevBox.setText("");
                    return true;
                }
                return false;
            });
        }
    }

    private void distribuirTextoPegado(int startIndex, String textoPegado) {
        String soloDigitos = textoPegado.replaceAll("[^0-9]", "");
        actualizandoProgramaticamente = true;

        if (soloDigitos.isEmpty()) {
            otpBoxes[startIndex].setText("");
            actualizandoProgramaticamente = false;
            return;
        }

        // Si pegó los 6 dígitos, distribuir desde la primera casilla
        int targetStart = (soloDigitos.length() == 6) ? 0 : startIndex;
        int lastFilledIndex = targetStart;

        for (int i = 0; i < soloDigitos.length() && (targetStart + i) < otpBoxes.length; i++) {
            lastFilledIndex = targetStart + i;
            otpBoxes[lastFilledIndex].setText(String.valueOf(soloDigitos.charAt(i)));
            otpBoxes[lastFilledIndex].setSelection(1);
        }

        actualizandoProgramaticamente = false;

        if (lastFilledIndex < otpBoxes.length - 1) {
            otpBoxes[lastFilledIndex + 1].requestFocus();
        } else {
            otpBoxes[otpBoxes.length - 1].requestFocus();
            ocultarTeclado(otpBoxes[otpBoxes.length - 1]);
        }
    }

    private String obtenerCodigoCompleto() {
        StringBuilder sb = new StringBuilder();
        for (EditText box : otpBoxes) {
            sb.append(box.getText().toString().trim());
        }
        return sb.toString();
    }

    private void intentarVerificarCodigo() {
        limpiarErrorOtp();
        String codigo = obtenerCodigoCompleto();

        if (codigo.length() < 6 || !codigo.matches("^[0-9]{6}$")) {
            mostrarErrorOtp(getString(R.string.rec_paso2_error_incompleto));
            // Enfocar la primera casilla vacía
            for (EditText box : otpBoxes) {
                if (box.getText().toString().trim().isEmpty()) {
                    box.requestFocus();
                    break;
                }
            }
            return;
        }

        viewModel.verificarCodigo(correoUsuario, codigo);
    }

    private void mostrarErrorOtp(String mensaje) {
        binding.tvErrorOtp.setText(mensaje);
        binding.tvErrorOtp.setVisibility(View.VISIBLE);
        for (EditText box : otpBoxes) {
            box.setBackgroundResource(R.drawable.bg_otp_box_error);
        }
    }

    private void limpiarErrorOtp() {
        if (binding.tvErrorOtp.getVisibility() == View.VISIBLE) {
            binding.tvErrorOtp.setText("");
            binding.tvErrorOtp.setVisibility(View.GONE);
            for (EditText box : otpBoxes) {
                box.setBackgroundResource(R.drawable.selector_otp_box);
            }
        }
    }

    private void limpiarCajasOtp() {
        actualizandoProgramaticamente = true;
        for (EditText box : otpBoxes) {
            box.setText("");
        }
        actualizandoProgramaticamente = false;
        otpBoxes[0].requestFocus();
    }

    private void observarViewModel() {
        viewModel.getCargando().observe(this, cargando -> {
            peticionEnCurso = Boolean.TRUE.equals(cargando);
            binding.progressBar.setVisibility(peticionEnCurso ? View.VISIBLE : View.GONE);
            binding.btnVerificarCodigo.setEnabled(!peticionEnCurso);
            binding.btnEditarCorreo.setEnabled(!peticionEnCurso);
            actualizarEstadoBotonReenvio();
            for (EditText box : otpBoxes) {
                box.setEnabled(!peticionEnCurso);
            }
        });

        viewModel.getError().observe(this, msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                mostrarErrorOtp(msg);
            }
        });

        viewModel.getRecuperacionSolicitada().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                viewModel.limpiarEstadoRecuperacionSolicitada();
                com.redalerta.client.util.AlertaMensajesHelper.mostrarExito(
                        binding.getRoot(),
                        getString(R.string.rec_paso2_msg_reenviado, correoUsuario)
                );
            }
        });

        viewModel.getCodigoVerificado().observe(this, resp -> {
            if (resp != null && resp.getResetToken() != null) {
                viewModel.limpiarEstadoCodigoVerificado();
                android.content.Intent intent = new android.content.Intent(this, RestablecerPasswordActivity.class);
                intent.putExtra(RestablecerPasswordActivity.EXTRA_RESET_TOKEN, resp.getResetToken());
                intent.putExtra(RestablecerPasswordActivity.EXTRA_CORREO, correoUsuario);
                startActivity(intent);
                finish();
            }
        });
    }

    private void ocultarTeclado(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        cancelarTemporizadorReenvio();
        super.onDestroy();
    }
}
