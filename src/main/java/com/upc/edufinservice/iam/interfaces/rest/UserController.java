package com.upc.edufinservice.iam.interfaces.rest;

import com.upc.edufinservice.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import com.upc.edufinservice.iam.interfaces.rest.resources.UserResource;
import com.upc.edufinservice.iam.interfaces.rest.transform.UserResourceFromAggregateAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints para la gestion de usuarios")
public class UserController {
    private final UserQueryService _userQueryservice;

    public UserController(UserQueryService userQueryservice){
        _userQueryservice = userQueryservice;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResource> getUserById(@PathVariable Long userId){
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        var user = _userQueryservice.handle(getUserByIdQuery);

        if (user.isEmpty()) return ResponseEntity.notFound().build();

        var userResource = UserResourceFromAggregateAssembler.toResourceFromAggregate(user.get());
        return ResponseEntity.ok(userResource);

    }
}
