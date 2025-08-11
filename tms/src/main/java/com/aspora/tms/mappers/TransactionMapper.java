package com.aspora.tms.mappers;

import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    TransactionResponseDTO toDTO(Transaction transaction);
}
