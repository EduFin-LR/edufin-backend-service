package com.upc.edufinservice.iam.interfaces.rest;

import com.upc.edufinservice.iam.domain.services.UserCommandService;
import com.upc.edufinservice.iam.interfaces.rest.resources.SignUpResource;
import com.upc.edufinservice.iam.interfaces.rest.resources.UserResource;
import com.upc.edufinservice.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.upc.edufinservice.iam.interfaces.rest.transform.UserResourceFromAggregateAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam/auth/sign-up")
@Tag(name = "Authentication", description = "Endpoints para el registro y autenticación de usuarios")
public class AuthenticationController {

    private final UserCommandService _userCommandService;

    public AuthenticationController(UserCommandService userCommandService){
        _userCommandService = userCommandService;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<UserResource> signUp(@RequestBody SignUpResource resource){
        var signUpCommand = SignUpCommandFromResourceAssembler.toCommandFromResource(resource);
        var user = _userCommandService.handle(signUpCommand);

        if (user.isEmpty()) return ResponseEntity.badRequest().build();

        var userResource = UserResourceFromAggregateAssembler.toResourceFromAggregate(user.get());
        return new ResponseEntity<>(userResource, HttpStatus.CREATED);
    }
}
