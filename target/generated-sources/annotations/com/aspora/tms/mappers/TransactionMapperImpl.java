package com.aspora.tms.mappers;

import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.entity.Transaction;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-11T20:23:01+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.1 (Oracle Corporation)"
)
@Component
public class TransactionMapperImpl implements TransactionMapper {

    @Override
    public TransactionResponseDTO toDTO(Transaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        TransactionResponseDTO transactionResponseDTO = new TransactionResponseDTO();

        if ( transaction.getStatus() != null ) {
            transactionResponseDTO.setStatus( transaction.getStatus().name() );
        }
        transactionResponseDTO.setAmount( transaction.getAmount() );
        transactionResponseDTO.setCreatedAt( transaction.getCreatedAt() );

        return transactionResponseDTO;
    }
}
