package com.bank.management.dto.mapper;

import com.bank.management.dto.response.CardResponse;
import com.bank.management.entity.Card;
import com.bank.management.util.PanMasking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {PanMasking.class})
public interface CardMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "maskedPan", expression = "java(PanMasking.mask(card.getPan()))")
    CardResponse toResponse(Card card);
}
