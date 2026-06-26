package com.upc.edufinservice.iam.interfaces.rest.transform;

import com.upc.edufinservice.iam.domain.model.commands.SignUpCommand;
import com.upc.edufinservice.iam.interfaces.rest.resources.SignUpResource;

public class SignUpCommandFromResourceAssembler {
    public static SignUpCommand toCommandFromResource(SignUpResource resource){
        return new SignUpCommand(resource.username(), resource.email(), resource.password(), resource.gender());
    }
}
