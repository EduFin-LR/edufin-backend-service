package com.upc.edufinservice.iam.domain.services;

import com.upc.edufinservice.iam.domain.model.aggregates.User;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByIdQuery;

import java.util.Optional;

public interface UserQueryService {
    Optional<User> handle(GetUserByIdQuery query);
}
