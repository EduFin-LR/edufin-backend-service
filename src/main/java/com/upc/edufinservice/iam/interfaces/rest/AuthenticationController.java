package com.upc.edufinservice.iam.interfaces.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.domain.services.UserCommandService;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.iam.infrastructure.tokens.jwt.JwtTokenProvider;
import com.upc.edufinservice.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.upc.edufinservice.iam.interfaces.rest.resources.SignInResource;
import com.upc.edufinservice.iam.interfaces.rest.resources.SignUpResource;
import com.upc.edufinservice.iam.interfaces.rest.resources.UserResource;
import com.upc.edufinservice.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.upc.edufinservice.iam.interfaces.rest.transform.UserResourceFromAggregateAssembler;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/iam/auth")
@Tag(name = "Authentication", description = "Endpoints para el registro y autenticación de usuarios")
public class AuthenticationController {

    private final UserCommandService _userCommandService;
    private final UserQueryService _userQueryService;
    private final PasswordEncoder _passwordEncoder;
    private final JwtTokenProvider _tokenProvider;

    public AuthenticationController(UserCommandService userCommandService,
            UserQueryService userQueryService,
                                    PasswordEncoder passwordEncoder,
                                    JwtTokenProvider tokenProvider){
        _userCommandService = userCommandService;
        _userQueryService = userQueryService;
        _passwordEncoder = passwordEncoder;
        _tokenProvider = tokenProvider;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<UserResource> signUp(@RequestBody SignUpResource resource){
        var signUpCommand = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = _userCommandService.handle(signUpCommand);

        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo crear el usuario.");
        }

        var userResource = UserResourceFromAggregateAssembler.toResourceFromAggregate(user.get());
        return new ResponseEntity<>(userResource, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInResource resource){
        // 1. Buscar al usuario
        var userOpt = _userQueryService.handle(new GetUserByUsernameQuery(resource.username()));

        // 2. Validar credenciales y lanzar excepción si fallan
        if(userOpt.isEmpty() || !_passwordEncoder.matches(resource.password(), userOpt.get().getPasswordHash())){
            // Aquí está la magia: ¡Lanzamos la excepción!
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // 3. Generar token
        var token = _tokenProvider.generateToken(userOpt.get().getUsername());

        // 4. Retornar el recurso autenticado
        return ResponseEntity.ok(new AuthenticatedUserResource(userOpt.get().getId(), userOpt.get().getUsername(), token));
    }
}
