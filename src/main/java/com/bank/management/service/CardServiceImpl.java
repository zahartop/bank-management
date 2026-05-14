package com.bank.management.service;

import com.bank.management.api.error.ForbiddenException;
import com.bank.management.api.error.NotFoundException;
import com.bank.management.dto.mapper.CardMapper;
import com.bank.management.dto.request.CardCreateRequest;
import com.bank.management.dto.response.CardResponse;
import com.bank.management.entity.Card;
import com.bank.management.entity.CardStatus;
import com.bank.management.entity.Role;
import com.bank.management.entity.User;
import com.bank.management.repository.CardRepository;
import com.bank.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    @Override
    @Transactional
    public CardResponse create(CardCreateRequest request, String actorUsername) {
        User actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

        User owner = resolveOwner(actor, request.ownerUserId());

        Card card = new Card();
        card.setUser(owner);
        card.setPan(request.pan());
        card.setExpiryDate(request.expiryDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(request.initialBalance());

        Card saved = cardRepository.save(card);
        return cardMapper.toResponse(saved);
    }

    private User resolveOwner(User actor, Long ownerUserId) {
        if (actor.getRole() == Role.ADMIN) {
            if (ownerUserId == null) {
                return actor;
            }
            return userRepository.findById(ownerUserId)
                    .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Owner user not found"));
        }
        if (ownerUserId != null) {
            throw new ForbiddenException("OWNER_FORBIDDEN", "Only administrators may set ownerUserId");
        }
        return actor;
    }

    @Override
    @Transactional(readOnly = true)
    public CardResponse getById(Long id, String actorUsername) {
        User actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        Card card = cardRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("CARD_NOT_FOUND", "Card not found"));
        if (actor.getRole() != Role.ADMIN && !card.getUser().getId().equals(actor.getId())) {
            throw new ForbiddenException("CARD_ACCESS_FORBIDDEN", "You cannot access this card");
        }
        return cardMapper.toResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> list(Pageable pageable, String actorUsername) {
        User actor = userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        Page<Card> page = actor.getRole() == Role.ADMIN
                ? cardRepository.findAllWithUser(pageable)
                : cardRepository.findByUser_Id(actor.getId(), pageable);
        return page.map(cardMapper::toResponse);
    }
}
