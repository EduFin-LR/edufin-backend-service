package com.upc.edufinservice.learning.interfaces.rest.transform;

import com.upc.edufinservice.learning.domain.model.aggregates.Question;
import com.upc.edufinservice.learning.domain.model.entities.QuestionOption;
import com.upc.edufinservice.learning.interfaces.rest.resources.QuestionOptionResource;
import com.upc.edufinservice.learning.interfaces.rest.resources.QuestionResource;

import java.util.List;
import java.util.stream.Collectors;

public class QuestionResourceFromAggregateAssembler {
    public static QuestionResource toResourceFromAggregate(Question question, List<QuestionOption> options) {
        var optionResources = options.stream()
                .map(opt -> new QuestionOptionResource(opt.getId(), opt.getOptionText(), opt.getIsCorrect()))
                .collect(Collectors.toList());

        return new QuestionResource(
                question.getId(),
                question.getQuestionText(),
                question.getExplanation(),
                optionResources
        );
    }
}