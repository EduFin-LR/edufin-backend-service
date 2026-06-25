package com.upc.edufinservice.iam.domain.services;

import com.upc.edufinservice.iam.domain.model.aggregates.User;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;

import java.util.Optional;

public interface UserQueryService {
    Optional<User> handle(GetUserByIdQuery query);
    Optional<User> handle(GetUserByUsernameQuery query);
}
