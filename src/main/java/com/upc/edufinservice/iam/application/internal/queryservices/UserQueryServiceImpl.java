package com.upc.edufinservice.iam.application.internal.queryservices;

import com.upc.edufinservice.iam.domain.model.queries.GetUserByUsernameQuery;
import com.upc.edufinservice.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.upc.edufinservice.iam.domain.model.aggregates.User;
import com.upc.edufinservice.iam.domain.model.queries.GetUserByIdQuery;
import com.upc.edufinservice.iam.domain.services.UserQueryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserQueryServiceImpl implements UserQueryService {
    private final UserRepository _userRepository;

    public UserQueryServiceImpl(UserRepository userRepository){
        _userRepository = userRepository;
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query){
        return _userRepository.findById(query.userId());
    }

    @Override
    public Optional<User> handle(GetUserByUsernameQuery query){
        return _userRepository.findByUsername(query.username());
    }
}
