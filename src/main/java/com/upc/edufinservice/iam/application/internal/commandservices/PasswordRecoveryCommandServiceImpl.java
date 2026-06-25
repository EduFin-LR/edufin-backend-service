package com.upc.edufinservice.iam.application.internal.commandservices;

import com.upc.edufinservice.iam.domain.model.entities.AccountRecoveryCode;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.AccountRecoveryCodeRepository;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class PasswordRecoveryCommandServiceImpl {

    private final UserRepository userRepository;
    private final AccountRecoveryCodeRepository recoveryCodeRepository;
    private final JavaMailSender mailSender;

    private final PasswordEncoder passwordEncoder;

    public PasswordRecoveryCommandServiceImpl(UserRepository userRepository,
                                              AccountRecoveryCodeRepository recoveryCodeRepository,
                                              JavaMailSender mailSender,
                                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void generateAndSendRecoveryCode(String email) {
        var userOpt = userRepository.findByEmail(email);

        // Por seguridad, si el correo no existe, no lanzamos error, solo ignoramos.
        // Así evitamos que atacantes descubran qué correos están registrados.
        if (userOpt.isEmpty()) return;
        var user = userOpt.get();

        // 1. Borrar códigos viejos para este usuario
        recoveryCodeRepository.deleteByUserId(user.getId());

        // 2. Generar código aleatorio de 6 dígitos (ej: 049281)
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 3. Guardar en la BD
        var recoveryCode = new AccountRecoveryCode(user.getId(), code);
        recoveryCodeRepository.save(recoveryCode);

        // 4. Enviar el correo
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Código de Recuperación de Contraseña - EduFin");

        String nombre = user.getFullName() != null ? user.getFullName() : user.getUsername();
        message.setText("Hola " + nombre + ",\n\n" +
                "Tu código para restablecer tu contraseña es: " + code + "\n\n" +
                "Este código expirará en 15 minutos.\n" +
                "Si no solicitaste este cambio, ignora este correo de forma segura.");

        mailSender.send(message);
    }

    @Transactional
    public boolean resetPassword(String email, String code, String newPassword) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return false;
        var user = userOpt.get();

        // 1. Buscar el código en la BD
        var recoveryCodeOpt = recoveryCodeRepository.findByUserIdAndCode(user.getId(), code);

        // 2. Validar que exista y no esté expirado
        if (recoveryCodeOpt.isEmpty() || recoveryCodeOpt.get().isExpired()) {
            return false;
        }

        // 3. Actualizar la contraseña

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        // 4. Borrar el código para que no se vuelva a usar
        recoveryCodeRepository.deleteByUserId(user.getId());

        return true;
    }
}