package com.upc.edufinservice.iam.interfaces.rest.transform;

import com.upc.edufinservice.iam.domain.model.aggregates.User;
import com.upc.edufinservice.iam.interfaces.rest.resources.UserResource;

public class UserResourceFromAggregateAssembler {
    public static UserResource toResourceFromAggregate(User aggregate){
        return new UserResource(aggregate.getId(),
                aggregate.getUsername(),
                aggregate.getEmail(),
                aggregate.getFullName(),
                aggregate.getAvatarUrl());
    }
}
